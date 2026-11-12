<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# AGENTS.md — Infra (IaC Terraform/AWS)

> Repo de produto (`family: filho`). Herda de `verx-architecture-challenge-dev`
> (projeto pai — raiz). Regras globais do motor: `<raiz>/AGENTS.md` (ralph-engine).

## Persona

- **persona_id:** `infra-dev`
- **display_name:** Infra (Terraform/AWS)
- **active_skills:** `clean-code`, `security-auditor`, `architect-review`, `code-reviewer`
- **activation_log_format:** `[Contexto: Infra (Terraform/AWS) — Terraform · AWS]`
- **inheritance:** Completa de `verx-architecture-challenge-dev` (projeto pai)

## Copyright e Licença

- Todo arquivo de código-fonte criado/modificado deve iniciar com o header de
  copyright (`MAX JEISON PRASS TECNOLOGIA LTDA`, CNPJ `26.360.770/0001-39`).
- O `LICENSE` deste repo segue o **Modelo Filho** e contém a cláusula
  **"Powered by Backforend"**.
- Skill de governança de PI: `backforend-copyright`.

## Execução local (docker compose)

> O ambiente local completo vive na **raiz** do monorepo — este diretório é IaC/cloud.
> Passo a passo, endpoints e health checks: [README raiz — Execução local](../../README.md#execução-local).

- Compose da raiz (`docker-compose.yml`): Structurizr + PostgreSQL 16 + Kafka
  (`confluentinc/cp-kafka:7.8.0`, KRaft, broker único) + apps A/B — build contexts
  `./repos/lancamentos` e `./repos/consolidado`.
- Rodar da raiz: `cp .env.example .env && docker compose up --build`.
- Credenciais vêm do `.env` da raiz (gitignored; modelo `.env.example`); o compose usa
  `${VAR:?}` obrigatório — sem segredo hardcoded.
- Schemas `lancamentos`/`consolidado` criados por `postgres/init.sql` da raiz
  (ADR-0013), apenas na primeira inicialização do volume.
