// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Regressão da redação LGPD (AC-3): credenciais nunca devem chegar ao log.
 */
class SensitiveDataMaskerTest {

    @Test
    void redigeHeaderAuthorizationComBearer() {
        String masked = SensitiveDataMasker.mask("Authorization: Bearer abc.def.ghi");

        assertThat(masked).doesNotContain("abc.def.ghi").contains("[REDACTED]");
    }

    @Test
    void redigeXApiKeyECookie() {
        String masked = SensitiveDataMasker.mask("x-api-key=S3cr3t Cookie=session=abc123");

        assertThat(masked).doesNotContain("S3cr3t").doesNotContain("abc123");
    }

    @Test
    void redigeTokenEmJson() {
        String masked = SensitiveDataMasker.mask("{\"access_token\":\"s3cr3t-value\"}");

        assertThat(masked).doesNotContain("s3cr3t-value").contains("[REDACTED]");
    }

    @Test
    void preservaMensagemSemDadosSensiveis() {
        String message = "HTTP GET /actuator/health -> 200 (3 ms)";

        assertThat(SensitiveDataMasker.mask(message)).isEqualTo(message);
    }
}
