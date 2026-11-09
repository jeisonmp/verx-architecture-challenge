---
status: accepted
date: 2026-11-09
decision-makers: Max
---
# 0012. IaC Terraform + FinOps — organização repos/, infra documentada (não implementada)

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

**R:** Correto em parte. O **S3 (com versioning)** é o obrigatório — guarda o `terraform.tfstate` remoto e versionado. O **DynamoDB** é só a tabela de **lock** (impede dois `terraform apply` concorrentes corromperem o state). Para **desafio solo** (um operador, sem CI concorrente), **S3 basta** — o DynamoDB lock fica **opcional** e documentado para quando houver equipe/CI.

### Q2 — Implementar ou documentar a infra?

**R:** **Documentar, não implementar.** Sem conta AWS real, o deliverable é o desenho (C4 deployment), o README de infra e a estimativa — **não** `terraform apply`. A estrutura de pastas e o conteúdo são descritos em `repos/infra/README.md`.

### Q3 — Organização dos projetos?

**R:** Monorepo em **`repos/`** com `servico-a/` (Lançamentos), `servico-b/` (Consolidado) e `infra/` (Terraform) — e o que mais precisarmos.

### Q4 — WAF em quais ambientes?

**R:** **WAF apenas em produção.** `dev`/`qas` ficam isolados via **VPC**, acessados somente por **notebook via VPN**.

### Q5 — Egress em dev?

**R:** **NAT Instance** (`t3.micro`, free tier) — sem NAT Gateway (~US$ 32/mês).

### Q6 — Alvo de custo em dev?

**R:** **~US$ 30/mês** (parâmetro do motor §6.2).

## Critérios de decisão

| Critério | Terraform | CDK | CloudFormation | Ansible |
|---|---|---|---|---|
| Portabilidade (multi-cloud) | sim | sim | não (AWS-only) | sim |
| Declarativo | HCL | código (imperativo) | YAML/JSON | YAML |
| Validação no CI (ADR-0005) | `terraform validate` + tfsec/checkov | — | — | — |
| Ecossistema/adoção | ampla | ampla | AWS-only | config mgmt |

## Decisão

1. **IaC = Terraform (HCL) portável** — não CDK/CloudFormation/Ansible.
2. **State remoto = S3 (versioning)**; **DynamoDB lock opcional** (omitido no desafio solo).
3. **Organização = `repos/`**: `servico-a/`, `servico-b/`, `infra/` — monorepo da challenge.
4. **Infra documentada, não implementada** — estrutura de pastas + conteúdo descritos no `repos/infra/README.md`.
5. **Isolamento por ambiente**: `dev`/`qas`/`prd` (workspaces/diretórios), com `dev` minimizado.
6. **Escopo do Terraform** → ROADMAP (com o que cada serviço faz).
7. **FinOps** → `repos/infra/README.md` (regras + estimativa).
8. **WAF apenas em produção**; `dev`/`qas` isolados via VPC + VPN (notebook).
9. **Egress em dev = NAT Instance** (free tier).
10. **Alvo dev ~US$ 30/mês.**

## Consequências

* Bom, porque Terraform é portável e já validado no CI (ADR-0005 §3: `terraform validate` + tfsec/checkov).
* Bom, porque documentar (não implementar) evita provisionar infra que não será executada (YAGNI).
* Bom, porque WAF só em prd + NAT Instance mantêm dev barato (~US$ 30/mês).
* Ruim, porque sem `terraform apply` real o IaC fica validado só estaticamente (sem runtime).
* Ruim, porque S3 sem DynamoDB não tem lock — aceitável solo, mas reabrir se virar equipe/CI.
