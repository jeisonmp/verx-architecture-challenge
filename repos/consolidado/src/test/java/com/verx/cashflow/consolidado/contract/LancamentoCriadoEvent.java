// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Cópia própria (test-scoped) do DTO do evento {@code lancamento.criado} no lado do
 * consumidor (B).
 *
 * <p>ADR-0015: A e B mantêm cópias independentes (sem lib compartilhada). B é um
 * <em>tolerant reader</em> — ignora campos desconhecidos e mantém apenas os campos
 * que realmente consome (contract minimalism), para que a evolução aditiva do
 * contrato no produtor não quebre o consumidor silenciosamente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LancamentoCriadoEvent(
        int schemaVersion,
        String eventType,
        Lancamento lancamento) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Lancamento(
            String id,
            String tipo,
            BigDecimal valor) {
    }
}
