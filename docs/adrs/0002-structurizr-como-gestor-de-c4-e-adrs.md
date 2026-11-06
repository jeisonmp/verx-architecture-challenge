---
status: accepted
date: 2026-11-05
decision-makers: Max
---
# 0002. Usar Structurizr como gestor de C4 e ADRs

## Contexto e problema

A solução exige documentação formal do modelo C4 (Contexto e Containers, níveis L1/L2) e um registro de decisões arquiteturais (ADRs). Precisamos de uma ferramenta que mantenha diagramas e decisões versionados como código no repositório, executável localmente via docker-compose e sem licença paga.

## Histórico da discussão (sessão de iteração — Q&A)

> Histórico de iteração Q&A não registrado — decisão anterior à adoção deste padrão.

## Decisão

Adotar o **Structurizr** (imagem `structurizr/structurizr`, executada em modo `local`), do Simon Brown (criador do modelo C4), como gestor de C4 e ADRs. O modelo C4 é descrito em `workspace.dsl` (diagrams-as-code) e os ADRs (formato MADR) são importados via `!adrs ./docs/adrs`. O serviço sobe com `docker compose up` e é acessado em `http://localhost:8089`.

## Consequências

* Bom, porque C4 e ADRs ficam versionados como código no mesmo repositório, revisáveis em pull request.
* Bom, porque a ferramenta é do próprio criador do C4, com fidelidade total ao modelo.
* Bom, porque o modo `local` é gratuito e roda em Docker local, sem infraestrutura adicional.
* Ruim, porque introduz a DSL própria do Structurizr (curva de aprendizado).
* Ruim, porque o modo `local` é single-user/local (colaboração e nuvem são pagas).
