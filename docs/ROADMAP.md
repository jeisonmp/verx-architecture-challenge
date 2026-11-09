# ROADMAP — Fluxo de Caixa (CQRS)

> Documento vivo de planejamento da arquitetura de **Fluxo de Caixa (CQRS)**.
> Fonte macro de planejamento. Decisões consolidadas viram ADRs em `docs/adrs/`.
> Modelo C4 (Contexto + Container) mantido no Structurizr (`workspace.dsl`).

## Estado atual (2026-11-09)

- ✅ Structurizr de pé e validado (`docker-compose.yml` → `structurizr/structurizr local`, porta 8089).
- ✅ ADRs 0001..0010 registrados — **todos accepted**.
- ✅ Norma de branches/versionamento (`GITFLOW.md`) + base de release (`release.yml`, semantic-release — estágio §3 da ADR-0005).
- ✅ `README.md` populado (execução local + validação manual).
- ✅ `workspace.dsl` — borda (API Gateway + WAF), DLQ e ator `cliente` (camada UI/Cliente) adicionados às visões L1/L2.
- ⏳ Docs em esqueleto (`BUSINESS.md`, `GLOSSARY.md`, `ARCHITECTURE.md`, `TECH_DEBT.md`).
- ⏳ ADRs em aberto: observabilidade (0011), IaC/FinOps (0012).

## O que fazer PRIMEIRO (ordem de ataque)

O C4 (`workspace.dsl`) **desenha decisões que ainda não foram tomadas**. Ordem correta: **decidir (ADR) → desenhar (C4) → narrar (docs)**.

1. **Fechar os ADRs em aberto** — observabilidade (0011) e IaC/FinOps (0012), além da regra anti-monólito distribuído.
2. **Popular as docs obrigatórias** — `BUSINESS.md` (domínios/capacidades), `GLOSSARY.md`, `ARCHITECTURE.md` (arquitetura-alvo).

> Sprints 1+ (código, testes, IaC, FinOps) começam depois de congelado o Sprint 0.

## Premissas (Sprint 0 — propostas, congeladas via ADR)

| Dimensão | Decisão proposta | Status |
|---|---|---|
| Cloud | AWS | premissa |
| IaC | Terraform | a decidir (ADR-0012) |
| Linguagem | Java 21 (LTS) + Spring Boot 3 · SQL leve (JDBI + Spring Data JDBC — combo) | ✅ ADR-0006 + ADR-0008 (accepted) |
| Arquitetura | CQRS + Transactional Outbox / Inbox | premissa |
| Mensageria | Kafka (MSK Serverless) | ✅ ADR-0003 (accepted) |
| Banco de dados | PostgreSQL (escrita + projeção de leitura) | ✅ ADR-0004 (accepted) |
| Orquestração | ECS Fargate (sem EKS pré-existente) | ✅ ADR-0010 (accepted) |
| Segurança | API Gateway + WAF + Cognito + Secrets Manager/IAM (passwordless) + refresh tokens | ✅ ADR-0007 (accepted) |
| Observabilidade | Structured logging (JSON) + métricas + tracing | a decidir (ADR-0011) |
| FinOps | AWS Pricing Calculator (estimativa mensal) | a decidir (ADR-0012) |
| Documentação | C4 (Structurizr) + ADRs (MADR) | ✅ em curso |

## Sprint 0 — Definições de Arquitetura (em andamento)

Objetivo: congelar stack, premissas e decisões **antes** de escrever código de produto.

**Decisões registradas**

- [x] ADR-0001 — processo de ADRs (MADR) — **accepted**
- [x] ADR-0002 — Structurizr como gestor de C4/ADRs (`structurizr/structurizr` em modo `local`) — **accepted**
- [x] ADR-0003 — Kafka/MSK — **accepted**
- [x] ADR-0004 — PostgreSQL — **accepted**
- [x] ADR-0005 — CI/CD GitHub Actions (dev → master) — **accepted**
- [x] ADR-0006 — Runtime/linguagem + acesso a dados (Java 21 + Spring Boot 3 + SQL leve) — **accepted**
- [x] ADR-0007 — Segurança em camadas (API Gateway + WAF + Cognito + Secrets Manager/IAM + refresh tokens) — **accepted**
- [x] ADR-0008 — Vert.x vs Spring Boot (Spring Boot mantido; Vert.x descartado) — **accepted**
- [x] ADR-0009 — Consultar a doc oficial do Structurizr antes de alterar o modelo C4 — **accepted**
- [x] ADR-0010 — Orquestração (ECS Fargate vs EKS) — **accepted**

