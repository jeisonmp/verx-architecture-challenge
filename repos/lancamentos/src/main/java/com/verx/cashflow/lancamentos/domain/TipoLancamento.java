// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.domain;

/**
 * Natureza do lançamento no fluxo de caixa.
 *
 * <p>Reflete o domínio {@code CHECK (tipo IN ('ENTRADA','SAIDA'))} da coluna
 * {@code lancamento.tipo} (V1__create_lancamento_outbox.sql).
 */
public enum TipoLancamento {
    ENTRADA,
    SAIDA
}
