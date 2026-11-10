<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# ARCHITECTURE — Fluxo de Caixa (CQRS)

> Arquitetura-alvo da solução. Modelo C4 (L1/L2/L3) mantido no Structurizr
> (`workspace.dsl`) e decisões registradas em `docs/adrs/` (MADR).

## Visão geral

Solução CQRS com **dois microsserviços** desacoplados por **Kafka** (ADR-0003):

- **A — Lançamentos (escrita):** `POST /lancamentos` grava o lançamento e o evento na
  mesma transação (Transactional Outbox); um worker publica no Kafka.
- **B — Consolidado (leitura):** consome o evento, aplica Inbox/idempotência e atualiza
  a projeção de saldo; serve `GET /consolidado` a 50 req/s.

O modelo C4 formal (Contexto L1, Containers L2, Deployment L3) vive em `workspace.dsl`
e é visualizado no Structurizr local (ADR-0002).

## Stack (congelada na Sprint 0)

| Camada | Decisão | ADR |
|---|---|---|
| Runtime/linguagem | Java 21 (LTS) + Spring Boot 3 | 0006/0008 |
| Acesso a dados | JDBI (núcleo transacional) + Spring Data JDBC (CRUD), sem ORM | 0006/0008 |
| Mensageria | Kafka — MSK Serverless (`lancamento.criado` + DLQ) | 0003/0014 |
| Banco | PostgreSQL RDS — instância única, schemas por serviço (dev) | 0004/0013 |
| Orquestração | ECS Fargate (sem EKS) | 0010 |
| Borda/segurança | API Gateway + WAF + Cognito + Secrets Manager/IAM | 0007 |
| Observabilidade | Logs JSON + CloudWatch + X-Ray (ADOT) | 0011 |
| IaC/FinOps | Terraform em `repos/infra/` (sem apply) + `docs/finops/` | 0012 |
| CI/CD | GitHub Actions (dev → master) + semantic-release | 0005 |

## Fluxo de dados (end-to-end)

```
cliente → API Gateway → A.api (POST /lancamentos)
   → A.db (lançamento + outbox, mesma transação)
   → A.outboxWorker → Kafka (lancamento.criado)
   → B.consumer (Inbox idempotente) → B.db (projeção)
   → B.api (GET /consolidado) ← cliente
```

- **Consistência na saída:** Transactional Outbox (ADR-0003/0006).
- **Entrega desacoplada:** Kafka, ordenação por chave (id do lançamento) (ADR-0003).
- **Idempotência na entrada:** Inbox + unique constraint (ADR-0006).
- **Falha não processável:** retries → DLQ `lancamento.dlq` (ADR-0003/0014).

## Regras arquiteturais transversais

- **Anti-monólito distribuído:** A e B não compartilham biblioteca de código; o único
  acoplamento em runtime é o contrato do evento, versionado por `schemaVersion`
  obrigatório (ADR-0015).
- **Segurança em camadas (defense in depth):** borda única (API Gateway + WAF), Cognito
  (JWT + refresh token rotativo), `api_key` para integração, passwordless via RDS IAM /
  MSK IAM, Secrets Manager como fallback e KMS em repouso (ADR-0007).
- **Observabilidade:** correlation ID/trace atravessa o Kafka via headers do evento
  (ADR-0011).

## Implantação (L3)

`workspace.dsl` descreve o deployment AWS: VPC + sub-redes, ECS Fargate (A e B), ALB,
RDS (instância única), MSK Serverless, API Gateway + WAF, Cognito, Secrets Manager, KMS,
CloudWatch + X-Ray e NAT Instance (dev). Detalhes de infra em `repos/infra/` e
`docs/ROADMAP.md`.
