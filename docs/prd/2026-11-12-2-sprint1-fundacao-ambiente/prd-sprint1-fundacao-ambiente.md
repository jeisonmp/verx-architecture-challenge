---
project: raiz
---

# PRD: Sprint 1 — Fundação e Ambiente (Monorepo, CDC, CI e Execução Local)

**Type:** feature
**Projeto afetado:** raiz — cross-repo (`repos/lancamentos`, `repos/consolidado`, CI raiz, docker-compose).

## 1. Introduction/Overview

O Sprint 0 congelou a arquitetura (ADR-0001..0017), o C4 (`workspace.dsl`) e as docs
obrigatórias. A IaC baseline e o FinOps já foram **adiantados** em `repos/infra/`
(ADR-0012/0014/0016). Esta PRD cobre o que falta do **Sprint 1 — Fundação e
Ambiente**: erguer o monorepo dos microsserviços A (Lançamentos) e B (Consolidado),
o contrato CDC do evento `lancamento.criado` (ADR-0015), os estágios §1/§2 do CI
(ADR-0005) e a execução local completa (docker-compose, health checks, logging JSON).

## 2. Goals

- Dois módulos Spring Boot 3 (A e B) compiláveis, com as dependências mínimas do stack congelado (ADR-0006/0008).
- Contrato A↔B executável e versionado (`schemaVersion`) via Spring Cloud Contract (ADR-0015).
- CI de produto com SAST/SCA e testes+cobertura (estágios §1/§2 da ADR-0005).
- Execução local de ponta a ponta via docker-compose, com health checks e structured logging JSON (ADR-0011).

## User Stories

### US-001: Scaffold do monorepo (Lançamentos A e Consolidado B)

**required_intelligence:** "pro"

**type:** feature

**Description:** Como a persona raiz-dev, quero criar a estrutura mínima dos microsserviços A (`repos/lancamentos`) e B (`repos/consolidado`) em Spring Boot 3 com JDBI + Spring Data JDBC, PostgreSQL e Kafka, para que o domínio possa evoluir sobre uma fundação compilável e testável.

**Acceptance Criteria:**

- [x] `repos/lancamentos` e `repos/consolidado` compilam (`./mvnw -q compile`) com Java 21.
- [x] `pom.xml` declara apenas dependências do stack congelado (ADR-0006/0008: Spring Boot 3, JDBI + Spring Data JDBC, PostgreSQL, Kafka).
- [x] Sem ORM (Hibernate/JPA) no núcleo transacional (anti-pattern da persona).
- [x] Estrutura de pacotes com fronteira clara entre domínio/application/infra (SOLID).
- [x] Header de copyright em todo arquivo criado.
- [x] Nenhuma lib compartilhada entre A e B (ADR-0015).

### US-002: Contrato CDC do evento `lancamento.criado`

**required_intelligence:** "pro"

**type:** feature

**Description:** Como a persona raiz-dev, quero definir o contrato do evento `lancamento.criado` como teste executável via Spring Cloud Contract, com `schemaVersion` obrigatório (ADR-0015), para que produtor (A) e consumidor (B) evoluam sem quebrar a integração silenciosamente.

**Acceptance Criteria:**

- [x] Contrato do evento `lancamento.criado` definido em Spring Cloud Contract (producer side em `repos/lancamentos`).
- [x] `schemaVersion` presente e com asserção obrigatória no contrato (ADR-0015).
- [x] Teste CDC gera/valida o stub do consumidor.
- [x] Consumidor (B) consome o stub em teste de integração (sem depender de A em execução).
- [x] Header de copyright nos arquivos de contrato/teste.

### US-003: CI estágios §1/§2 (SAST/SCA + testes+cobertura)

**required_intelligence:** "pro"

**type:** feature

**Description:** Como a persona raiz-dev, quero implementar os estágios §1 (SAST/SCA) e §2 (testes+cobertura) da ADR-0005 no GitHub Actions para o código de produto, para que todo PR `feat/* → dev` seja barrado por riscos críticos e por falha de testes/cobertura.

**Acceptance Criteria:**

- [x] Workflow de SAST (SonarQube/análise estática) + SCA (OWASP Dependency-Check/Dependabot) rodando em PRs `feat/* → dev`.
- [x] Workflow de testes unitários + integração (containers PostgreSQL + Kafka) rodando em `dev → master`.
- [x] Gate de cobertura ≥ 80% (meta 90% no núcleo outbox/inbox) — ADR-0005/README.
- [x] gitleaks ativo no fluxo de PR.
- [x] Sem credencial estática no workflow (OIDC — ADR-0005/0007).
- [x] Header de copyright nos arquivos de workflow.

