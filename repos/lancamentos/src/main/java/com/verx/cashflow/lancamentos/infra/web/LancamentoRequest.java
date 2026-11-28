// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.verx.cashflow.lancamentos.domain.TipoLancamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo de entrada do {@code POST /lancamentos}.
 *
 * <p>Validação (jakarta.validation): {@code tipo} obrigatório (enum restringe a
 * {@code ENTRADA}/{@code SAIDA} — valor desconhecido falha na desserialização),
 * {@code valor} positivo, {@code descricao} não-vazia e {@code data} ISO-8601
 * (OffsetDateTime). Violação ⇒ HTTP 400.
 */
public record LancamentoRequest(
        @NotNull(message = "tipo é obrigatório") TipoLancamento tipo,
        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "data é obrigatória") OffsetDateTime data,
        @NotBlank(message = "descricao é obrigatória") String descricao) {
}
