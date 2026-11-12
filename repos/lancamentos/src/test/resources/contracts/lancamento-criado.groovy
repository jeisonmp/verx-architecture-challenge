// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package contracts

import org.springframework.cloud.contract.spec.Contract

/*
 * Contrato CDC do evento `lancamento.criado` (produtor A — Lançamentos).
 *
 * ADR-0015: A e B não compartilham biblioteca de código — o único acoplamento
 * em runtime é este contrato, versionado por `schemaVersion` (obrigatório).
 * O stub gerado é test-only e consumido por B via Spring Cloud Contract Stub Runner.
 */
Contract.make {
    name("lancamentoCriado")
    label("lancamento.criado")
    input {
        triggeredBy("criarLancamento()")
    }
    outputMessage {
        sentTo("lancamento.criado")
        body([
            // ADR-0015: schemaVersion é obrigatório e fixado em 1 — a ausência
            // (ou versão desconhecida) faz B rejeitar a mensagem (DLQ).
            // Asserção mandatória: o produtor deve sempre publicar exatamente 1.
            schemaVersion: 1,
            eventId: $(anyUuid()),
            eventType: "lancamento.criado",
            occurredAt: $(anyIso8601WithOffset()),
            lancamento: [
                id: $(anyUuid()),
                tipo: $(anyOf("ENTRADA", "SAIDA")),
                valor: $(anyNumber()),
                data: $(anyIso8601WithOffset()),
                descricao: $(anyNonBlankString())
            ]
        ])
        headers {
            header("contentType", "application/json")
        }
    }
}
