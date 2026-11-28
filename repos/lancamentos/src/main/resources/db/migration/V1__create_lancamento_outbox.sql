-- Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
-- All rights reserved.
-- PROPRIETARY AND CONFIDENTIAL.
-- No part of this software may be reproduced, distributed, or transmitted
-- in any form or by any means without express written permission.

-- V1 — Schema transacional do Microsserviço A (Lançamentos).
-- Transacional Outbox: lancamento + outbox_event gravados na mesma transação (US-002);
-- o relay (US-003) publica os eventos PENDING no tópico lancamento.criado.

CREATE TABLE lancamento (
    id          UUID PRIMARY KEY,
    tipo        VARCHAR(10) NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    valor       NUMERIC(19,2) NOT NULL CHECK (valor > 0),
    data        TIMESTAMPTZ NOT NULL,
    descricao   VARCHAR(255) NOT NULL,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    aggregate_id   UUID NOT NULL,
    topic          VARCHAR(255) NOT NULL,
    payload        JSONB NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED')),
    tentativas     INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ NULL
);
