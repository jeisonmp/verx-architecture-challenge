---
name: lancamentos-dev
description: "Skill do Microsserviço A — Lançamentos (escrita) da solução Fluxo de Caixa (CQRS). Ativa ao trabalhar em lançamentos, POST /lancamentos, Transactional Outbox ou na escrita do fluxo de caixa."
---

# lancamentos-dev — Lançamentos (Microsserviço A · Escrita)

> Skill neta de produto (filha de `verx-architecture-challenge-dev`). Repo:
> `repos/lancamentos/`. Modelo C4 e ADRs no projeto pai (raiz).

## Objetivo

Acelerar desenvolvimento, code review e debugging do **Microsserviço A —
Lançamentos**: recebe `POST /lancamentos`, grava lançamento + evento na mesma
transação (Transactional Outbox) e publica no tópico `lancamento.criado`.

## Ativação de Contexto

Ao ativar esta skill, carregue:

1. `repos/lancamentos/AGENTS.md`
2. `repos/lancamentos/README.md`
3. `workspace.dsl` (modelo C4) e `docs/adrs/` (ADR-0003, ADR-0006/0008, ADR-0015)

## Stack e Versões

| Tecnologia | Versão/Uso |
| ---------- | ---------- |
| Java | 21 |
| Spring Boot | 3 |
| JDBI + Spring Data JDBC | acesso a dados (sem ORM — ADR-0006/0008) |
| PostgreSQL | 16.3 |
| Kafka (MSK Serverless) | produtor do tópico `lancamento.criado` |

## Comandos Essenciais

```bash
# Sprint 1+ — build/teste consolidados quando o serviço existir (ver README do projeto pai)
```

## Anti-padrões

- ORM (Hibernate/JPA) no núcleo transacional — usar JDBI + Spring Data JDBC (ADR-0006/0008)
- Dual-write DB↔Kafka sem Outbox relay + idempotência
- Libs compartilhadas entre A↔B (regra anti-monólito distribuído — ADR-0015)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)
