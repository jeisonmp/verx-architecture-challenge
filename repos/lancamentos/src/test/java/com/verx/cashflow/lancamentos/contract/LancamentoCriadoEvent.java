// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cópia (test-scoped) do DTO do evento {@code lancamento.criado} no lado do
 * produtor (A), usada para materializar o payload do contrato CDC.
 *
 * <p>ADR-0015: cada serviço mantém a própria cópia do DTO — não há jar compartilhado.
 * No Sprint 2 este registro é substituído pelo caminho real do Transactional Outbox;
 * o formato do payload (e o contrato) permanecem os mesmos.
 */
public record LancamentoCriadoEvent(
        int schemaVersion,
        String eventId,
        String eventType,
        OffsetDateTime occurredAt,
        Lancamento lancamento) {

    public record Lancamento(
            String id,
            String tipo,
            BigDecimal valor,
            OffsetDateTime data,
            String descricao) {
    }

    public static LancamentoCriadoEvent exemploValido() {
        return new LancamentoCriadoEvent(
                1,
                UUID.randomUUID().toString(),
                "lancamento.criado",
                OffsetDateTime.now(),
                new Lancamento(
                        UUID.randomUUID().toString(),
                        "ENTRADA",
                        new BigDecimal("150.00"),
                        OffsetDateTime.now(),
                        "Venda — pagamento recebido"));
    }
}
