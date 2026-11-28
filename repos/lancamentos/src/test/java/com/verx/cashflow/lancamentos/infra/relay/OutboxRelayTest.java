// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.verx.cashflow.lancamentos.domain.OutboxEvent;
import com.verx.cashflow.lancamentos.infra.persistence.LancamentoRepository;

/**
 * Teste unitário do relay (mock do repositório e do KafkaTemplate) — US-003.
 *
 * <p>Cobre sucesso (marca {@code PUBLISHED}), falha (mantém {@code PENDING} e
 * incrementa {@code tentativas}) e a asserção do contrato CDC
 * ({@code schemaVersion=1}).
 */
class OutboxRelayTest {

    private LancamentoRepository repository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        repository = mock(LancamentoRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        relay = new OutboxRelay(repository, kafkaTemplate, 100);
    }

    @Test
    void publicaEventoPendenteNoTopicoEMarcaComoPublished() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String payload = "{\"schemaVersion\":1,\"eventType\":\"lancamento.criado\",\"eventId\":\""
                + eventId + "\"}";
        OutboxEvent evento = new OutboxEvent(eventId, aggregateId, "lancamento.criado", payload);

        when(repository.claimPending(100)).thenReturn(List.of(evento));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        relay.relayPendingEvents();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1).description("AC-2: publica cada evento pendente no tópico com key/value"))
                .send(eq("lancamento.criado"), keyCaptor.capture(), valueCaptor.capture());
        assertThat(keyCaptor.getValue())
                .as("AC-2: key = aggregate_id")
                .isEqualTo(aggregateId.toString());
        assertThat(valueCaptor.getValue())
                .as("AC-2: value = payload do contrato CDC")
                .isEqualTo(payload);
        assertThat(valueCaptor.getValue())
                .as("AC-5: schemaVersion publicado é sempre 1")
                .contains("\"schemaVersion\":1");
        verify(repository, times(1).description("AC-3: sucesso → status=PUBLISHED")).markPublished(eventId);
        verify(repository, never().description("AC-4: sem falha, tentativas não incrementa")).markFailed(any());
    }

    @Test
    void falhaDePublicacaoMantemPendenteEIncrementaTentativas() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent evento = new OutboxEvent(eventId, aggregateId, "lancamento.criado",
                "{\"schemaVersion\":1,\"eventType\":\"lancamento.criado\"}");

        when(repository.claimPending(100)).thenReturn(List.of(evento));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay.relayPendingEvents();

        verify(repository, times(1).description("AC-4: falha → incrementa tentativas (mantém PENDING)"))
                .markFailed(eventId);
        verify(repository, never().description("AC-3: falha → NÃO marca PUBLISHED")).markPublished(any());
    }
}
