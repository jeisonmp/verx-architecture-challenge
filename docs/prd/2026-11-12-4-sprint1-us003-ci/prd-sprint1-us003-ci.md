---
project: raiz
---

# PRD: Sprint 1 — US-003 (CI de Produto: SAST/SCA + Testes/Cobertura)

**Type:** feature
**Projeto afetado:** raiz — cross-repo (`.github/workflows/` na raiz; `pom.xml` de `repos/lancamentos` e `repos/consolidado`).

## 1. Introduction/Overview

O Sprint 1 entregou o monorepo (US-001), o contrato CDC (US-002), o docker-compose (US-004) e health/logging JSON (US-005), mas a **US-003 — CI de produto (estágios §1/§2 da ADR-0005)** ficou *skipped* por bloqueio de escrita em `.github/` (fix `chown` já aplicado). Esta PRD cobre esse gap: erguer os workflows de CI de produto (SAST/SCA + testes/cobertura) que faltam para fechar o Sprint 1.

## 2. Goals

- Gate de segurança em PRs `feat/* → dev`: SAST + SCA + gitleaks (ADR-0005 §1).
- Gate de qualidade em `dev → master`: testes unitários + integração (PostgreSQL + Kafka) com cobertura ≥ 80% (ADR-0005 §2).
- Zero credencial estática nos workflows (OIDC — ADR-0005/0007).
- Header de copyright em todos os arquivos criados.

## User Stories

### US-001: Workflow de segurança — SAST + SCA + gitleaks (feat/* → dev)

**required_intelligence:** "pro"

**type:** feature

**Description:** Como a persona raiz-dev, quero um workflow de CI que rode análise estática (SAST), varredura de dependências (SCA) e detecção de segredos (gitleaks) em PRs `feat/* → dev`, para que nenhum código de produto entre em `dev` com risco crítico conhecido.

**Acceptance Criteria:**

- [x] `.github/workflows/analysis.yml` com job SAST (SonarQube/análise estática) rodando em PRs `feat/* → dev`.
- [x] `.github/workflows/dependabot.yml` com SCA (varredura/atualização de dependências) configurado.
- [x] gitleaks ativo no fluxo de PR (passo no `analysis.yml` ou workflow próprio).
- [x] Sem credencial estática no workflow (OIDC via `permissions:` + `id-token` — ADR-0005/0007).
- [x] Header de copyright nos arquivos de workflow.
- [x] Workflows sintaticamente válidos (YAML parse OK).

### US-002: Workflow de testes + cobertura (dev → master)

**required_intelligence:** "pro"

**type:** feature

**Description:** Como a persona raiz-dev, quero um workflow de testes unitários + integração (containers PostgreSQL + Kafka) com gate de cobertura, para que `dev → master` só aceite código verde e coberto.

**Acceptance Criteria:**

- [x] `.github/workflows/quality.yml` com testes unitários + integração (service containers PostgreSQL + Kafka) rodando em `dev → master`.
- [x] JaCoCo configurado nos `pom.xml` de `repos/lancamentos` (A) e `repos/consolidado` (B).
- [x] Gate de cobertura ≥ 80% (meta 90% no núcleo outbox/inbox) — ADR-0005/README.
- [x] Sem credencial estática no workflow (OIDC — ADR-0005/0007).
- [x] Header de copyright nos arquivos de workflow e nos `pom.xml` alterados.
- [x] `./mvnw -q test` passa localmente em A e B (build verde).

## 4. Functional Requirements

- FR-1: workflow SAST + SCA + gitleaks rodando em PRs `feat/* → dev` (ADR-0005 §1).
- FR-2: workflow de testes unitários + integração (PostgreSQL + Kafka) rodando em `dev → master` (ADR-0005 §2).
- FR-3: gate de cobertura JaCoCo ≥ 80% (90% núcleo outbox/inbox).
- FR-4: autenticação OIDC, sem credencial estática (ADR-0005/0007).

## 5. Non-Goals (Out of Scope)

- Estágio §3 (release) — já coberto por `release.yml` (Sprint 0).
- CI de infra (`infra.yml`) — já existente; esta PRD é só do código de produto.
- Regra de negócio de `POST /lancamentos` / `GET /consolidado` (Sprint 2/3).
- SonarQube auto-hospedado (usar análise estática disponível no GitHub Actions).

## 6. Design Considerations

- OIDC (sem secrets estáticos): `permissions:` mínimos + `id-token: write` para autenticar provedores (ADR-0005/0007).
- `gitleaks` como passo do `analysis.yml` (mais simples que um workflow separado).

## 7. Technical Considerations

- **Tier:** `pro` — design de CI (OIDC, gates de cobertura) é decisão de arquitetura.
- **Copyright:** header padrão da holding (skill `backforend-copyright`) em todo arquivo criado.
- **YAGNI:** não criar jobs para ferramentas que ainda não existem no stack congelado.
- **LGPD:** nada de dados pessoais nos logs do CI.

## 8. Success Metrics

- PR `feat/* → dev` é barrado por risco crítico (SAST/SCA/gitleaks).
- `dev → master` roda testes com containers e falha se cobertura < 80%.
- Nenhum secret estático nos workflows (auditoria OIDC).

## 9. Open Questions

- SAST: SonarQube Cloud vs CodeQL (default do GitHub) — decidir no US-001.
- Cobertura 90% no núcleo outbox/inbox: o núcleo ainda não existe (Sprint 2/3) — aplicar a meta global 80% agora.
