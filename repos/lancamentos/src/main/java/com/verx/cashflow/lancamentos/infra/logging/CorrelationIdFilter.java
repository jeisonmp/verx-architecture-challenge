// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propaga o {@code correlationId} no MDC de cada requisição e emite uma linha de
 * acesso estruturada (ADR-0011).
 *
 * <p>A linha contém apenas método, path (sem query string), status e duração.
 * Headers e body não são logados — {@code Authorization}, {@code Cookie},
 * {@code x-api-key} e tokens nunca alcançam o log (LGPD §2.1).
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Chave do MDC consumida pelo encoder JSON ({@code mdc} provider). */
    public static final String CORRELATION_ID = "correlationId";

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = sanitize(request.getHeader(CORRELATION_ID_HEADER));
        MDC.put(CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("HTTP {} {} -> {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMillis);
            MDC.remove(CORRELATION_ID);
        }
    }

    private static String sanitize(String candidate) {
        if (candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
