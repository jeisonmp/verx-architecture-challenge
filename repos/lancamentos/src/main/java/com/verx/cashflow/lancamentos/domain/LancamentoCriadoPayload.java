// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload (jsonb) do evento {@code lancamento.criado} gravado em {@code outbox_event}.
 *
 * <p>Espelha exatamente o contrato CDC {@code lancamento-criado.groovy} (ADR-0015):
 * {@code schemaVersion} (sempre 1), {@code eventId}, {@code eventType},
 * {@code occurredAt} (ISO-8601 com offset) e o snapshot {@code lancamento}
 * ({@code id}, {@code tipo}, {@code valor}, {@code data}, {@code descricao}).
 * {@code criadoEm} não é exposto: não faz parte do contrato e o consumidor B
 * rejeitaria campos desconhecidos.
 */
public record LancamentoCriadoPayload(
        int schemaVersion,
        String eventId,
        String eventType,
        OffsetDateTime occurredAt,
        LancamentoPayload lancamento) {

    public record LancamentoPayload(
            String id,
            String tipo,
            BigDecimal valor,
            OffsetDateTime data,
            String descricao) {
    }

    public static LancamentoCriadoPayload from(Lancamento lancamento, UUID eventId) {
        return new LancamentoCriadoPayload(
                1,
                eventId.toString(),
                "lancamento.criado",
                lancamento.criadoEm(),
                new LancamentoPayload(
                        lancamento.id().toString(),
                        lancamento.tipo().name(),
                        lancamento.valor(),
                        lancamento.data(),
                        lancamento.descricao()));
    }
}
