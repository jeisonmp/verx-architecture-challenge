// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verx.cashflow.lancamentos.LancamentosApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Classe-base do teste CDC do produtor (A).
 *
 * <p>O plugin Spring Cloud Contract gera {@code LancamentoCriadoTest} a partir de
 * {@code lancamento-criado.groovy}; o método {@code criarLancamento()} dispara o
 * evento e o {@link MessageVerifierSender} captura a mensagem para compará-la ao contrato.
 *
 * <p>Sprint 1: não há produtor real (Transactional Outbox entra no Sprint 2), então
 * o evento é construído aqui de forma inline, mantendo o contrato já executável.
 * Quando o outbox existir, este método passa a invocar o caminho de produção real —
 * o contrato permanece o mesmo.
 */
@SpringBootTest(
        classes = {LancamentosApplication.class, LancamentoCriadoTestChannelConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@AutoConfigureMessageVerifier
public abstract class LancamentoCriadoBase {

    @Autowired
    private MessageVerifierSender<Message<?>> messagingMessageVerifier;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Gatilho referenciado pelo contrato ({@code triggeredBy "criarLancamento()"}).
     */
    public void criarLancamento() {
        LancamentoCriadoEvent event = LancamentoCriadoEvent.exemploValido();
        try {
            String json = objectMapper.writeValueAsString(event);
            Message<String> message = MessageBuilder
                    .withPayload(json)
                    .setHeader("contentType", "application/json")
                    .build();
            messagingMessageVerifier.send(message, "lancamento.criado");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Falha ao serializar o evento lancamento.criado para o contrato CDC", e);
        }
    }
}
