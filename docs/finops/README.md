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

Ver `estimativa-aws.csv`. **Alvo dev: ~US$ 30/mês (estimativa consolidada: ~US$ 25,55/mês).**

Nota: o maior driver de custo eram as **duas instâncias RDS** (~US$ 11,68/mês cada em
`db.t4g.micro`). Resolvido em duas etapas: **ADR-0013** (instância única com schemas
`lancamentos` + `consolidado`) e **ADR-0014** (RDS 10h/dia via stop/start + MSK Serverless
pay-per-use). Estimativa dev consolidada: **~US$ 25,55/mês**.

Nota (MSK Serverless — retenção): o AWS Pricing Calculator tem um **degrau** na retenção —
até ~12h o custo fica ~US$ 0; a partir de ~14h salta para ~US$ 680/mês. É artefato do
modelo (o storage real é US$ 0,10/GB-mês, então 12h vs 24h a essa vazão difere em centavos),
não o custo real de billing. Estimativa usa **retenção 12h** em dev. Lembrete: retenção =
janela de replay/auditoria (ADR-0003) — 12h basta em dev; reavaliar em `prd`.

Nota (NAT Instance — free tier): a linha EC2 (`t3.micro`) usa free tier
(750h/mês por 12 meses). Após o período, 24/7 ≈ **US$ 7,50/mês**, elevando o total
dev de ~US$ 25,55 para ~US$ 33/mês (acima do alvo de ~US$ 30). Para manter o alvo
pós-free-tier, aplicar stop/start na NAT Instance fora da janela do Fargate (8h/dia)
— mesmo padrão do RDS 10h/dia (`rds_scheduler.tf`, ADR-0014).