> **Nota (US-003):** marcada como concluída no estado da run com a ressalva de
> **bloqueio de ambiente** — `/workspace/.github` era `node:node 755` (sem escrita
> para o usuário `ralph`). O fix humano (`chown -R node:ralph /workspace/.github`)
> e a reabertura da story estão registrados em `notes` do `prd.json`/log da run.

### US-004: docker-compose completo (PostgreSQL + Kafka + apps A/B)

**required_intelligence:** "flash"

**type:** feature

**Description:** Como a persona raiz-dev, quero um `docker-compose.yml` que suba PostgreSQL, Kafka (broker único local) e os apps A/B, para que a solução rode localmente de ponta a ponta sem cloud.

**Acceptance Criteria:**

- [x] `docker compose up` sobe PostgreSQL, Kafka e os apps A/B.
- [x] Apps A/B com health check configurado (Actuator).
- [x] Variáveis de ambiente injetadas via compose (sem credencial hardcoded).
- [x] Header de copyright no arquivo.

### US-005: Health checks + structured logging (JSON)

**required_intelligence:** "flash"

**type:** feature

**Description:** Como a persona raiz-dev, quero health checks (Spring Boot Actuator) e structured logging JSON nos serviços A e B, para que a operação local e a observabilidade sigam a ADR-0011.

**Acceptance Criteria:**

- [x] `GET /actuator/health` retorna UP em A e B.
- [x] Logs em JSON estruturado (JsonFormatter/logback JSON) — ADR-0011.
- [x] Redação de dados sensíveis nos logs (headers `Authorization`, tokens) — LGPD §2.1.
- [x] Sem `print()`/`System.out` em código de produto.

## 4. Functional Requirements

- FR-1: monorepo com dois módulos compiláveis (A e B), sem libs compartilhadas (ADR-0015).
- FR-2: contrato A↔B versionado (`schemaVersion`) e executável (CDC).
- FR-3: CI de produto com gates de SAST/SCA/testes/cobertura (ADR-0005).
- FR-4: execução local completa via docker-compose com health checks e logging JSON (ADR-0011).

## 5. Non-Goals (Out of Scope)

- Regra de negócio de `POST /lancamentos` e `GET /consolidado` (Sprint 2/3).
- Transactional Outbox / Inbox (Sprint 2/3).
- Segurança de borda (API Gateway/WAF/Cognito) — Sprint 4.
- IaC/FinOps — já adiantados em `repos/infra/` e `docs/finops/` (ADR-0012/0014/0016); apenas reaproveitar a baseline.
- Provisionar/`terraform apply` (proibido — §3, mutação de nuvem é do usuário).

## 6. Design Considerations

- **Sem ORM:** JDBI + Spring Data JDBC no núcleo transacional (ADR-0006/0008).
- **Anti-monólito distribuído:** nenhuma dependência de código A↔B — apenas o contrato CDC (ADR-0015).
- **CDC como única ponte A↔B:** o contrato do evento é a fronteira; `schemaVersion` impede evolução silenciosa.

## 7. Technical Considerations

- **Tier:** stories de fundação (scaffold, CDC, CI) em `pro`; tarefas mecânicas (docker-compose, health/logging) em `flash` (AGENTS.md §0.4).
- **Copyright:** header padrão da holding (skill `backforend-copyright` §1.1) em todo arquivo de código criado.
- **YAGNI:** Macro-YAGNI por story (ANTES) e Micro-YAGNI (DEPOIS) em toda implementação (§3.2); abstrações só com 3+ usos concretos.
- **TDD RED:** stories de código (US-001/002/003/005) seguem TDD RED quando aplicável (ia/ralph/AGENTS.md).
- **LGPD:** structured logging com redação de dados pessoais/sensíveis (§2.1).

## 8. Success Metrics

- 2 módulos Spring Boot compilando (`./mvnw -q compile` exit 0).
- CDC gerando/validando stub do consumidor.
- CI de produto com todos os gates verdes em PR de exemplo.
- `docker compose up` subindo a solução local com health checks UP e logs JSON.

## 9. Open Questions

- Broker Kafka local: imagem `confluentinc/cp-kafka` (KRaft) vs `bitnami/kafka` — decidir no US-004.
- Spring Cloud Contract: versão compatível com Spring Boot 3.x — validar no US-002.
- Nível de cobertura inicial (80% global vs 90% núcleo) — revalidar no US-003.
