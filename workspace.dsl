/*
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
*/

workspace "Fluxo de Caixa" "Solução CQRS com Outbox/Inbox" {

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
        systemLandscape "Visão do Ecossistema" {
            include *
        }
        systemContext lancamentos "Contexto — CashFlow (L1)" {
            include *
        }
        container lancamentos "Containers — Lançamentos (L2)" {
            include *
        }
        container consolidado "Containers — Consolidado (L2)" {
            include *
        }
        theme default
    }

    !adrs ./docs/adrs
}
