// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.verx.cashflow.lancamentos.domain.Lancamento;
import com.verx.cashflow.lancamentos.domain.OutboxEvent;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
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

    /**
     * Reivindica (claim) até {@code batch} eventos {@code PENDING} ordenados por
     * criação, com {@code FOR UPDATE SKIP LOCKED} dentro de uma transação JDBI
     * (US-003) — instâncias concorrentes do relay nunca reivindicam a mesma linha.
     *
     * <p>{@code payload::text} devolve o JSON do contrato CDC já serializado;
     * {@link OutboxEventMapper} mapeia as colunas para {@link OutboxEvent}.
     */
    @SqlQuery("""
            SELECT id, aggregate_id, topic, payload::text AS payload
            FROM outbox_event
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :batch
            FOR UPDATE SKIP LOCKED
            """)
    @RegisterRowMapper(OutboxEventMapper.class)
    @Transaction
    List<OutboxEvent> claimPending(@Bind("batch") int batch);

    /**
     * Marca o evento como {@code PUBLISHED} com {@code published_at} — sucesso da
     * publicação (US-003). Atualização atômica de linha única.
     */
    @SqlUpdate("""
            UPDATE outbox_event
            SET status = 'PUBLISHED', published_at = now()
            WHERE id = :id
            """)
    int markPublished(@Bind("id") UUID id);

    /**
     * Incrementa {@code tentativas} mantendo {@code PENDING} — falha da publicação
     * (US-003), para retry no próximo tick (at-least-once).
     */
    @SqlUpdate("""
            UPDATE outbox_event
            SET tentativas = tentativas + 1
            WHERE id = :id
            """)
    int markFailed(@Bind("id") UUID id);

    /**
     * Mapeia a projeção do claim para {@link OutboxEvent} (SQL explícito, sem ORM).
     */
    class OutboxEventMapper implements RowMapper<OutboxEvent> {

        @Override
        public OutboxEvent map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new OutboxEvent(
                    rs.getObject("id", UUID.class),
                    rs.getObject("aggregate_id", UUID.class),
                    rs.getString("topic"),
                    rs.getString("payload"));
        }
    }
}
