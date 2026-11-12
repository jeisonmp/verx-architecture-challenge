// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.infra.logging;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Mascara credenciais antes de escrever mensagens em log (ADR-0011, LGPD §2.1).
 *
 * <p>Cobre os formatos mais comuns de exposição acidental: headers HTTP
 * ({@code Authorization}, {@code Cookie}, {@code x-api-key}), tokens soltos
 * ({@code Bearer <token>}) e pares chave/valor ou JSON com nomes sensíveis.
 *
 * <p>Cópia própria do serviço B — a ADR-0015 proíbe lib compartilhada entre A e B.
 */
public final class SensitiveDataMasker {

    private static final String REDACTED = "[REDACTED]";

    private static final List<Masking> MASKINGS = List.of(
            // Authorization: Bearer <token> / Authorization=Basic <base64>
            new Masking(
                    Pattern.compile("(?i)(authorization\\s*[:=]\\s*)(?:bearer|basic)?\\s*[^\\s,;]+"),
                    "$1" + REDACTED),
            // Bearer <token> solto no texto
            new Masking(
                    Pattern.compile("(?i)\\bbearer\\s+[A-Za-z0-9\\-._~+/]+=*"),
                    "Bearer " + REDACTED),
            // Pares chave/valor de credenciais (x-api-key=..., token=..., password=...)
            new Masking(
                    Pattern.compile("(?i)((?:x-api-key|cookie|set-cookie|access_token|refresh_token"
                            + "|token|password|secret)\\s*[:=]\\s*)[^\\s,;]+"),
                    "$1" + REDACTED),
            // Valores JSON de campos sensíveis ("authorization": "...")
            new Masking(
                    Pattern.compile("(?i)(\"(?:authorization|x-api-key|cookie|access_token"
                            + "|refresh_token|token|password|secret)\"\\s*:\\s*)\"[^\"]*\""),
                    "$1\"" + REDACTED + "\""));

    private SensitiveDataMasker() {
    }

    /**
     * Retorna a mensagem com credenciais substituídas por {@code [REDACTED]}.
     *
     * @param message mensagem original (pode ser {@code null})
     * @return mensagem mascarada, ou a própria entrada quando nula/vazia
     */
    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String masked = message;
        for (Masking masking : MASKINGS) {
            masked = masking.pattern().matcher(masked).replaceAll(masking.replacement());
        }
        return masked;
    }

    private record Masking(Pattern pattern, String replacement) {
    }
}
