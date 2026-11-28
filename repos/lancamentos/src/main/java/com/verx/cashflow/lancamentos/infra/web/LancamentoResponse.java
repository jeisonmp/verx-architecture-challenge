// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.verx.cashflow.lancamentos.domain.Lancamento;
import com.verx.cashflow.lancamentos.domain.TipoLancamento;

/**
 * Corpo de resposta do {@code POST /lancamentos} (201 Created): o lançamento criado.
 */
public record LancamentoResponse(
        UUID id,
        TipoLancamento tipo,
        BigDecimal valor,
        OffsetDateTime data,
        String descricao,
        OffsetDateTime criadoEm) {

    public static LancamentoResponse from(Lancamento lancamento) {
        return new LancamentoResponse(
                lancamento.id(),
                lancamento.tipo(),
                lancamento.valor(),
                lancamento.data(),
                lancamento.descricao(),
                lancamento.criadoEm());
    }
}
