// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integração do schema (Flyway) e do repositório JDBI contra PostgreSQL real.
 *
 * <p>{@code @Testcontainers(disabledWithoutDocker = true)} pula a classe quando o
 * daemon Docker não está disponível (ex.: runner BYOI sem socket) — os testes
 * unitários continuam verdes e a integração roda no host/CI com Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
class LancamentoRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private Jdbi jdbi;
    private LancamentoRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        Flyway.configure().dataSource(dataSource).load().migrate();

        jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
        repository = jdbi.onDemand(LancamentoRepository.class);
    }

    @Test
    void migrationSobeLimpaEmBancoVazio() {
        List<String> tables = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('lancamento', 'outbox_event')
                ORDER BY table_name
                """).mapTo(String.class).list());

        assertThat(tables)
                .as("AC-2/AC-3: migration V1 cria lancamento e outbox_event")
                .containsExactly("lancamento", "outbox_event");
    }

    @Test
    void insertLancamentoPersisteRegistro() {
        UUID id = UUID.randomUUID();

        int inserted = repository.insertLancamento(
                id,
                "ENTRADA",
                new BigDecimal("150.00"),
                OffsetDateTime.parse("2026-11-21T10:00:00Z"),
                "Venda — pagamento recebido");

        assertThat(inserted).as("AC-5: INSERT em lancamento afeta 1 linha").isEqualTo(1);

        Map<String, Object> row = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT tipo, valor, data, descricao, criado_em
                FROM lancamento
                WHERE id = :id
                """).bind("id", id).mapToMap().one());

        assertThat(row.get("tipo")).isEqualTo("ENTRADA");
        assertThat(row.get("valor")).isEqualTo(new BigDecimal("150.00"));
        assertThat(row.get("descricao")).isEqualTo("Venda — pagamento recebido");
        assertThat(row.get("criado_em")).isNotNull();
    }

    @Test
    void insertOutboxEventPersisteEventoPendente() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        int inserted = repository.insertOutboxEvent(
                eventId,
                aggregateId,
                "lancamento.criado",
                "{\"schemaVersion\":1}",
                1,
                "PENDING",
                0);

        assertThat(inserted).as("AC-5: INSERT em outbox_event afeta 1 linha").isEqualTo(1);

        Map<String, Object> row = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT aggregate_id, topic, payload, schema_version, status, tentativas, created_at, published_at
                FROM outbox_event
                WHERE id = :id
                """).bind("id", eventId).mapToMap().one());

        assertThat(row.get("aggregate_id")).isEqualTo(aggregateId);
        assertThat(row.get("topic")).isEqualTo("lancamento.criado");
        assertThat(row.get("schema_version")).isEqualTo(1);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("tentativas")).isEqualTo(0);
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("published_at")).isNull();
    }
}
