// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.verx.cashflow.lancamentos.domain.Lancamento;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

/**
 * Repositório JDBI (SQL Object) da escrita do microsserviço A.
 *
 * <p>Grava {@code lancamento} e {@code outbox_event} com SQL explícito (ADR-0006/0008 —
 * sem ORM). A atomicidade entre os dois inserts é garantida por {@code @Transaction}
 * no caso de uso (US-002); aqui ficam apenas os comandos de persistência.
 *
 * <p>{@code tipo} e {@code status} são bindados como {@code String} (valores do
 * CHECK constraint); a conversão a partir do enum de domínio ocorre no caso de uso.
 */
public interface LancamentoRepository {

    @SqlUpdate("""
            INSERT INTO lancamento (id, tipo, valor, data, descricao)
            VALUES (:id, :tipo, :valor, :data, :descricao)
            """)
    int insertLancamento(
            @Bind("id") UUID id,
            @Bind("tipo") String tipo,
            @Bind("valor") BigDecimal valor,
            @Bind("data") OffsetDateTime data,
            @Bind("descricao") String descricao);

    @SqlUpdate("""
            INSERT INTO outbox_event (id, aggregate_id, topic, payload, schema_version, status, tentativas)
            VALUES (:id, :aggregateId, :topic, CAST(:payload AS jsonb), :schemaVersion, :status, :tentativas)
            """)
    int insertOutboxEvent(
            @Bind("id") UUID id,
            @Bind("aggregateId") UUID aggregateId,
            @Bind("topic") String topic,
            @Bind("payload") String payload,
            @Bind("schemaVersion") int schemaVersion,
            @Bind("status") String status,
            @Bind("tentativas") int tentativas);

    /**
     * Grava {@code lancamento} e o {@code outbox_event} correspondente na mesma
     * transação JDBI — a atomicidade do Transactional Outbox (US-002).
     *
     * <p>O evento nasce com os invariantes do caminho de escrita: {@code schemaVersion=1}
     * (contrato CDC, ADR-0015), {@code status='PENDING'} e {@code tentativas=0}; a
     * publicação é responsabilidade do relay (US-003). Se o insert do outbox falhar,
     * o {@code @Transaction} desfaz o insert de {@code lancamento}.
     */
    @Transaction
    default void gravarLancamentoEOutbox(
            Lancamento lancamento, UUID eventId, String topic, String payload) {
        insertLancamento(
                lancamento.id(),
                lancamento.tipo().name(),
                lancamento.valor(),
                lancamento.data(),
                lancamento.descricao());
        insertOutboxEvent(eventId, lancamento.id(), topic, payload, 1, "PENDING", 0);
    }
}
