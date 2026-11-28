// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verx.cashflow.lancamentos.infra.persistence.LancamentoRepository;

/**
 * Integração do relay contra PostgreSQL real (Flyway) e um producer Kafka de
 * teste em memória ({@link MockProducer}, do {@code kafka-clients} transitivo) —
 * prova o evento publicado no tópico {@code lancamento.criado} e a transição de
 * status do outbox (US-003).
 *
 * <p>{@code @Testcontainers(disabledWithoutDocker = true)} pula a classe quando o
 * daemon Docker não está disponível (ex.: runner BYOI sem socket) — os testes
 * unitários continuam verdes e a integração roda no host/CI com Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
class OutboxRelayIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private Jdbi jdbi;
    private LancamentoRepository repository;
    private MockProducer<String, String> mockProducer;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();

        jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
        repository = jdbi.onDemand(LancamentoRepository.class);

        mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory(mockProducer));
        relay = new OutboxRelay(repository, kafkaTemplate, 100);
    }

    @Test
    void publicaEventoPendenteNoTopicoLancamentoCriado() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = "{\"schemaVersion\":1,\"eventType\":\"lancamento.criado\",\"eventId\":\""
                + eventId + "\",\"lancamento\":{\"id\":\"" + aggregateId + "\",\"tipo\":\"ENTRADA\","
                + "\"valor\":150.00,\"data\":\"2026-11-27T10:00:00Z\",\"descricao\":\"Venda\"}}";
        repository.insertOutboxEvent(eventId, aggregateId, "lancamento.criado", payload, 1, "PENDING", 0);

        relay.relayPendingEvents();

        List<ProducerRecord<String, String>> history = mockProducer.history();
        assertThat(history).as("AC-2: exatamente um evento publicado").hasSize(1);
        ProducerRecord<String, String> record = history.get(0);
        assertThat(record.topic()).as("AC-2: tópico lancamento.criado").isEqualTo("lancamento.criado");
        assertThat(record.key()).as("AC-2: key = aggregate_id").isEqualTo(aggregateId.toString());

        JsonNode published = new ObjectMapper().readTree(record.value());
        assertThat(published.get("schemaVersion").asInt())
                .as("AC-5: schemaVersion publicado é sempre 1").isEqualTo(1);
        assertThat(published.get("eventType").asText()).isEqualTo("lancamento.criado");
        assertThat(published.get("lancamento").get("id").asText()).isEqualTo(aggregateId.toString());

        Map<String, Object> row = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT status, tentativas, published_at
                FROM outbox_event
                WHERE id = :id
                """).bind("id", eventId).mapToMap().one());
        assertThat(row.get("status")).as("AC-3: sucesso → status=PUBLISHED").isEqualTo("PUBLISHED");
        assertThat(row.get("tentativas")).isEqualTo(0);
        assertThat(row.get("published_at")).as("AC-3: published_at preenchido").isNotNull();
    }

    @Test
    void falhaDePublicacaoMantemPendenteEIncrementaTentativas() {
        UUID aggregateId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        repository.insertOutboxEvent(eventId, aggregateId, "lancamento.criado",
                "{\"schemaVersion\":1,\"eventType\":\"lancamento.criado\"}", 1, "PENDING", 0);

        mockProducer.errorNext(new RuntimeException("broker down"));

        relay.relayPendingEvents();

        Map<String, Object> row = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT status, tentativas, published_at
                FROM outbox_event
                WHERE id = :id
                """).bind("id", eventId).mapToMap().one());
        assertThat(row.get("status")).as("AC-4: falha → mantém PENDING").isEqualTo("PENDING");
        assertThat(row.get("tentativas")).as("AC-4: falha → incrementa tentativas").isEqualTo(1);
        assertThat(row.get("published_at")).as("AC-4: published_at permanece nulo").isNull();
    }

    private static ProducerFactory<String, String> producerFactory(MockProducer<String, String> producer) {
        return new ProducerFactory<>() {
            @Override
            public Producer<String, String> createProducer() {
                return producer;
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return Map.of();
            }

            @Override
            public boolean transactionCapable() {
                return false;
            }
        };
    }
}