**Decisões pendentes (pré-requisito do C4/docs)**

- [ ] ADR-0011 — Observabilidade (structured logging JSON + métricas + tracing)
- [ ] ADR-0012 — IaC Terraform + FinOps (estimativa AWS Pricing Calculator)
- [ ] Regra anti-monólito distribuído (sem libs compartilhadas A↔B)

**Documentação C4 + docs (após as decisões)**

- [x] `workspace.dsl` — borda (API Gateway + WAF), DLQ e ator `cliente` (camada UI/Cliente); visões L1/L2
- [ ] `BUSINESS.md` — mapeamento de domínios funcionais e capacidades de negócio
- [ ] `GLOSSARY.md` — vocabulário do domínio
- [ ] `ARCHITECTURE.md` — arquitetura-alvo (desenho completo da solução)
- [x] `README.md` — instruções de execução local (requisito obrigatório)

**Dependências (encadeamento de decisões)**

- ADR-0001 (processo) → ADR-0002..0010 (decisões de domínio/técnica).
- ADR-0005 (CI) depende de ADR-0006 (runtime) + `GITFLOW.md` (branches/tags).
- ADR-0007 (segurança) define a borda (API Gateway + WAF) e a gestão de segredos (IAM passwordless) desenhadas no `workspace.dsl`.
- ADR-0010 (orquestração) define ECS Fargate + task role (integração com ADR-0007).
- `release.yml` (estágio §3 — release) depende de ADR-0005 + `GITFLOW.md`; §1/§2 (SAST/SCA, testes+cobertura) ficam para o Sprint 1.
- `workspace.dsl` desenha as decisões de ADR-0002/0003/0004/0006/0007/0010.

## Sprint 1 — Fundação e Ambiente

- Monorepo (estrutura dos microsserviços A e B)
- CI no GitHub Actions (implementação dos estágios §1/§2 da ADR-0005 — SAST/SCA, testes+cobertura; base de release já em `release.yml`)
- docker-compose completo (Kafka/MSK, PostgreSQL, apps)
- Health checks + structured logging (JSON)
- IaC baseline (VPC, rede, recursos base)

## Sprint 2 — Microsserviço A · Lançamentos (escrita)

- Endpoint `POST /lancamentos`
- Schema do banco + **Transactional Outbox** (grava lançamento + evento na mesma transação)
- Outbox worker (publica no Kafka)
- Testes unitários + integração

## Sprint 3 — Microsserviço B · Consolidado (leitura)

- Consumer Kafka + **Inbox** (idempotência — evita dupla contagem)
- Projeção de saldo otimizada para leitura
- Endpoint `GET /consolidado` (50 req/s)
- Testes + DLQ (cenário de falha)

## Sprint 4 — Segurança

- Segredos via Secrets Manager/Vault (sem credencial hardcoded)
- API Gateway na borda + rate limiting + WAF
- Autenticação/autorização (refresh tokens)

## Sprint 5 — Resiliência e Observabilidade

- Retries com backoff, circuit breaker, DLQ
- Structured logging + métricas + tracing
- Simulação de queda do consolidado (controle continua de pé)

## Sprint 6 — Testes de Carga e Validação

- K6/JMeter provando 50 req/s no consolidado
- Cenários de falha (DLQ, autorização, idempotência)
- Ajustes de performance

## Sprint 7 — IaC e FinOps finais

- Terraform completo (`main.tf`)
- Estimativa consolidada AWS Pricing Calculator (link)
- Documentação final C4 + ADRs completos

## Sprint 8 — Entrega e Defesa Técnica

- README de entrega
- Trade-offs documentados (consistência eventual, FinOps)
- Preparação para a defesa técnica da arquitetura
