---
name: consolidado-dev
description: "Skill do Microsserviço B — Consolidado (leitura) da solução Fluxo de Caixa (CQRS). Ativa ao trabalhar em consolidado, GET /consolidado, Inbox, idempotência, projeção de saldo ou na leitura/consulta do fluxo de caixa."
---

# consolidado-dev — Consolidado (Microsserviço B · Leitura)

> Skill neta de produto (filha de `verx-architecture-challenge-dev`). Repo:
> `repos/consolidado/`. Modelo C4 e ADRs no projeto pai (raiz).

## Objetivo

Acelerar desenvolvimento, code review e debugging do **Microsserviço B —
Consolidado**: consome o tópico `lancamento.criado`, aplica **Inbox** com
idempotência e mantém a projeção de saldo. Expõe `GET /consolidado` (50 req/s).

## Ativação de Contexto

Ao ativar esta skill, carregue:

1. `repos/consolidado/AGENTS.md`
2. `repos/consolidado/README.md`
3. `workspace.dsl` (modelo C4) e `docs/adrs/` (ADR-0003, ADR-0006/0008, ADR-0015)

## Stack e Versões

| Tecnologia | Versão/Uso |
| ---------- | ---------- |
| Java | 21 |
| Spring Boot | 3 |
| JDBI + Spring Data JDBC | acesso a dados (sem ORM — ADR-0006/0008) |
| PostgreSQL | 16.3 |
| Kafka (MSK Serverless) | consumidor `lancamento.criado` + DLQ |

## Comandos Essenciais

```bash
# Sprint 1+ — build/teste consolidados quando o serviço existir (ver README do projeto pai)
```

## Anti-padrões

- ORM (Hibernate/JPA) na projeção — usar JDBI + Spring Data JDBC (ADR-0006/0008)
- Processar evento sem Inbox/idempotência (reprocessamento/duplicidade)
- Libs compartilhadas entre A↔B (regra anti-monólito distribuído — ADR-0015)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)
