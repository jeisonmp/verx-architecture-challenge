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
    ├── backend.tf     # state remoto S3 + locking nativo (use_lockfile = true) + workspace_key_prefix
    ├── providers.tf   # provider AWS + required_version >= 1.10
    ├── variables.tf   # variáveis (região, ambiente, RDS, Fargate)
    ├── meta.tf        # locals — lê environments/<env>.yaml (config por ambiente)
    ├── main.tf        # VPC (+ IGW/NAT Instance), RDS PostgreSQL, MSK Serverless, ECS Fargate, KMS, IAM
    ├── rds_scheduler.tf  # RDS stop/start 10h/dia (dev) — EventBridge + Lambda (ADR-0014)
    ├── nat_scheduler.tf  # NAT Instance stop/start 10h/dia (dev) — EventBridge + Lambda (ADR-0014)
    ├── lambda/rds_scheduler/  # código Python do stop/start
    ├── lambda/nat_scheduler/  # código Python do stop/start da NAT
    └── environments/  # dev.yaml / qas.yaml / prd.yaml — flags FinOps por ambiente
```

## Arquitetura de infra (alvo — produção)

A visão C4 de deployment está no `workspace.dsl` (`deployment * aws`). Estrutura:

- **Borda:** `API Gateway` + `AWS WAF` (só produção) + `Amazon Cognito` (autenticação JWT + refresh tokens).
- **VPC:** sub-redes privadas (Fargate/RDS/MSK) + sub-rede pública com **NAT Instance** (egress free-tier); **ECS Fargate** (serviços A e B), **RDS PostgreSQL** (A e B) e **MSK Serverless** (Kafka `lancamento.criado` + DLQ).
- **Serviços gerenciados:** `Secrets Manager` (fallback), `KMS` (criptografia em repouso), `CloudWatch` + `X-Ray` (observabilidade).
- **Identidade:** `task role` (ECS Fargate) para RDS IAM + MSK IAM (passwordless, ADR-0007).

## Estado (state backend)

- **S3** (versioning) — estado remoto.
- **Locking nativo via `use_lockfile = true`** (Terraform 1.10+) — **sem DynamoDB**. Usa S3 conditional writes (`.tflock`).
- **Configuração parcial:** `bucket`/`region` via `-backend-config` no `init` (backend não aceita variável).
- **Isolamento por ambiente:** `workspace_key_prefix = "env"` + workspaces `dev`/`qas`/`prd` (ADR-0012 dec5).

## FinOps

- **Ferramenta:** AWS Pricing Calculator — estimativa e link oficial em `docs/finops/`.
- **Alvo dev:** ~US$ 30/mês.
- **Regras dev:**
  - sem 24/7 — `desired_count = 0` fora do horário;
  - RDS `db.t4g.micro` single-AZ (sem Multi-AZ / Performance Insights);
  - RDS stop/start 10h/dia em dev (`rds_scheduler.tf` — EventBridge + Lambda, ADR-0014);
  - MSK Serverless (sem custo de cluster fixo);
  - egress = **NAT Instance `t3.micro`** on-demand 10h/dia (US$ 3,16/mês; US$ 0 no free tier) declarada no `main.tf`, sem NAT Gateway — stop/start 10h/dia via `nat_scheduler.tf` (ADR-0014);
  - **WAF apenas em produção**; `dev`/`qas` acessados por notebook via VPN (VPC);
  - sem Container Insights / CloudFront / Multi-AZ em dev.

## Bootstrap — identidade do pipeline (fora do Terraform)

> **Premissa (ADR-0005 + ADR-0007):** o pipeline de IaC roda no GitHub Actions e
> **não carrega credencial estática** — a autenticação é via **OIDC**: o job assume
> uma role IAM diretamente, sem `AWS_ACCESS_KEY_ID`/secret versionado.

- A role do pipeline (`fluxo-caixa-tf-pipeline`) é criada **fora do Terraform**
  (bootstrap), porque o Terraform não pode gerenciar a própria role que assume
  (chicken-and-egg).
- O CI de hoje (`../../.github/workflows/infra.yml`, raiz do repo) roda **só
  validação estática** (`terraform fmt/validate` + tfsec/checkov) — **sem `apply`,
  sem credencial** (ADR-0012 dec4). O step OIDC abaixo entra quando houver conta real.

**Trust policy** (escopada ao repo):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<conta>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike":    { "token.actions.githubusercontent.com:sub": "repo:<org>/<repo>:*" }
    }
  }]
}
```

**Permissions policy** (least-privilege — esboço):

- `ecs:*`, `rds:*` + `rds-db:connect` (IAM auth), `kafka-cluster:*` (MSK IAM),
  `ec2:*` (VPC/subnet/SG), `kms:*` (chave de repouso), `iam:PassRole` (task roles);
- `s3:GetObject`/`PutObject` **somente** no bucket de state.

**Step OIDC do workflow** (quando houver `plan`/`apply`):

```yaml
permissions:
  id-token: write
  contents: read
steps:
  - uses: aws-actions/configure-aws-credentials@v4
    with:
      role-to-assume: arn:aws:iam::<conta>:role/fluxo-caixa-tf-pipeline
      aws-region: us-east-1
```
