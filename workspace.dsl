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
        cliente = person "Cliente" "Usuário/Integrador que lança e consulta o fluxo de caixa (UI/cliente ou integração api_key)"

        gateway = softwareSystem "API Gateway + WAF" "Borda única — WAF, rate limiting, TLS 1.2+, autenticação Cognito e api_key" "Amazon API Gateway + AWS WAF"

        lancamentos = softwareSystem "CashFlow · Lançamentos" "Microsserviço A — escrita (POST + Transactional Outbox)" {
            api = container "API de Lançamentos" "Recebe POST /lancamentos" "Java 21 / Spring Boot"
            outboxWorker = container "Outbox Worker" "Lê a tabela outbox e publica no Kafka" "Java 21 / Spring Boot"
            db = container "DB Lançamentos" "PostgreSQL — lançamentos + tabela outbox" "PostgreSQL"
        }

        broker = softwareSystem "Kafka" "Barramento de eventos (escrita → leitura)" {
            topic = container "Tópico lancamento.criado" "Evento de lançamento criado" "Apache Kafka"
            dlq = container "Tópico lancamento.dlq" "Eventos não processáveis após retries (dead letter)" "Apache Kafka"
        }

        consolidado = softwareSystem "CashFlow · Consolidado" "Microsserviço B — leitura/projeção (Inbox + idempotência)" {
            api = container "API de Consulta" "GET /consolidado — 50 req/s" "Java 21 / Spring Boot"
            consumer = container "Consumer" "Consome eventos, aplica Inbox e atualiza a projeção" "Java 21 / Spring Boot"
            db = container "DB Consolidado" "Projeção de saldo otimizada para leitura" "PostgreSQL"
        }

        cliente -> gateway "Faz lançamento (POST) e consulta (GET)"
        gateway -> lancamentos.api "POST /lancamentos (JWT/Cognito)"
        gateway -> consolidado.api "GET /consolidado (JWT ou api_key)"
        lancamentos.api -> lancamentos.db "Grava lançamento + evento (outbox, mesma transação)"
        lancamentos.outboxWorker -> lancamentos.db "Lê eventos pendentes"
        lancamentos.outboxWorker -> broker.topic "Publica evento"
        broker.topic -> consolidado.consumer "Entrega evento"
        consolidado.consumer -> consolidado.db "Atualiza projeção (idempotente)"
        consolidado.consumer -> broker.dlq "Desvia evento não processável (retry excedido)"
        consolidado.api -> consolidado.db "Lê projeção"

        aws = deploymentEnvironment "AWS — Alvo (produção)" {
            deploymentNode "Amazon Web Services" "Provedor de nuvem" {
                deploymentNode "us-east-1" "Região" {
                    infrastructureNode "AWS WAF" "Proteção ativa — rate limiting (apenas produção)" "AWS WAF"
                    softwareSystemInstance gateway
                    infrastructureNode "Amazon Cognito" "Autenticação (JWT) + refresh tokens" "Amazon Cognito"

                    deploymentNode "VPC" "Rede isolada dos serviços" {
                        deploymentNode "Sub-rede privada" {
                            deploymentNode "ECS Fargate — Serviço A" "Lançamentos" "AWS Fargate" {
                                containerInstance lancamentos.api
                                containerInstance lancamentos.outboxWorker
                            }
                            deploymentNode "ECS Fargate — Serviço B" "Consolidado" "AWS Fargate" {
                                containerInstance consolidado.api
                                containerInstance consolidado.consumer
                            }
                        }
                        deploymentNode "Amazon RDS" "PostgreSQL" "Amazon RDS" {
                            deploymentNode "PostgreSQL — Lançamentos" {
                                containerInstance lancamentos.db
                            }
                            deploymentNode "PostgreSQL — Consolidado" {
                                containerInstance consolidado.db
                            }
                        }
                        deploymentNode "Amazon MSK Serverless" "Kafka — lancamento.criado + DLQ" "Amazon MSK" {
                            softwareSystemInstance broker
                        }
                    }

                    infrastructureNode "AWS Secrets Manager" "Fallback para segredos não IAM-ificados" "AWS Secrets Manager"
                    infrastructureNode "AWS KMS" "Gestão de chaves (criptografia em repouso)" "AWS KMS"
                    infrastructureNode "CloudWatch + X-Ray" "Logs, métricas e tracing" "Amazon CloudWatch"
                }
            }
        }
    }

    views {
        systemLandscape ecossistema "Visão do Ecossistema (L1)" {
            include *
        }
        systemContext lancamentos contexto-lancamentos "Contexto — Lançamentos (L1)" {
            include *
        }
        systemContext consolidado contexto-consolidado "Contexto — Consolidado (L1)" {
            include *
        }
        container lancamentos containers-lancamentos "Containers — Lançamentos (L2)" {
            include *
        }
        container consolidado containers-consolidado "Containers — Consolidado (L2)" {
            include *
        }
        deployment * aws deployment-infra "Deployment — Infra AWS (L3)" {
            include *
        }
    }

    !adrs ./docs/adrs
}
