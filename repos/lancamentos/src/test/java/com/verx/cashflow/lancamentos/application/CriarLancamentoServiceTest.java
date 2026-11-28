// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.verx.cashflow.lancamentos.domain.Lancamento;
import com.verx.cashflow.lancamentos.domain.TipoLancamento;
import com.verx.cashflow.lancamentos.infra.persistence.LancamentoRepository;

/**
 * Teste unitário do caso de uso (mock do repositório) — US-002.
 */
class CriarLancamentoServiceTest {

    private LancamentoRepository repository;
    private ObjectMapper objectMapper;
    private CriarLancamentoService service;

    @BeforeEach
    void setUp() {
        repository = mock(LancamentoRepository.class);
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new CriarLancamentoService(repository, objectMapper);
    }

    @Test
    void criarGravaLancamentoEOutboxComPayloadDoContrato() throws Exception {
        TipoLancamento tipo = TipoLancamento.ENTRADA;
        BigDecimal valor = new BigDecimal("150.00");
        OffsetDateTime data = OffsetDateTime.parse("2026-11-21T10:00:00Z");
        String descricao = "Venda — pagamento recebido";

        Lancamento resultado = service.criar(tipo, valor, data, descricao);

        ArgumentCaptor<Lancamento> lancamentoCaptor = ArgumentCaptor.forClass(Lancamento.class);
        ArgumentCaptor<UUID> eventIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(repository).gravarLancamentoEOutbox(
                lancamentoCaptor.capture(), eventIdCaptor.capture(), topicCaptor.capture(), payloadCaptor.capture());

        Lancamento gravado = lancamentoCaptor.getValue();
        assertThat(gravado.id()).isEqualTo(resultado.id());
        assertThat(gravado.tipo()).isEqualTo(tipo);
        assertThat(gravado.valor()).isEqualByComparingTo(valor);
        assertThat(gravado.data()).isEqualTo(data);
        assertThat(gravado.descricao()).isEqualTo(descricao);
        assertThat(gravado.criadoEm()).isNotNull();

        assertThat(resultado.id()).isEqualTo(gravado.id());
        assertThat(topicCaptor.getValue()).isEqualTo("lancamento.criado");
        assertThat(eventIdCaptor.getValue()).isNotNull();

        // AC-3: payload do contrato CDC — schemaVersion=1, eventType, eventId, snapshot sem criadoEm.
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(payload.get("eventType").asText()).isEqualTo("lancamento.criado");
        assertThat(payload.get("eventId").asText()).isEqualTo(eventIdCaptor.getValue().toString());
        assertThat(payload.get("occurredAt").asText()).isNotBlank();

        JsonNode lancamentoJson = payload.get("lancamento");
        assertThat(lancamentoJson.get("id").asText()).isEqualTo(gravado.id().toString());
        assertThat(lancamentoJson.get("tipo").asText()).isEqualTo("ENTRADA");
        assertThat(lancamentoJson.get("descricao").asText()).isEqualTo(descricao);
        assertThat(lancamentoJson.get("valor").decimalValue()).isEqualByComparingTo(valor);
        assertThat(lancamentoJson.has("criadoEm")).isFalse();
    }

    @Test
    void falhaDePersistenciaPropagaSemPublicar() {
        doThrow(new RuntimeException("erro de persistência")).when(repository)
                .gravarLancamentoEOutbox(any(), any(), any(), any());

        assertThatThrownBy(() -> service.criar(
                TipoLancamento.SAIDA,
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2026-11-21T10:00:00Z"),
                "pagamento de fornecedor"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("erro de persistência");

        verify(repository).gravarLancamentoEOutbox(any(), any(), any(), any());
        // AC-5: a escrita nunca envia ao Kafka diretamente — a publicação é do relay (US-003).
    }
}
