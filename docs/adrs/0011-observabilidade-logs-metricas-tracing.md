---
status: accepted
date: 2026-11-09
decision-makers: Max
---
# 0011. Observabilidade — logs JSON, métricas CloudWatch e tracing X-Ray (ADOT)

## Contexto e problema

Os dois microsserviços (Lançamentos e Consolidado) precisam de observabilidade ponta a ponta — logs, métricas e tracing — **atravessando o barramento Kafka** (A → B). O motor (`AGENTS.md` §2) manda *structured logging* em JSON com correlation IDs, e §2.1 (LGPD) proíbe expor tokens/`x-api-key`/body em logs de produção. Precisamos definir o stack sem adicionar infra pesada (FinOps) e coerente com a stack AWS já decidida (ADR-0007, ADR-0010).

## Alternativas consideradas

* **AWS-native** — `logstash-logback-encoder` → CloudWatch Logs; Micrometer → CloudWatch; **ADOT** (OTel SDK) → X-Ray (escolhida).
* **Datadog SaaS** — `dd-trace` → Datadog Agent (com `ddapm-test-agent` para local).
* **OpenTelemetry puro** — Collector próprio + backend OSS (Jaeger/Zipkin + Prometheus/Grafana).
* **X-Ray SDK nativo** — instrumentação direta no SDK AWS, sem OTel.

## Histórico da discussão (sessão de iteração — Q&A)

### Q1 — "ADOT como sidecar me permite criar um ambiente local com o `ddapm-test-agent` (Datadog)?"

**R:** Não. **ADOT** é o OTel Collector **enxuto da AWS** — empacota só os exporters AWS (`xray`, `cloudwatch`, `amp`, `otlp`), **sem exporter `datadog`**. O **`ddapm-test-agent`** é um *test double* do Datadog Agent que fala o **protocolo APM Datadog** (porta 8126), não OTLP. Protocolos e fornecedores distintos — um não alimenta o outro. O que o padrão OTel dá é **instrumentação portável** (W3C Trace Context, backend trocável por config de exporter): Datadog local entraria via OTel Collector *contrib* com exporter `datadog`, ou via `dd-trace`, **não** via ADOT.

### Q2 — Tracing: ADOT (X-Ray) vs OpenTelemetry puro vs X-Ray SDK nativo?

**R:** **ADOT** — mantém o padrão OTel (W3C, propaga via Kafka) com backend X-Ray, **sem infra extra de Collector**. O X-Ray SDK nativo amarraria o código ao SDK AWS; o OTel puro exigiria Collector + backend próprios (custo e sustentação). ADOT é o meio-termo nativo-AWS.

### Q3 — Métricas: CloudWatch vs Prometheus/Grafana?

**R:** **CloudWatch** — zero infra extra (sem Prometheus server/Grafana), coerente com FinOps (2 serviços, 50 req/s). Prometheus/AMP só se o cenário crescer ou virar multi-serviço.

### Q4 — Como correlacionar o trace através do Kafka?

**R:** O `correlationId`/trace via **header do evento** (não do payload) — o consumer restaura o contexto do header, então o trace **não quebra** no `lancamento.criado` → projeção.

### Q5 — LGPD/redação nos logs?

**R:** Sim — `Authorization`/`x-api-key`/tokens nunca em log; sem body-trace em `prd` (§2.1 do `AGENTS.md`).

## Critérios de decisão

| Critério | AWS-native (ADOT+CloudWatch) | Datadog | OTel puro |
|---|---|---|---|
| Infra extra | nenhuma | Datadog Agent | Collector + backend |
| Custo | pay-per-use (trace/GB) | APM por host/mês | OSS (custo de sustentação) |
| Coerência AWS (ADR-0007/0010) | sim | desvia | parcial |
| Propagação Kafka (W3C) | sim | sim | sim |

## Decisão

Adotar **observabilidade AWS-native**, sem infra própria:

1. **Logs — JSON estruturado** via `logstash-logback-encoder` (Logback, padrão Spring Boot) → **CloudWatch Logs** (driver `awslogs` do ECS). MDC com `correlationId`, `traceId`/`spanId`, `severity`, `timestamp`. **Redação LGPD:** `Authorization`, `x-api-key`, `Cookie` e tokens mascarados antes de escrever; sem *body-trace* em `prd`.
2. **Métricas — Micrometer** (Spring Boot Actuator) → **CloudWatch** (`micrometer-registry-cloudwatch`/EMF). Latência/erros/taxa HTTP, consumer lag do Kafka, contadores de outbox/inbox, JVM.
3. **Tracing — ADOT** (OpenTelemetry SDK, W3C Trace Context) → **AWS X-Ray**, com auto-instrumentação no ECS. Propagação por headers **HTTP** e **headers do evento Kafka** (`lancamento.criado`), ligando A → B num único trace.

Justificativa: correlation ID atravessa o barramento (trace fim-a-fim A→B) sem infra própria — CloudWatch e X-Ray são gerenciados e pay-per-use (FinOps). A instrumentação via OTel SDK mantém o código **agnóstico e padronizado** (W3C Trace Context): o lock-in fica no backend gerenciado da AWS, não no código — trocar de backend é questão de config de exporter, sem tocar no negócio.

Evitar o **OpenTelemetry puro** (Collector + Prometheus + Jaeger próprios) elimina três custos reais: (1) **infraestrutura de logs/rastreabilidade para sustentar**; (2) **equipe dedicada** para operar e escalar esses servidores; e (3) o risco de **fate sharing** — num pico, os servidores de logs tenderiam a cair junto com os serviços, deixando a operação no escuro. Com o sidecar da AWS, o armazenamento, a escalabilidade e a visualização ficam **100% delegados à AWS** (X-Ray para rastreabilidade, CloudWatch para logs), e o código permanece livre e padronizado — sem dor de cabeça operacional.

## Refinamento — ambiente local (sem AWS)

- **Tracing:** OTLP → **Jaeger** (container no `docker-compose.yml`) para visualização de trace.
- **Logs:** console em JSON (mesmo encoder); **Métricas:** endpoint `/actuator/prometheus`.
- **Sampling:** 100% em `dev`; reduzido em `prd` (controle de custo de X-Ray).

## Consequências

* Bom, porque o correlation ID/trace atravessa o Kafka (fim-a-fim A→B), essencial para depurar o fluxo assíncrono Outbox/Inbox.
* Bom, porque zero infra extra — CloudWatch/X-Ray gerenciados, pay-per-use (FinOps).
* Bom, porque a instrumentação OTel é portável (backend trocável sem tocar no código).
* Bom, porque a redação LGPD cumpre §2.1 do motor.
* Ruim, porque CloudWatch/X-Ray têm UX menos rica que Datadog/Grafana.
* Ruim, porque há lock-in no backend AWS (mitigado pelo SDK OTel portável).
* Ruim, porque a propagação via header Kafka exige disciplina manual no consumer (restaurar o contexto do header).
