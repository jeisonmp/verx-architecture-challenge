# FinOps — Estimativa de custos AWS

> Fonte: **ADR-0012**. Estimativa mensal da infraestrutura (alvo `dev`).

## Link oficial (AWS Pricing Calculator)

> [Abrir estimativa no AWS Pricing Calculator](https://calculator.aws/#/estimate?id=9ec6a0d577dd2e6024b889dd076c5f5a88016018)
>
> O CSV exportado da ferramenta oficial está versionado como `estimativa-aws-oficial.csv`.

## Como gerar

1. Abrir o [AWS Pricing Calculator](https://calculator.aws/#/createCalculator).
2. Adicionar os serviços do escopo (RDS PostgreSQL, MSK Serverless, ECS Fargate, CloudWatch/X-Ray, NAT Instance).
3. **Export estimate** → CSV → salvar como `docs/finops/estimativa-aws-oficial.csv` (substituir o export anterior).
4. Copiar o link **Share** e colar na seção acima.

## Estimativa dev

Ver `estimativa-aws-dev.csv` (corrigida) e `estimativa-aws-oficial.csv` (export bruto do calculator).

**Histórico do alvo:** partimos de um alvo de ~US$ 30/mês. A estimativa inicial
(~US$ 25,55 → ~US$ 28,71) já embutia o RDS único (ADR-0013) e o stop/start (ADR-0014),
mas **subdimensionava o Fargate** (0,25 vCPU / 0,5 GB). Validando o sizing contra a
necessidade real (Java 21 + Spring Boot 3 a 50 req/s), o Fargate subiu para
**1 vCPU / 2 GB** — estimativa corrigida: **~US$ 46,33/mês**.

Nota (RDS — histórico): o maior driver de custo eram as **duas instâncias RDS**
(~US$ 11,68/mês cada em `db.t4g.micro`). Resolvido em duas etapas: **ADR-0013**
(instância única com schemas `lancamentos` + `consolidado`) e **ADR-0014** (RDS
10h/dia via stop/start + MSK Serverless pay-per-use).

Nota (Fargate — sizing): Spring Boot 3 não roda em 0,5 GB (heap ~128 MB é insuficiente
para JVM + metaspace). 1 vCPU / 2 GB dá heap ~512 MB, confortável para 50 req/s no load
test. O Fargate (2 tasks, 8h/dia) saltou de ~US$ 6 para **US$ 24,03/mês**.

Nota (carga — capacidade × tráfego real): os **50 req/s** são a *capacidade* provada no
load test (ADR-0005), não o tráfego real de dev. X-Ray (1M req/mês) e CloudWatch
(5 GB/mês) refletem o **tráfego real de dev** (~1M requests/mês de testes manuais) —
por isso não há 43M traces/mês (que seria 50 req/s × 8h/dia × 30 dias).

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
