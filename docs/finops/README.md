# FinOps — Estimativa de custos AWS

> Fonte: **ADR-0012**. Estimativa mensal da infraestrutura (alvo `dev`).

## Link oficial (AWS Pricing Calculator)

> ⚠️ **PENDENTE** — gerar na ferramenta oficial e colar o link aqui:
>
> `https://calculator.aws/#/estimate?id=<ESTIMATE_ID>`
>
> O CSV exportado da ferramenta oficial deve substituir `estimativa-aws.csv` (que é uma estimativa preliminar).

## Como gerar

1. Abrir o [AWS Pricing Calculator](https://calculator.aws/#/createCalculator).
2. Adicionar os serviços do escopo (RDS PostgreSQL, MSK Serverless, ECS Fargate, CloudWatch/X-Ray, NAT Instance).
3. **Export estimate** → CSV → salvar como `docs/finops/estimativa-aws.csv` (substituir o preliminar).
4. Copiar o link **Share** e colar na seção acima.

## Estimativa preliminar (base manual)

Ver `estimativa-aws.csv`. **Alvo dev: ~US$ 30/mês.**

Nota: o maior driver de custo são as **duas instâncias RDS** (~US$ 11,68/mês cada em
`db.t4g.micro`). Mitigação FinOps para fechar em ~US$ 30/mês: usar **Aurora Serverless v2**
(min. 0,5 ACU) ou uma única instância compartilhada em `dev`; a decisão fica para a
Sprint 7 (estimativa consolidada).
