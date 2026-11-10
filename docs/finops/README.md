# FinOps — Estimativa de custos AWS

> Fonte: **ADR-0012**. Estimativa mensal da infraestrutura (alvo `dev`).

## Link oficial (AWS Pricing Calculator)

> ⚠️ **PENDENTE** — gerar na ferramenta oficial e colar o link aqui:
>
> `https://calculator.aws/#/estimate?id=<ESTIMATE_ID>`
>
> O CSV exportado da ferramenta oficial está versionado como `estimativa-aws-oficial.csv`.

## Como gerar

1. Abrir o [AWS Pricing Calculator](https://calculator.aws/#/createCalculator).
2. Adicionar os serviços do escopo (RDS PostgreSQL, MSK Serverless, ECS Fargate, CloudWatch/X-Ray, NAT Instance).
3. **Export estimate** → CSV → salvar como `docs/finops/estimativa-aws-oficial.csv` (substituir o export anterior).
4. Copiar o link **Share** e colar na seção acima.

## Estimativa preliminar (base manual)

Ver `estimativa-aws-dev.csv`. **Alvo dev: ~US$ 30/mês (estimativa consolidada on-demand: ~US$ 28,71/mês).**

Nota: o maior driver de custo eram as **duas instâncias RDS** (~US$ 11,68/mês cada em
`db.t4g.micro`). Resolvido em duas etapas: **ADR-0013** (instância única com schemas
`lancamentos` + `consolidado`) e **ADR-0014** (RDS 10h/dia via stop/start + MSK Serverless
pay-per-use). Estimativa dev consolidada (on-demand): **~US$ 28,71/mês**.

Nota (MSK Serverless — retenção): o AWS Pricing Calculator tem um **degrau** na retenção —
até ~12h o custo fica ~US$ 0; a partir de ~14h salta para ~US$ 680/mês. É artefato do
modelo (o storage real é US$ 0,10/GB-mês, então 12h vs 24h a essa vazão difere em centavos),
não o custo real de billing. Estimativa usa **retenção 12h** em dev. Lembrete: retenção =
janela de replay/auditoria (ADR-0003) — 12h basta em dev; reavaliar em `prd`.

Nota (NAT Instance — on-demand 10h/dia): decidido **on-demand** (não spot).
10h/dia = 300h/mês < 750h do free tier → **US$ 0 nos 12 primeiros meses**; após,
≈ **US$ 3,16/mês**. Spot foi descartado: não consome free tier e o reclaim (aviso de
~2min) derrubaria o egress das sub-redes privadas. A janela de 10h acompanha o RDS
(`rds_scheduler.tf`, ADR-0014) e cobre o Fargate (8h/dia).
