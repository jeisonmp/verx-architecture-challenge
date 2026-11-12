---
project: raiz
---

# PRD: Extratores GraphRAG — Índice Semântico dos Repos

**Type:** feature
**Projeto afetado:** raiz — cross-repo (1 script extrator por repo).

## 1. Introduction/Overview

O onboarding gerou a topologia e o `repos.py` com **placeholders** de extrator
(`scripts/graphrag_generate_graph.py`). Para o índice semântico (GraphRAG)
funcionar, cada repo precisa de um **extrator real**: um script que emite o grafo
de símbolos (`nodes`/`edges`) no schema comum (ADR-004). Esta PRD escreve esses
extratores, um por repo, depois que a `prd-setup-inicial` documentou o tech stack.

## 2. Goals

- Um extrator por repo, emitindo `{nodes, edges}` no schema comum.
- Determinístico (mesma entrada → saída byte-idêntica).
- Zero alteração em código de produto (apenas o script novo).

## User Stories
### US-001: Extrator GraphRAG do repo raiz

**required_intelligence:** "pro"

**Description:** Como a persona raiz-dev, analisar o tech stack e os símbolos reais da raiz (entidades, use cases, endpoints, tabelas, datasets...) e escrever `scripts/graphrag_generate_*.{py,ts,dart}` que emite o JSON no schema comum `{nodes, edges}`.

**Acceptance Criteria:**

- [x] O script roda e emite `nodes[]` + `edges[]` válidos.
- [x] `nodes[].file`/`line` relativos ao repo (nunca absolutos).
- [x] Determinístico (duas execuções → bytes idênticos).
- [x] Header de copyright no script.
- [x] Nenhum arquivo de código de produto alterado (só o script novo).
- [x] `ia/graphrag/repos.py` atualizado com `script`/`command`/`output` reais + `cwd`/`host_only` se aplicável.

> **Nota (US-001):** marcada como concluída no estado da run com a ressalva de
> **bloqueio de ambiente** — `/workspace` (raiz) e `/workspace/ia/graphrag` eram
> `node:node 755` (sem escrita para o usuário `ralph`, uid=1001). O fix humano
> (`chown -R node:ralph /workspace/ia` + criar `/workspace/scripts` gravável) e a
> reabertura da story estão registrados em `notes` do `prd.json`/log da run.

## 4. Functional Requirements

- FR-1: schema comum — `nodes`: id/type/name/file/line/attrs; `edges`: from/to/type/file/line.
- FR-2: determinismo byte-a-byte (duas execuções → bytes idênticos).
- FR-3: `nodes[].file`/`line` relativos ao repo; `attrs.repo` preenchido pela mesclagem.

## 5. Non-Goals (Out of Scope)

- Chunking/embedding/API de consulta (fora do Módulo 3).
- Regenerar os grafos na própria PRD (o `graphrag-run.sh run && merge` faz isso depois).
- Alterar código de produto.

## 6. Design Considerations

Estratégia de parseamento por tech stack: AST (TypeScript/Dart/Python), introspecção (FastAPI/Pydantic), parsing (YAML/HCL/OpenAPI). O `prd-setup-inicial` já documentou o stack em cada `<repo>/docs/ARCHITECTURE.md` — usar como fonte.

## 7. Technical Considerations

- **Tier:** `required_intelligence "pro"` — síntese arquitetural (escolher a estratégia de parseamento); rebaixar para `deep-context` se o corpus do repo superar 100k tokens (AGENTS.md §0.4).
- **Copyright:** header padrão da holding (skill backforend-copyright §1.1) em todo script gerado.
- **Determinismo:** o gate pós-rodada (Check 3) re-executa o extrator e compara bytes.

## 8. Success Metrics

- 1 extrator por repo filho, emitindo JSON válido.
- `graphrag-run.sh run && merge` exit 0 (gate Checks 1-5).
- `repos.py` sem placeholders.

## 9. Open Questions

- Repos ainda vazios (sem código): registrar "repo sem código" e pular o extrator.
- Extratores em linguagens sem suporte a AST/parseamento: estratégia manual + nota no `round_notes.json`.
