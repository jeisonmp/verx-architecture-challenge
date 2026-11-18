// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Regressão da propagação do {@code correlationId} e da redação LGPD §2.1 no
 * filtro de acesso (ADR-0011): headers/body nunca alcançam a linha de log.
 */
class CorrelationIdFilterTest {

    private static final String UUID_REGEX =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void limpaMdc() {
        MDC.clear();
    }

    @Test
    void preservaCorrelationIdValidoEPropagaAoMdc() throws Exception {
        String correlationId = "abc.DEF_123-xyz";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consolidado");
        request.addHeader("X-Correlation-Id", correlationId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteCadeia = new AtomicReference<>();
        AtomicBoolean cadeiaInvocada = new AtomicBoolean();
        FilterChain chain = (req, res) -> {
            cadeiaInvocada.set(true);
            mdcDuranteCadeia.set(MDC.get(CorrelationIdFilter.CORRELATION_ID));
        };

        filter.doFilter(request, response, chain);

        // AC-1: header válido é preservado e propagado ao MDC.
        assertThat(mdcDuranteCadeia.get()).isEqualTo(correlationId);
        // AC-4: a cadeia é invocada.
        assertThat(cadeiaInvocada.get()).isTrue();
        // AC-3: o response header recebe o valor sanitizado.
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(correlationId);
        // AC-4: o MDC é removido no finally.
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID)).isNull();
    }

    @Test
    void substituiHeaderInvalidoPorUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consolidado");
        request.addHeader("X-Correlation-Id", "valor invalido;DROP TABLE");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteCadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuranteCadeia.set(MDC.get(CorrelationIdFilter.CORRELATION_ID));

        filter.doFilter(request, response, chain);

        // AC-2: header fora do padrão vira UUID usado no MDC.
        assertThat(mdcDuranteCadeia.get()).matches(UUID_REGEX);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(mdcDuranteCadeia.get());
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID)).isNull();
    }

    @Test
    void geraUuidQuandoHeaderAusente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consolidado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteCadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuranteCadeia.set(MDC.get(CorrelationIdFilter.CORRELATION_ID));

        filter.doFilter(request, response, chain);

        // AC-2: header ausente gera UUID usado no MDC.
        assertThat(mdcDuranteCadeia.get()).matches(UUID_REGEX);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(mdcDuranteCadeia.get());
    }

    @Test
    void linhaDeAcessoNaoExpoeCredenciais() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/consolidado");
            request.setQueryString("access_token=super-secret");
            request.addHeader("Authorization", "Bearer super-secret");
            request.addHeader("Cookie", "session=super-secret");
            request.addHeader("x-api-key", "super-secret");
            request.addHeader("X-Correlation-Id", "corr-123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(appender.list).hasSize(1);
            String accessLine = appender.list.get(0).getFormattedMessage();
            assertThat(accessLine)
                    .contains("POST")
                    .contains("/consolidado")
                    .doesNotContain("super-secret")
                    .doesNotContain("Authorization")
                    .doesNotContain("Cookie")
                    .doesNotContain("access_token")
                    .doesNotContain("x-api-key");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void removeMdcNoFinallyMesmoQuandoCadeiaFalha() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consolidado");
        request.addHeader("X-Correlation-Id", "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("falha na cadeia")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class)
                .hasMessage("falha na cadeia");

        // AC-4: mesmo com exceção, o MDC é limpo no finally.
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID)).isNull();
    }
}
