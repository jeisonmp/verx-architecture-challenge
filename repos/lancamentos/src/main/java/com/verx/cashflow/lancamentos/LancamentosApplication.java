// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microsserviço A — Lançamentos (escrita).
 *
 * <p>Registra lançamentos e publica o evento {@code lancamento.criado} via
 * Transactional Outbox (implementado no Sprint 2/3 — fora do escopo do scaffold).
 */
@SpringBootApplication
public class LancamentosApplication {

    public static void main(String[] args) {
        SpringApplication.run(LancamentosApplication.class, args);
    }
}
