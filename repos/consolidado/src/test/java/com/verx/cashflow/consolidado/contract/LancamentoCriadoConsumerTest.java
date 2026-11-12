// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verx.cashflow.consolidado.ConsolidadoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.messaging.Message;

/**
 * Teste de integração do consumidor (B) contra o stub gerado pelo produtor (A).
 *
 * <p>ADR-0015 (CDC): B consome o stub do evento {@code lancamento.criado} sem depender
 * de A em execução. O stub é carregado do repositório local pelo Spring Cloud Contract
 * Stub Runner; B o desserializa como <em>tolerant reader</em> (cópia própria do DTO,
 * ignorando campos desconhecidos) e valida o {@code schemaVersion} obrigatório.
 *
 * <p>Pré-requisito no host: {@code ./mvnw -f repos/lancamentos install} (gera e instala
 * o stub) antes de {@code ./mvnw -f repos/consolidado test}.
 */
@SpringBootTest(
        classes = ConsolidadoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@AutoConfigureMessageVerifier
@AutoConfigureStubRunner(
        ids = {"com.verx.cashflow:lancamentos:+:stubs:8080"},
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class LancamentoCriadoConsumerTest {

    private static final String TOPIC_LANCAMENTO_CRIADO = "lancamento.criado";

    @Autowired
    private StubTrigger stubTrigger;

    @Autowired
    private MessageVerifier<Message<?>> messageVerifier;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveConsumirStubDoLancamentoCriadoComSchemaVersionObrigatorio() throws Exception {
        // when: o stub do produtor (A) publica o evento no tópico
        stubTrigger.trigger("lancamento.criado");

        // then: B recebe a mensagem do stub (sem A em execução)
        Message<?> received =
                messageVerifier.receive(TOPIC_LANCAMENTO_CRIADO, 5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        // Tolerant reader (ADR-0015): B desserializa com a PRÓPRIA cópia do DTO,
        // ignorando campos desconhecidos (eventId, occurredAt, data, descricao).
        LancamentoCriadoEvent event =
                objectMapper.readValue(payloadAsString(received.getPayload()), LancamentoCriadoEvent.class);

        // ADR-0015: schemaVersion é obrigatório desde a v1 — a ausência ou versão
        // desconhecida faz B rejeitar a mensagem (DLQ).
        assertThat(event.schemaVersion()).isEqualTo(1);
        assertThat(event.eventType()).isEqualTo("lancamento.criado");
        assertThat(event.lancamento()).isNotNull();
        assertThat(event.lancamento().id()).isNotBlank();
        assertThat(event.lancamento().tipo()).isIn("ENTRADA", "SAIDA");
        assertThat(event.lancamento().valor()).isNotNull();
    }

    private static String payloadAsString(Object payload) {
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(payload);
    }
}
