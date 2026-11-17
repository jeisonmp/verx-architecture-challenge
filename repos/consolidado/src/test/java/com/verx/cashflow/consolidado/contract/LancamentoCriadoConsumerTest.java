// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Prova do contrato {@code lancamento.criado} no lado do consumidor (B) — ADR-0015.
 *
 * <p>Com o Stub Runner de messaging removido no Spring Cloud Contract 4.x, B não sobe
 * mais o stub de A. Este teste unitário desserializa um fixture JSON (espelho do corpo
 * do contrato {@code lancamento-criado.groovy} de A) usando o DTO próprio de B
 * ({@link LancamentoCriadoEvent}) e valida os campos que B consome. O DTO é um
 * <em>tolerant reader</em> — {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * descarta os campos que B não consome ({@code eventId}, {@code occurredAt},
 * {@code data}, {@code descricao}).
 */
class LancamentoCriadoConsumerTest {

    /** Fixture no classpath: espelho do corpo do contrato lancamento.criado de A. */
    private static final String FIXTURE_LANCAMENTO_CRIADO = "contracts/lancamento-criado.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveDesserializarLancamentoCriadoComoTolerantReader() throws IOException {
        // when: B lê o corpo do contrato publicado por A (fixture equivalente ao stub)
        LancamentoCriadoEvent event = carregarFixture();

        // then: schemaVersion é obrigatório e fixado em 1 (ADR-0015) — ausência ou
        // versão desconhecida faz B rejeitar a mensagem via DLQ.
        assertThat(event.schemaVersion()).isEqualTo(1);
        assertThat(event.eventType()).isEqualTo("lancamento.criado");

        // B consome apenas id/tipo/valor do lancamento — o restante é ignorado.
        assertThat(event.lancamento()).isNotNull();
        assertThat(event.lancamento().id()).isNotBlank();
        assertThat(event.lancamento().tipo()).isIn("ENTRADA", "SAIDA");
        assertThat(event.lancamento().valor()).isNotNull();
    }

    private LancamentoCriadoEvent carregarFixture() throws IOException {
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream(FIXTURE_LANCAMENTO_CRIADO)) {
            assertThat(in)
                    .as("fixture %s deve existir no classpath", FIXTURE_LANCAMENTO_CRIADO)
                    .isNotNull();
            return objectMapper.readValue(in, LancamentoCriadoEvent.class);
        }
    }
}
