---
project: raiz
---

# PRD: Consolidar o compose local na raiz e reorganizar os READMEs

**Type:** feature (refatoração de organização)
**Projeto afetado:** raiz — cross-repo (raiz + `repos/infra`, com toques em
`repos/lancamentos` e `repos/consolidado`).

## 1. Introduction/Overview

O ambiente local de ponta a ponta (PostgreSQL + Kafka + apps A/B) hoje vive em
`repos/infra/docker-compose.yml` — um diretório cujo escopo é **IaC/Terraform AWS**.
Quem quer subir o ambiente precisa descobrir o compose "escondido" na pasta de infra e
ler um README que mistura instruções de subir com documentação de cloud.

Esta PRD reorganiza a entrega:

- a stack de dev local passa a subir pelo compose da **raiz** (`docker-compose.yml`, que
  hoje só tem o Structurizr);
- cada README documenta apenas o seu escopo: a raiz explica **arquitetura + como subir +
  como testar**; o `repos/infra` fica com a documentação de **IaC/cloud**;
- os artefatos de dev (`.env`, `.env.example`, `postgres/init.sql`) vão para a raiz.

## 2. Goals

- Um único comando da raiz (`docker compose up --build`) sobe a stack completa
  (Structurizr + PostgreSQL + Kafka + A + B) usando as credenciais de dev do `.env` raiz.
- O README raiz contém: arquitetura (resumo), comandos para subir o ambiente e comandos
  de testes (unitários/integração + validações manuais).
- O README de `repos/infra` contém somente o que é pertinente ao IaC/cloud (Terraform,
  state, FinOps, bootstrap OIDC), sem instruções de subir o compose.
- Nenhuma referência residual a `repos/infra/docker-compose.yml` (grep vazio).
- O ambiente sobe de ponta a ponta exatamente como antes (mesmos serviços, portas e
  health checks), apenas com caminhos/arquivos reorganizados.

## 3. User Stories

### US-001: Consolidar o compose local na raiz

**required_intelligence:** "flash"

**Description:** Como desenvolvedor, quero um único `docker-compose.yml` na raiz que suba
Structurizr + PostgreSQL + Kafka + apps A/B, para subir o ambiente de dev com um comando,
sem procurar na pasta de infra.

**Acceptance Criteria:**

- [x] O `docker-compose.yml` da raiz passa a conter, além do `structurizr`, os serviços
      `postgres`, `kafka`, `lancamentos` e `consolidado` (copiados de
      `repos/infra/docker-compose.yml`, mantendo `name: fluxo-caixa`).
- [x] Build contexts ajustados para `./repos/lancamentos` e `./repos/consolidado`
      (antes `../lancamentos`/`../consolidado`).
- [x] O volume de init do Postgres aponta para `./postgres/init.sql` (raiz).
- [x] Nenhum valor de credencial hardcoded permanece — continuam via `${VAR:?}` do `.env`.
- [x] `repos/infra/docker-compose.yml` é removido.
- [x] `docker compose config` valida o arquivo da raiz sem erro (com `.env` presente).

### US-002: Mover os artefatos de dev para a raiz

**required_intelligence:** "flash"

**Description:** Como desenvolvedor, quero `.env`, `.env.example` e `postgres/init.sql`
na raiz, para que os artefatos de dev acompanhem o compose que os consome.

**Acceptance Criteria:**

- [x] `.env.example` movido de `repos/infra/.env.example` para a raiz (versionado), com o
      comentário de cópia atualizado para `cp .env.example .env`.
- [x] `.env` (gitignored) passa a existir na raiz, gerado a partir do `.env.example`.
- [x] `postgres/init.sql` movido de `repos/infra/postgres/init.sql` para
      `postgres/init.sql` na raiz, com o conteúdo preservado (schemas `lancamentos`/
      `consolidado`, ADR-0013).
- [x] O `.gitignore` da raiz passa a ignorar `.env` (e mantém `.env.example` rastreado).
- [x] Não restam `.env`, `.env.example` nem `postgres/init.sql` residuais em `repos/infra/`.

### US-003: Reescrever o README da raiz (arquitetura + subir + testes)

**required_intelligence:** "flash"

**Description:** Como desenvolvedor, quero que o README da raiz seja o ponto único de
entrada com arquitetura, comandos para subir e comandos de testes, para não ter que
procurar em outros READMEs.

**Acceptance Criteria:**

- [x] A seção "Execução local" do README raiz documenta `cp .env.example .env` seguido de
      `docker compose up --build`, com a tabela de endpoints (Lançamentos A :8080,
      Consolidado B :8081, PostgreSQL :5432, Kafka `kafka:9092`, Structurizr :8089) e os
      health checks em `/actuator/health`.
- [x] O README raiz mantém/amplia o resumo de arquitetura (tabela de serviços + CQRS,
      mensageria e Outbox/Inbox).
