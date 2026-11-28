// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Cobertura do modelo de domínio de Lançamento (US-001).
 */
class LancamentoTest {

    @Test
    void tipoLancamentoExpoeDoisValores() {
        assertThat(TipoLancamento.values())
                .containsExactly(TipoLancamento.ENTRADA, TipoLancamento.SAIDA);
    }

    @Test
    void recordLancamentoExpoeCamposPersistidos() {
        UUID id = UUID.randomUUID();
        OffsetDateTime data = OffsetDateTime.parse("2026-11-21T10:00:00Z");
        OffsetDateTime criadoEm = OffsetDateTime.parse("2026-11-21T10:05:00Z");
        BigDecimal valor = new BigDecimal("150.00");

        Lancamento lancamento = new Lancamento(
                id, TipoLancamento.ENTRADA, valor, data, "Venda — pagamento recebido", criadoEm);

        assertThat(lancamento.id()).isEqualTo(id);
        assertThat(lancamento.tipo()).isEqualTo(TipoLancamento.ENTRADA);
        assertThat(lancamento.valor()).isEqualByComparingTo(valor);
        assertThat(lancamento.data()).isEqualTo(data);
        assertThat(lancamento.descricao()).isEqualTo("Venda — pagamento recebido");
        assertThat(lancamento.criadoEm()).isEqualTo(criadoEm);
    }
}
