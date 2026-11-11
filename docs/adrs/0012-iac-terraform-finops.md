---
status: accepted
date: 2026-11-09
decision-makers: Max
---
# 0012. IaC Terraform + FinOps — organização repos/, infra declarada em .tf (sem apply)

## Contexto e problema

O feedback do desafio pede "arquivos práticos de IaC (Terraform/Ansible) e estimativas financeiras estruturadas de custos de nuvem, indo além de descrições textuais". Mas não há conta AWS real nesta challenge — precisamos decidir **até onde implementar a infra** (implementar vs documentar), a **ferramenta de IaC**, o **state backend** e a **organização dos projetos** (monorepo), sem gastar esforço com provisionamento que não será executado.

## Alternativas consideradas

* **Terraform (HCL)** — escolhida.
* **AWS CDK** (TypeScript/Java) — código imperativo, menos declarativo.
* **CloudFormation** (YAML/JSON) — declarativo, mas AWS-only.
* **Ansible** — gestão de configuração, não modelagem de estado de infra.
* **State:** S3 (versioning) vs S3 + DynamoDB (lock) vs local.

## Histórico da discussão (sessão de iteração — Q&A)

### Q1 — "State remoto: a AWS resolve totalmente com S3, sem precisar provisionar DynamoDB?"

**R:** Sim — e o **Terraform 1.10+** elimina o DynamoDB de vez. O backend S3 ganhou **state locking nativo** via `use_lockfile = true`, que usa *S3 conditional writes* (um arquivo `.tflock` ao lado do estado). O `dynamodb_table` passa a ser **desnecessário** (a partir do Terraform 1.11 foi marcado como *deprecated*). Requer **Terraform ≥ 1.10** e `terraform init -reconfigure` na migração.

### Q2 — Implementar ou documentar a infra?

**R:** **Declarar os arquivos `.tf` reais** (ECS Fargate, MSK Serverless, RDS PostgreSQL) em `repos/infra/`, **sem `terraform apply`** (não há conta AWS real). O código é a evidência concreta de IaC; o provisionamento efetivo fica para um ambiente com conta.

### Q3 — Organização dos projetos?

**R:** Monorepo em **`repos/`** com `servico-a/` (Lançamentos), `servico-b/` (Consolidado) e `infra/` (Terraform) — e o que mais precisarmos.

### Q4 — WAF em quais ambientes?

**R:** **WAF apenas em produção.** `dev`/`qas` ficam isolados via **VPC**, acessados somente por **notebook via VPN**.

### Q5 — Egress em dev?

**R:** **NAT Instance** (`t3.micro`, free tier) — sem NAT Gateway (~US$ 32/mês).

### Q6 — Alvo de custo em dev?

**R:** **~US$ 30/mês** (limite de custo do ambiente dev).

## Critérios de decisão

| Critério | Terraform | CDK | CloudFormation | Ansible |
|---|---|---|---|---|
| Portabilidade (multi-cloud) | sim | sim | não (AWS-only) | sim |
| Declarativo | HCL | código (imperativo) | YAML/JSON | YAML |
| Validação no CI (ADR-0005) | `terraform validate` + Trivy/checkov | — | — | — |
| Ecossistema/adoção | ampla | ampla | AWS-only | config mgmt |

## Decisão

1. **IaC = Terraform (HCL) portável** — não CDK/CloudFormation/Ansible.
2. **State remoto = S3** com **state locking nativo** (`use_lockfile = true`, Terraform 1.10+) — **sem DynamoDB**.
3. **Organização = `repos/`**: `servico-a/`, `servico-b/`, `infra/` — monorepo da challenge.
4. **Infra declarada em `repos/infra/`** — `backend.tf`, `providers.tf`, `variables.tf`, `main.tf` com ECS Fargate, MSK Serverless e RDS PostgreSQL reais (sem `terraform apply`).
5. **Isolamento por ambiente**: `dev`/`qas`/`prd` (workspaces/diretórios), com `dev` minimizado.
6. **Escopo do Terraform** → ROADMAP (com o que cada serviço faz).
7. **FinOps** → `docs/finops/` (CSV de estimativa + link oficial do AWS Pricing Calculator) e `repos/infra/README.md` (regras).
8. **WAF apenas em produção**; `dev`/`qas` isolados via VPC + VPN (notebook).
9. **Egress em dev = NAT Instance** (free tier).
10. **Alvo dev ~US$ 30/mês.**

## Consequências

* Bom, porque Terraform é portável e já validado no CI (ADR-0005 §3: `terraform validate` + Trivy/checkov).
* Bom, porque `use_lockfile = true` usa o locking nativo do S3 (1.10+) e elimina o DynamoDB — um recurso a menos para manter.
* Bom, porque os `.tf` reais (ECS/MSK/RDS) evidenciam IaC concreto, além de texto.
* Bom, porque WAF só em prd + NAT Instance mantêm dev barato (alvo ~US$ 30/mês).
* Ruim, porque sem `terraform apply` real o IaC fica validado só estaticamente (sem runtime).
