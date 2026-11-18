// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

/**
 * Regressão da conversion word Logback {@code %maskedMsg} (ADR-0011, LGPD §2.1):
 * a mensagem formatada deve passar por {@link SensitiveDataMasker} antes de ser
 * serializada no JSON.
 */
class SensitiveDataMessageConverterTest {

    private final SensitiveDataMessageConverter converter = new SensitiveDataMessageConverter();

    @Test
    void redigeCredencialDaMensagemFormatada() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("Authorization: Bearer abc.def");

        // AC-1: token sensível na mensagem formatada é redigido.
        assertThat(converter.convert(event))
                .isEqualTo("Authorization: [REDACTED]")
                .doesNotContain("abc.def");
    }

    @Test
    void preservaMensagemSemCredenciais() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("HTTP GET /consolidado -> 200 (3 ms)");

        // O converter é aplicado a toda linha de log: mensagens limpas não mudam.
        assertThat(converter.convert(event)).isEqualTo("HTTP GET /consolidado -> 200 (3 ms)");
    }
}
