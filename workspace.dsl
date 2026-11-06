/*
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
*/

/*
Regra: qualquer alteração neste arquivo exige consulta prévia à documentação
oficial do Structurizr (docs/refs/structurizr.github.io) e c4-model (src/modelos/c4-model.md).
Ver ADR-0009.
*/

workspace "Fluxo de Caixa" "Solução CQRS com Outbox/Inbox" {

    !identifiers hierarchical

    model {
        cliente = person "Cliente" "Usuário que lança e consulta o fluxo de caixa"

        lancamentos = softwareSystem "CashFlow · Lançamentos" "Microsserviço A — escrita (POST + Transactional Outbox)" {
            api = container "API de Lançamentos" "Recebe POST /lancamentos" "Java 21 / Spring Boot"
            outboxWorker = container "Outbox Worker" "Lê a tabela outbox e publica no Kafka" "Java 21 / Spring Boot"
            db = container "DB Lançamentos" "PostgreSQL — lançamentos + tabela outbox" "PostgreSQL"
        }

        broker = softwareSystem "Kafka" "Barramento de eventos (escrita → leitura)" {
            topic = container "Tópico lancamento.criado" "Evento de lançamento criado" "Apache Kafka"
        }

        consolidado = softwareSystem "CashFlow · Consolidado" "Microsserviço B — leitura/projeção (Inbox + idempotência)" {
            api = container "API de Consulta" "GET /consolidado — 50 req/s" "Java 21 / Spring Boot"
            consumer = container "Consumer" "Consome eventos, aplica Inbox e atualiza a projeção" "Java 21 / Spring Boot"
            db = container "DB Consolidado" "Projeção de saldo otimizada para leitura" "PostgreSQL"
        }

        cliente -> lancamentos.api "Faz lançamento (POST)"
        lancamentos.api -> lancamentos.db "Grava lançamento + evento (outbox, mesma transação)"
        lancamentos.outboxWorker -> lancamentos.db "Lê eventos pendentes"
        lancamentos.outboxWorker -> broker.topic "Publica evento"
        broker.topic -> consolidado.consumer "Entrega evento"
        consolidado.consumer -> consolidado.db "Atualiza projeção (idempotente)"
        cliente -> consolidado.api "Consulta saldo consolidado (GET)"
        consolidado.api -> consolidado.db "Lê projeção"
    }

    views {
        systemLandscape ecossistema "Visão do Ecossistema (L1)" {
        }
        systemContext lancamentos contexto-lancamentos "Contexto — Lançamentos (L1)" {
        }
        systemContext consolidado contexto-consolidado "Contexto — Consolidado (L1)" {
        }
        container lancamentos containers-lancamentos "Containers — Lançamentos (L2)" {
        }
        container consolidado containers-consolidado "Containers — Consolidado (L2)" {
        }
    }

    !adrs ./docs/adrs
}
