// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verx.cashflow.lancamentos.domain.Lancamento;
import com.verx.cashflow.lancamentos.domain.LancamentoCriadoPayload;
import com.verx.cashflow.lancamentos.domain.TipoLancamento;
import com.verx.cashflow.lancamentos.infra.persistence.LancamentoRepository;

/**
 * Caso de uso de criação de lançamento (Transactional Outbox).
 *
 * <p>Grava {@code lancamento} e o {@code outbox_event} na mesma transação JDBI
 * ({@link LancamentoRepository#gravarLancamentoEOutbox}) sem enviar ao Kafka — a
 * publicação fica a cargo do relay (US-003). Se a persistência falhar, a exceção
 * propaga (HTTP 500) e nada é publicado, pois o insert do outbox também é desfeito.
 */
@Service
public class CriarLancamentoService {

    private static final String TOPICO_LANCAMENTO_CRIADO = "lancamento.criado";

    private final LancamentoRepository repository;
    private final ObjectMapper objectMapper;

    public CriarLancamentoService(LancamentoRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Lancamento criar(TipoLancamento tipo, BigDecimal valor, OffsetDateTime data, String descricao) {
        UUID id = UUID.randomUUID();
        Lancamento lancamento = new Lancamento(id, tipo, valor, data, descricao, OffsetDateTime.now());

        UUID eventId = UUID.randomUUID();
        String payload = serializarPayload(lancamento, eventId);
        repository.gravarLancamentoEOutbox(lancamento, eventId, TOPICO_LANCAMENTO_CRIADO, payload);
        return lancamento;
    }

    private String serializarPayload(Lancamento lancamento, UUID eventId) {
        try {
            return objectMapper.writeValueAsString(LancamentoCriadoPayload.from(lancamento, eventId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o payload do evento lancamento.criado", e);
        }
    }
}
