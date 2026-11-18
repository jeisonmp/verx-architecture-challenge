// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;

import org.junit.jupiter.api.Test;

/**
 * Regressão do conversion word {@code %maskedMsg} (ADR-0011, LGPD §2.1).
 *
 * <p>Garante que {@link SensitiveDataMessageConverter#convert(ILoggingEvent)}
 * aplica {@link SensitiveDataMasker} à mensagem formatada antes da serialização
 * JSON — credenciais nunca alcançam o log.
 */
class SensitiveDataMessageConverterTest {

    private final SensitiveDataMessageConverter converter = new SensitiveDataMessageConverter();

    @Test
    void redigeTokenSensivelNaMensagemFormatada() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("Authorization: Bearer abc.def.ghi");

        String converted = converter.convert(event);

        assertThat(converted)
                .as("AC-1: mensagem formatada com token sensivel e redigida")
                .doesNotContain("abc.def.ghi")
                .contains("[REDACTED]");
    }

    @Test
    void preservaMensagemFormatadaSemDadosSensiveis() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("HTTP GET /actuator/health -> 200 (3 ms)");

        assertThat(converter.convert(event))
                .as("AC-2: convert preserva mensagem formatada sem credenciais")
                .isEqualTo("HTTP GET /actuator/health -> 200 (3 ms)");
    }
}
