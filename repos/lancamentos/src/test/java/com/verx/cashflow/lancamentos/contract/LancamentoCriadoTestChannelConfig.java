// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.PollableChannel;

/**
 * Configuração de teste do produtor (A) que registra o canal em memória do
 * contrato CDC {@code lancamento.criado}.
 *
 * <p>O bean é nomeado exatamente com o destino do contrato
 * ({@code sentTo("lancamento.criado")} em {@code lancamento-criado.groovy}),
 * pois o {@code SpringIntegrationStubMessages} do Spring Cloud Contract 4.x
 * resolve o canal por nome no contexto da aplicação.
 */
@TestConfiguration
public class LancamentoCriadoTestChannelConfig {

    @Bean(name = "lancamento.criado")
    PollableChannel lancamentoCriadoChannel() {
        return new LancamentoCriadoChannel();
    }
}
