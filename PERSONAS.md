<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# PERSONAS.md — Centralized Persona Registry

> **Gerado por `ralph-engine-onboarding` (generate-topology.sh).** Fonte única das personas.

---

## 1. QUICK-LOOKUP TABLE: Directory → Persona

| Directory Path     | persona_id  | display_name | Activation Log |
| ------------------ | ----------- | ------------ | -------------- |
| `<raiz>/ia/ralph/` | `ralph`     | Ralph        | `[Contexto: Ralph — Autonomous Coding Agent]` |
| `.` | `verx-architecture-challenge-dev` | Verx Architecture Challenge | `[Contexto: Verx Architecture Challenge — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]` |
| `repos/lancamentos/` | `lancamentos-dev` | Lançamentos (Escrita) | `[Contexto: Lançamentos (Escrita) — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]` |
| `repos/consolidado/` | `consolidado-dev` | Consolidado (Leitura) | `[Contexto: Consolidado (Leitura) — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]` |
| `repos/infra/` | `infra-dev` | Infra (Terraform/AWS) | `[Contexto: Infra (Terraform/AWS) — Terraform · AWS]` |

---

## 2. Loading Protocol

> **Fonte única de resolução:** §1 (QUICK-LOOKUP). Protocolo completo no `AGENTS.md` global.

---

## 3. Persona Entries

### 3.1 ralph — Ralph (Autonomous Agent)

| Field                     | Value |
| ------------------------- | ----- |
| **persona_id**            | `ralph` |
| **display_name**          | Ralph |
| **repo_path**             | `<raiz>/ia/ralph/` |
| **active_skills**         | `ralph`, `prd`, `ralph-engine-onboarding` |
| **activation_log_format** | `[Contexto: Ralph — Autonomous Coding Agent]` |
| **inheritance**           | Root — no parent |

### 3.2 verx-architecture-challenge-dev — Verx Architecture Challenge (Projeto Pai)

| Field                     | Value |
| ------------------------- | ----- |
| **persona_id**            | `verx-architecture-challenge-dev` |
| **display_name**          | Verx Architecture Challenge |
| **repo_path**             | `.` |
| **active_skills**         | `architect-review`, `security-auditor`, `docker-expert`, `microservices-patterns`, `github-actions-templates` |
| **activation_log_format** | `[Contexto: Verx Architecture Challenge — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]` |
| **inheritance**           | Completa de `<raiz>/AGENTS.md` |

**directory_routing:**

| Diretório | Contexto |
| --------- | -------- |
| `docs/adrs/` | Decisões de arquitetura (MADR) |
| `docs/` | C4 (`workspace.dsl`) + documentação local |
| `.github/workflows/` | Pipelines CI (feat/fix/* → dev → master) |
| `repos/` | Repos filhos: `lancamentos/` (A, escrita) · `consolidado/` (B, leitura) · `infra/` (IaC) |

**anti_patterns:**

- ORM (Hibernate/JPA) no núcleo transacional — usar JDBI + Spring Data JDBC (ADR-0006/0008)
- Modelo reativo (Vert.x) — descartado (ADR-0008)
- Dual-write DB↔Kafka sem Outbox relay + idempotência
- Libs compartilhadas entre os serviços A↔B (regra anti-monólito distribuído)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)

### 3.3 lancamentos-dev — Lançamentos (Microsserviço A · Escrita)

| Field                     | Value |
| ------------------------- | ----- |
| **persona_id**            | `lancamentos-dev` |
| **display_name**          | Lançamentos (Escrita) |
| **repo_path**             | `repos/lancamentos/` |
| **active_skills**         | `clean-code`, `security-auditor`, `architect-review`, `code-reviewer` |
| **activation_log_format** | `[Contexto: Lançamentos (Escrita) — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]` |
| **inheritance**           | Completa de `verx-architecture-challenge-dev` (projeto pai) |

**anti_patterns:**

- ORM (Hibernate/JPA) no núcleo transacional — usar JDBI + Spring Data JDBC (ADR-0006/0008)
- Dual-write DB↔Kafka sem Outbox relay + idempotência
- Libs compartilhadas entre os serviços A↔B (regra anti-monólito distribuído — ADR-0015)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)

### 3.4 consolidado-dev — Consolidado (Microsserviço B · Leitura)

| Field                     | Value |
| ------------------------- | ----- |
| **persona_id**            | `consolidado-dev` |
| **display_name**          | Consolidado (Leitura) |
| **repo_path**             | `repos/consolidado/` |
| **active_skills**         | `clean-code`, `security-auditor`, `architect-review`, `code-reviewer` |
| **activation_log_format** | `[Contexto: Consolidado (Leitura) — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]` |
| **inheritance**           | Completa de `verx-architecture-challenge-dev` (projeto pai) |

**anti_patterns:**

- ORM (Hibernate/JPA) na projeção — usar JDBI + Spring Data JDBC (ADR-0006/0008)
- Processar evento sem Inbox/idempotência (reprocessamento/duplicidade)
- Libs compartilhadas entre os serviços A↔B (regra anti-monólito distribuído — ADR-0015)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)

### 3.5 infra-dev — Infra (IaC Terraform/AWS)

| Field                     | Value |
| ------------------------- | ----- |
| **persona_id**            | `infra-dev` |
| **display_name**          | Infra (Terraform/AWS) |
| **repo_path**             | `repos/infra/` |
| **active_skills**         | `clean-code`, `security-auditor`, `architect-review`, `code-reviewer` |
| **activation_log_format** | `[Contexto: Infra (Terraform/AWS) — Terraform · AWS]` |
| **inheritance**           | Completa de `verx-architecture-challenge-dev` (projeto pai) |

**anti_patterns:**

- NAT Gateway em dev — usar NAT Instance `t3.micro` free-tier (ADR-0012)
- Recursos 24/7 em dev — RDS stop/start 10h/dia (ADR-0014); NAT stop/start 10h/dia (ADR-0016)
- Credenciais estáticas no pipeline — OIDC + task role (ADR-0005/0007)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)

---

**Skill Activation by Project (from `prd.json["project"]`):**

> **FONTE DA VERDADE ÚNICA.** O campo `project` do frontmatter do PRD (2ª coluna) é a chave que o sidecar `ralph-mcp` e a skill `ralph` resolvem para ativar a cadeia de skills. Mantê-la em paridade com `ia/graphrag/repos.py`.

| Repo afetado | `project` field | Skill Activation Chain |
| ------------ | --------------- | ---------------------- |
| `.` | `raiz` | `backforend` → `verx-architecture-challenge-dev` |
| `repos/lancamentos/` | `lancamentos` | `backforend` → `lancamentos-dev` |
| `repos/consolidado/` | `consolidado` | `backforend` → `consolidado-dev` |
| `repos/infra/` | `infra` | `backforend` → `infra-dev` |

**Skill Activation by Story Type (from `prd.json` story `type` field):**

| Story `type` | Skill Activation Chain |
| ------------ | ---------------------- |
| `bugfix` | chain do `project` → `backforend-bug-hunter` |
| `investigation` | chain do `project` → `backforend-bug-hunter` (modo evidência, SEM fix) |
| `feature` | chain do `project` — TDD RED |
| `docs` | chain do `project` — sem TDD RED |
