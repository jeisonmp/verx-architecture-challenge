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
 * Agregado persistido de um lançamento (entrada ou saída) do fluxo de caixa.
 *
 * <p>Espelha a tabela {@code lancamento} (V1__create_lancamento_outbox.sql):
 * {@code id}, {@code tipo}, {@code valor}, {@code data}, {@code descricao} e
 * {@code criado_em}.
 */
public record Lancamento(
        UUID id,
        TipoLancamento tipo,
        BigDecimal valor,
        OffsetDateTime data,
        String descricao,
        OffsetDateTime criadoEm) {
}
