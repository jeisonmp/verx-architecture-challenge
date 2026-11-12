---
project: raiz
---

# PRD: Setup Inicial — Documentação Técnica Local dos Repos

**Type:** docs
**Projeto afetado:** raiz — cross-repo (documentação local).

## 1. Introduction/Overview

O onboarding gerou a topologia (`PROJECTS.md`, `PERSONAS.md`) e os **esqueletos**
de documentação local em cada repo (`docs/ARCHITECTURE.md`, `docs/BUSINESS.md`,
`docs/GLOSSARY.md`, `docs/TECH_DEBT.md`). Para evitar estouro de contexto e
timeouts durante a entrevista, o **conteúdo** dessas documentações é gerado aqui,
de forma assíncrona, pelo loop normal do Ralph.

## 2. Goals

- Popular cada `docs/` local com documentação técnica derivada do código real do repo.
- Preservar a fronteira global (raiz `docs/`) × local (`<repo>/docs/`).
- Zero alteração em código de produto.

## User Stories
### US-001: Popular docs/ do repo raiz

**required_intelligence:** "pro"

**type:** docs

**Description:** Como a persona raiz-dev, quero analisar o código existente na raiz e preencher os 4 documentos locais de contexto (docs/ARCHITECTURE.md, docs/BUSINESS.md, docs/GLOSSARY.md, docs/TECH_DEBT.md), para que o domínio do repo fique documentado e navegável pelo agente.

**Acceptance Criteria:**

- [x] docs/ARCHITECTURE.md descreve módulos, camadas e padrões reais observados no código.
- [x] docs/BUSINESS.md descreve as regras de negócio e o domínio do repo.
- [x] docs/GLOSSARY.md lista os termos de domínio com definições.
- [x] docs/TECH_DEBT.md lista débitos técnicos observados no código (ou registra "sem débitos identificados").
- [x] Nenhum arquivo de código de produto alterado.
- [x] Nenhuma referência upward a AGENTS.md/skills nos docs (AGENTS.md §7.1).
- [x] Header de copyright preservado nos docs (exceto `CHANGELOG.md`, que não recebe header).

## 4. Functional Requirements

- FR-1: Cada repo filho recebe os 4 documentos locais preenchidos com conteúdo derivado do código.
- FR-2: Documentação local não referencia AGENTS.md/skills (nível IA) — AGENTS.md §7.1.
- FR-3: Header de copyright preservado em todos os docs (exceto CHANGELOGs).
- FR-4: Nenhum arquivo de código-fonte de produto é alterado.

## 5. Non-Goals (Out of Scope)

- Alterar código de produto.
- Alterar a documentação global da raiz (docs/ do workspace).
- Criar PRDs de feature/negócio.
- Provisionar infraestrutura.

## 6. Design Considerations

Não se aplica — sem interface de usuário.

## 7. Technical Considerations

- **Fronteira de contexto:** a raiz global (docs/) documenta o motor/ecossistema; cada <repo>/docs/ documenta o domínio local daquele repo.
- **Tier:** required_intelligence "pro" — síntese arquitetural a partir da leitura do código; rebaixar para "deep-context" apenas se o corpus de um repo superar 100k tokens (AGENTS.md §0.4).
- **Copyright:** aplicar o header padrão da holding (skill backforend-copyright §1.1) a qualquer doc regravado, exceto `CHANGELOG.md` (registro de release não recebe header).

## 8. Success Metrics

- 4/4 documentos presentes e preenchidos em cada repo filho.
- Zero alterações em código de produto (diff limpo na raiz).
- Documentação local sem referências upward (AGENTS.md §7.1).

## 9. Open Questions

- Repos ainda vazios (sem código): o doc deve registrar "repo sem código — aguardando primeira implementação" em vez de inventar conteúdo.
- Necessidade de incluir a documentação global da raiz no escopo futuro.
