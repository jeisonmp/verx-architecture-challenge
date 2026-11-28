// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.domain;

import java.util.UUID;

/**
 * Projeção de leitura de uma linha {@code PENDING} de {@code outbox_event},
 * reivindicada pelo relay (US-003) para publicação no Kafka.
 *
 * <p>{@code payload} é o JSON do contrato CDC {@code lancamento.criado} já
 * serializado na escrita ({@code schemaVersion=1}); o relay publica o valor
 * verbatim como value da mensagem.
 */
public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String topic,
        String payload) {
}
