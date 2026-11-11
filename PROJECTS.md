<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# PROJECTS.md — Fonte Única da Estrutura de Repos

> **Gerado por `ralph-engine-onboarding` (generate-topology.sh).** Fonte única dos
> paths de repo — `AGENTS.md` (§5.2), `PERSONAS.md` (§1) e o `validate-framework.sh`
> derivam/validam contra ele.

```yaml
repos:
  - path: .
    persona: verx-architecture-challenge-dev
    license: LICENSE
    family: filho
    tech_stack: [Structurizr C4, MADR ADRs, GitHub Actions CI/CD]

  - path: repos/lancamentos
    persona: lancamentos-dev
    license: LICENSE
    family: filho
    tech_stack: [Java 21, Spring Boot 3, JDBI + Spring Data JDBC, PostgreSQL, Kafka (Transactional Outbox)]

  - path: repos/consolidado
    persona: consolidado-dev
    license: LICENSE
    family: filho
    tech_stack: [Java 21, Spring Boot 3, JDBI + Spring Data JDBC, PostgreSQL, Kafka (Inbox/idempotência)]

  - path: repos/infra
    persona: infra-dev
    license: LICENSE
    family: filho
    tech_stack: [Terraform, AWS (ECS Fargate, RDS, MSK Serverless, VPC, NAT, WAF, Cognito, Secrets Manager, KMS, CloudWatch/X-Ray)]
```
