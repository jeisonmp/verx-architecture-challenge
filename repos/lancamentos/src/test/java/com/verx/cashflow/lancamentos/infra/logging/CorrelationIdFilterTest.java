// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Regressão do {@link CorrelationIdFilter} (ADR-0011, LGPD §2.1).
 *
 * <p>Garante que o {@code correlationId} é sanitizado, propagado ao MDC e
 * devolvido no response header, que o MDC é sempre limpo no {@code finally} e
 * que credenciais não alcançam a linha de acesso do log.
 */
class CorrelationIdFilterTest {

    private static final String HEADER = "X-Correlation-Id";

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservaCorrelationIdValidoNoMdcEnoResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        request.addHeader(HEADER, "abc-123_OK.v1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenInChain = new String[1];

        filter.doFilter(request, response, (req, res) -> seenInChain[0] = MDC.get(CorrelationIdFilter.CORRELATION_ID));

        assertThat(seenInChain[0])
                .as("AC-1: header valido propagado ao MDC")
                .isEqualTo("abc-123_OK.v1");
        assertThat(response.getHeader(HEADER))
                .as("AC-3: response header recebe o correlationId sanitizado")
                .isEqualTo("abc-123_OK.v1");
    }

    @Test
    void geraUuidQuandoHeaderAusente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenInChain = new String[1];

        filter.doFilter(request, response, (req, res) -> seenInChain[0] = MDC.get(CorrelationIdFilter.CORRELATION_ID));

        assertThatCode(() -> UUID.fromString(seenInChain[0]))
                .as("AC-2: header ausente gera UUID no MDC")
                .doesNotThrowAnyException();
        assertThat(response.getHeader(HEADER))
                .as("AC-3: response header recebe o UUID gerado")
                .isEqualTo(seenInChain[0]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc 123", "abc$123", "token;drop", ""})
    void geraUuidQuandoHeaderInvalido(String invalid) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        request.addHeader(HEADER, invalid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenInChain = new String[1];

        filter.doFilter(request, response, (req, res) -> seenInChain[0] = MDC.get(CorrelationIdFilter.CORRELATION_ID));

        assertThatCode(() -> UUID.fromString(seenInChain[0]))
                .as("AC-2: header invalido '%s' gera UUID no MDC", invalid)
                .doesNotThrowAnyException();
        assertThat(seenInChain[0]).isNotEqualTo(invalid);
        assertThat(response.getHeader(HEADER)).isEqualTo(seenInChain[0]);
    }

    @Test
    void geraUuidQuandoHeaderExcede128Caracteres() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        request.addHeader(HEADER, "a".repeat(129));
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenInChain = new String[1];

        filter.doFilter(request, response, (req, res) -> seenInChain[0] = MDC.get(CorrelationIdFilter.CORRELATION_ID));

        assertThatCode(() -> UUID.fromString(seenInChain[0]))
                .as("AC-5: header acima de 128 chars exercita o ramo invalido de sanitize")
                .doesNotThrowAnyException();
    }

    @Test
    void invocaFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = new boolean[1];

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).as("AC-4: FilterChain.doFilter invocado").isTrue();
    }

    @Test
    void removeCorrelationIdDoMdcNoFinally() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        request.addHeader(HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID)).isEqualTo("abc-123"));

        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID))
                .as("AC-4: MDC removido no finally")
                .isNull();
    }

    @Test
    void removeCorrelationIdDoMdcMesmoQuandoCadeiaFalha() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
        request.addHeader(HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID))
                .as("AC-4: MDC removido no finally mesmo com excecao")
                .isNull();
    }

    @Test
    void linhaDeAcessoNaoExpoeCredenciaisNemQueryString() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/lancamentos");
            request.setQueryString("token=SEGREDO_QUERY");
            request.addHeader("Authorization", "Bearer SEGREDO_HEADER");
            request.addHeader("Cookie", "session=SEGREDO_COOKIE");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> {
            });
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
                .as("AC-5: o filtro emite exatamente uma linha de acesso")
                .hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
                .as("LGPD §2.1: credenciais nunca alcancam a linha de acesso")
                .contains("GET /lancamentos")
                .doesNotContain("SEGREDO_QUERY")
                .doesNotContain("SEGREDO_HEADER")
                .doesNotContain("SEGREDO_COOKIE")
                .doesNotContain("Bearer");
    }
}
