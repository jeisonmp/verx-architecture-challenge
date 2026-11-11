---
name: verx-architecture-challenge-dev
description: "Skill de produto da solução Fluxo de Caixa (CQRS) — projeto pai (raiz). Acelera desenvolvimento, code review, arquitetura e debugging do domínio de fluxo de caixa com CQRS, Transactional Outbox/Inbox e barramento Kafka. Ativa ao mencionar fluxo de caixa, CQRS, lançamentos, consolidado, outbox, inbox, consolidação ou o modelo C4/ADRs da solução."
---

# verx-architecture-challenge-dev — Fluxo de Caixa (CQRS) · Projeto Pai

> Skill neta de produto. Repo pai: raiz (C4, ADRs, CI). Repos filhos:
> `repos/lancamentos/` (A, escrita), `repos/consolidado/` (B, leitura),
> `repos/infra/` (IaC Terraform/AWS).

## Objetivo

Acelerar desenvolvimento, code review e decisões de arquitetura da solução
**Fluxo de Caixa (CQRS)** — escrita/leitura desacopladas via barramento Kafka e
Transactional Outbox/Inbox.

## Ativação de Contexto

Ao ativar esta skill, carregue:

1. `AGENTS.md`
2. `README.md`
3. `docs/ARCHITECTURE.md`
4. `docs/BUSINESS.md`
5. `docs/adrs/` (decisões MADR) e `workspace.dsl` (modelo C4)

## Stack e Versões

| Tecnologia | Uso |
| ---------- | --- |
| Structurizr DSL | modelo C4 (`workspace.dsl`) |
| MADR | ADRs em `docs/adrs/` |
| GitHub Actions | CI/CD (SAST/SCA/gitleaks → dev → master, semantic-release) |

## Comandos Essenciais

```bash
docker compose up   # sobe o Structurizr (C4 + ADRs) em http://localhost:8089
```

## Anti-padrões

- ORM (Hibernate/JPA) no núcleo transacional — usar JDBI + Spring Data JDBC (ADR-0006/0008)
- Modelo reativo (Vert.x) — descartado (ADR-0008)
- Dual-write DB↔Kafka sem Outbox relay + idempotência
- Libs compartilhadas entre os serviços A↔B (regra anti-monólito distribuído)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)
