# Infra — IaC (Terraform) · documentado, não implementado

> Fonte: **ADR-0012**. Este diretório documenta a infraestrutura como código (IaC) da solução.
> **Não é implementado** nesta challenge — apenas a estrutura de pastas e o que ela terá.
> Este README é vivo: atualizado a cada decisão da ADR-0012.

## Organização dos repositórios (monorepo)

```
repos/
├── servico-a/   # Microsserviço A — Lançamentos (POST /lancamentos + Transactional Outbox)
├── servico-b/   # Microsserviço B — Consolidado (GET /consolidado + Inbox/idempotência)
└── infra/       # IaC (Terraform) — documentado, não implementado
    ├── README.md
    └── terraform/
        ├── main.tf
        ├── variables.tf
        ├── outputs.tf
        ├── modules/            # vpc, rds, msk, ecs, edge, iam
        └── environments/
            ├── dev/            # minimizado (FinOps)
            ├── qas/
            └── prd/            # alvo completo (com WAF)
```

## Arquitetura de infra (alvo — produção)

A visão C4 de deployment está no `workspace.dsl` (`deployment * aws`). Estrutura:

- **Borda:** `API Gateway` + `AWS WAF` (só produção) + `Amazon Cognito` (autenticação JWT + refresh tokens).
- **VPC:** sub-rede privada com **ECS Fargate** (serviços A e B), **RDS PostgreSQL** (A e B) e **MSK Serverless** (Kafka `lancamento.criado` + DLQ).
- **Serviços gerenciados:** `Secrets Manager` (fallback), `KMS` (criptografia em repouso), `CloudWatch` + `X-Ray` (observabilidade).
- **Identidade:** `task role` (ECS Fargate) para RDS IAM + MSK IAM (passwordless, ADR-0007).

## FinOps

- **Ferramenta:** AWS Pricing Calculator (link consolidado na Sprint 7).
- **Alvo dev:** ~US$ 30/mês.
- **Regras dev:**
  - sem 24/7 — `desired_count = 0` fora do horário;
  - RDS `db.t4g.micro` single-AZ (sem Multi-AZ / Performance Insights);
  - MSK Serverless (sem custo de cluster fixo);
  - egress = **NAT Instance `t3.micro`** (free tier), sem NAT Gateway;
  - **WAF apenas em produção**; `dev`/`qas` acessados por notebook via VPN (VPC);
  - sem Container Insights / CloudFront / Multi-AZ em dev.

## Estado (state backend)

- **S3** (versioning) — obrigatório para state remoto.
- **DynamoDB** (lock) — **opcional**; dispensável no desafio solo, adicionar se houver equipe/CI concorrente.
