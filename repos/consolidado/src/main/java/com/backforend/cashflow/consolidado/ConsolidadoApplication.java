// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.backforend.cashflow.consolidado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do microsserviço de leitura (Consolidado).
 */
@SpringBootApplication
public class ConsolidadoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsolidadoApplication.class, args);
    }
}
