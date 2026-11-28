// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.relay;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.verx.cashflow.lancamentos.domain.OutboxEvent;
import com.verx.cashflow.lancamentos.infra.persistence.LancamentoRepository;

/**
 * Outbox worker (relay) — publica os eventos {@code PENDING} do outbox no tópico
 * {@code lancamento.criado} (US-003).
 *
 * <p>Poll in-process via {@code @Scheduled}. O claim
 * ({@link LancamentoRepository#claimPending}) usa {@code FOR UPDATE SKIP LOCKED}
 * para que instâncias concorrentes nunca reivindiquem a mesma linha; a publicação
 * é at-least-once — sucesso marca {@code PUBLISHED}, falha mantém {@code PENDING}
 * e incrementa {@code tentativas} para retry no próximo tick.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long KAFKA_SEND_TIMEOUT_SECONDS = 5L;

    private final LancamentoRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxRelay(LancamentoRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                       @Value("${app.outbox.relay.batch-size:100}") int batchSize) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:5000}",
               initialDelayString = "${app.outbox.relay.initial-delay-ms:5000}")
    public void relayPendingEvents() {
        List<OutboxEvent> pendentes = repository.claimPending(batchSize);
        for (OutboxEvent evento : pendentes) {
            try {
                kafkaTemplate.send(evento.topic(), evento.aggregateId().toString(), evento.payload())
                        .get(KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                repository.markPublished(evento.id());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                repository.markFailed(evento.id());
            } catch (Exception e) {
                log.warn("Falha ao publicar o evento {} no tópico {} — mantendo PENDING e incrementando tentativas",
                        evento.id(), evento.topic(), e);
                repository.markFailed(evento.id());
            }
        }
    }
}
