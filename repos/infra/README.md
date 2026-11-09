# Infra — IaC (Terraform) · declarado em .tf (sem apply)

> Fonte: **ADR-0012**. Este diretório contém a infraestrutura como código (IaC) da solução.
> Os arquivos `.tf` são **declarados, mas não aplicados** (não há conta AWS real na challenge).
> Este README é vivo: atualizado a cada decisão da ADR-0012.

## Organização dos repositórios (monorepo)

```
repos/
├── servico-a/   # Microsserviço A — Lançamentos (POST /lancamentos + Transactional Outbox)
├── servico-b/   # Microsserviço B — Consolidado (GET /consolidado + Inbox/idempotência)
└── infra/       # IaC (Terraform) — declarado, sem apply
    ├── README.md
    ├── backend.tf     # state remoto S3 + locking nativo (use_lockfile = true)
    ├── providers.tf   # provider AWS + required_version >= 1.10
    ├── variables.tf   # variáveis (região, ambiente, RDS, Fargate)
    └── main.tf        # VPC, RDS PostgreSQL, MSK Serverless, ECS Fargate, KMS, IAM
```

## Arquitetura de infra (alvo — produção)

A visão C4 de deployment está no `workspace.dsl` (`deployment * aws`). Estrutura:

- **Borda:** `API Gateway` + `AWS WAF` (só produção) + `Amazon Cognito` (autenticação JWT + refresh tokens).
- **VPC:** sub-rede privada com **ECS Fargate** (serviços A e B), **RDS PostgreSQL** (A e B) e **MSK Serverless** (Kafka `lancamento.criado` + DLQ).
- **Serviços gerenciados:** `Secrets Manager` (fallback), `KMS` (criptografia em repouso), `CloudWatch` + `X-Ray` (observabilidade).
- **Identidade:** `task role` (ECS Fargate) para RDS IAM + MSK IAM (passwordless, ADR-0007).

## Estado (state backend)

- **S3** (versioning) — estado remoto.
- **Locking nativo via `use_lockfile = true`** (Terraform 1.10+) — **sem DynamoDB**. Usa S3 conditional writes (`.tflock`).

## FinOps

- **Ferramenta:** AWS Pricing Calculator — estimativa e link oficial em `docs/finops/`.
- **Alvo dev:** ~US$ 30/mês.
- **Regras dev:**
  - sem 24/7 — `desired_count = 0` fora do horário;
  - RDS `db.t4g.micro` single-AZ (sem Multi-AZ / Performance Insights);
  - MSK Serverless (sem custo de cluster fixo);
  - egress = **NAT Instance `t3.micro`** (free tier), sem NAT Gateway;
  - **WAF apenas em produção**; `dev`/`qas` acessados por notebook via VPN (VPC);
  - sem Container Insights / CloudFront / Multi-AZ em dev.