- [x] A seção de testes lista `mvn -B -f repos/lancamentos verify` e
      `mvn -B -f repos/consolidado verify` (gate JaCoCo ≥ 80%).
- [x] A seção de validações manuais é preservada (ZAP DAST, e2e, k6 load test).
- [x] A "execução local" não é mais apenas o Structurizr — a stack completa é o default.

### US-004: Reduzir o README de infra ao escopo IaC

**required_intelligence:** "flash"

**Description:** Como desenvolvedor, quero que o README de `repos/infra` fale apenas de
IaC/cloud (Terraform, state, FinOps, bootstrap), para não duplicar instruções de dev.

**Acceptance Criteria:**

- [x] Removida a seção "Execução local completa (docker compose)" do `repos/infra/README.md`.
- [x] O README de infra passa a apontar (link, sem duplicar) para o README raiz quando o
      assunto é subir o ambiente local.
- [x] A seção "Organização dos repositórios" reflete que o compose não está mais em `infra`.
- [x] O conteúdo de IaC (state backend, FinOps, bootstrap OIDC, arquitetura de infra)
      permanece intacto.

### US-005: Atualizar AGENTS.md e referências cruzadas

**required_intelligence:** "flash"

**Description:** Como desenvolvedor, quero que nenhum `AGENTS.md` aponte para
`repos/infra/docker-compose.yml`, para que a documentação do agente reflita a nova
organização.

**Acceptance Criteria:**

- [x] `repos/infra/AGENTS.md` (§ Execução local) atualizado para apontar o compose da raiz
      e o `cp .env.example .env` da raiz.
- [x] `repos/lancamentos/AGENTS.md` e `repos/consolidado/AGENTS.md` atualizam o ponteiro
      "Compose completo em `repos/infra/docker-compose.yml`" para a raiz.
- [x] `grep -rn "repos/infra/docker-compose" .` retorna vazio (exceto histórico git).
- [x] Nenhum link de README/AGENTS.md quebrado (auditoria via `backforend-docs-audit`).

## 4. Functional Requirements

- FR-1: O comando `docker compose up --build` na raiz sobe Structurizr (8089),
  PostgreSQL (5432), Kafka (broker único KRaft), Lançamentos (8080) e Consolidado (8081).
- FR-2: O compose da raiz carrega as credenciais de dev do `.env` da raiz (auto-load do
  docker compose), sem `--env-file`.
- FR-3: O README da raiz documenta a arquitetura (tabela de serviços + fluxo CQRS) e os
  comandos de subir e testar.
- FR-4: O README de `repos/infra` documenta somente IaC/cloud e referencia o README raiz
  para execução local.
- FR-5: Os artefatos de dev (`.env`, `.env.example`, `postgres/init.sql`) residem na raiz.

## 5. Non-Goals

- **Não** alterar o conteúdo do IaC (`*.tf`, `lambda/`, `environments/`) nem o
  `terraform validate`/Trivy/checkov.
- **Não** mudar imagens/versões dos serviços (PostgreSQL 16, cp-kafka 7.8.0, apps A/B).
- **Não** alterar topologia de rede, volumes ou healthchecks do compose — só mover o arquivo.
- **Não** criar profiles/composes múltiplos (decisão tomada: um único `docker-compose.yml`).
- **Não** alterar os CI workflows — eles não referenciam o compose (usam service containers
  do GitHub Actions para PostgreSQL/Kafka).
- **Não** tocar no `prd.json`/estado do Ralph.

## 6. Design Considerations

- Compose único na raiz (Structurizr + stack) — decisão já tomada (simplicidade, um arquivo).
- README raiz como "front door" da execução local; README de infra como referência de IaC.

## 7. Technical Considerations

- O docker compose auto-carrega `.env` do diretório do compose (raiz), dispensando
  `--env-file repos/infra/.env`.
- Ajustar paths relativos: build contexts (`./repos/*`) e volume do init
  (`./postgres/init.sql`).
- Impacto rastreado via grep (`repos/infra/docker-compose`): `README.md` raiz,
  `repos/infra/README.md`, `repos/{infra,lancamentos,consolidado}/AGENTS.md`.
- CI (`quality.yml`, `infra.yml`) não referencia o compose — não é impactado.

## 8. Success Metrics

- `docker compose up --build` na raiz sobe os 5 serviços com health checks UP (paridade
  com o comportamento atual).
- `grep -rn "repos/infra/docker-compose"` → 0 ocorrências.
- README raiz contém os três blocos (arquitetura, subir, testes) sem depender do README de infra.
- `repos/infra/README.md` não contém mais instruções de execução local do compose.

## 9. Open Questions

- O `postgres/init.sql` permanece como inicialização única (primeira subida do volume) ou
  ganha migração idempotente (Flyway) no Sprint 2/3?
- Vale consolidar as validações manuais (ZAP/k6/e2e) num script único na raiz
  (ex.: `scripts/validate-local.sh`) em vez de comandos soltos no README?
