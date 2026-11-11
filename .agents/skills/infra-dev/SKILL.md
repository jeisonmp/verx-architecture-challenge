---
name: infra-dev
description: "Skill de infraestrutura (IaC Terraform/AWS) da solução Fluxo de Caixa (CQRS). Ativa ao trabalhar em Terraform, AWS (ECS Fargate, RDS, MSK Serverless, VPC, NAT, WAF, Cognito), FinOps ou provisionamento da infra da solução."
---

# infra-dev — Infra (IaC Terraform/AWS)

> Skill neta de produto (filha de `verx-architecture-challenge-dev`). Repo:
> `repos/infra/`. Fonte da decisão: `docs/adrs/0012` (IaC/FinOps). Sem `apply`
> — a mutação de estado de nuvem é sempre do usuário.

## Objetivo

Acelerar desenvolvimento, code review e debugging da **infraestrutura como código
(Terraform)** da solução — VPC, ECS Fargate, RDS PostgreSQL, MSK Serverless, WAF,
Cognito, Secrets Manager, KMS e observabilidade (CloudWatch/X-Ray).

## Ativação de Contexto

Ao ativar esta skill, carregue:

1. `repos/infra/AGENTS.md`
2. `repos/infra/README.md`
3. `workspace.dsl` (visão deployment L3) e `docs/adrs/0012` + `docs/finops/`

## Stack e Versões

| Tecnologia | Uso |
| ---------- | --- |
| Terraform | IaC — estado remoto S3 + locking `use_lockfile` |
| AWS | ECS Fargate, RDS, MSK Serverless, VPC, WAF, Cognito, Secrets Manager, KMS |
| GitHub Actions | `terraform validate` + tfsec/checkov (sem apply) |

## Comandos Essenciais

```bash
terraform validate && tfsec .     # validação estática (CI)
```

## FinOps (dev)

- Sem recursos 24/7 — RDS/NAT com stop/start 10h/dia (ADR-0014).
- Egress = NAT Instance `t3.micro` free-tier, **sem NAT Gateway** em dev.
- WAF apenas em produção; MSK Serverless (sem custo fixo de cluster).

## Anti-padrões

- NAT Gateway em dev — usar NAT Instance `t3.micro` free-tier (ADR-0014)
- Recursos 24/7 em dev — RDS/NAT com stop/start 10h/dia (ADR-0014)
- Credenciais estáticas no pipeline — OIDC + task role (ADR-0005/0007)
- Secrets hardcoded — usar Secrets Manager (ADR-0007)
