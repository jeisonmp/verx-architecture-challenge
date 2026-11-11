# Fluxo de Caixa (CQRS)

Sistema de fluxo de caixa com desacoplamento entre escrita e leitura via CQRS, mensageria e os padrões Transactional Outbox / Inbox.

## Arquitetura (resumo)

| Serviço | Papel | Stack |
|---|---|---|
| **CashFlow · Lançamentos** (A) | Escrita — `POST /lancamentos` + Transactional Outbox | Java 21 · Spring Boot 3 · PostgreSQL |
| **Kafka (MSK Serverless)** | Barramento de eventos (escrita → leitura) | — |
| **CashFlow · Consolidado** (B) | Leitura — Inbox + projeção, `GET /consolidado` (50 req/s) | Java 21 · Spring Boot 3 · PostgreSQL |

Decisões em `docs/adrs/` · modelo C4 em `workspace.dsl` (Structurizr) · planejamento em `docs/ROADMAP.md`.

## Execução local

```bash
docker compose up        # sobe o Structurizr (gestor de C4/ADRs)
```

Acesse o Structurizr em http://localhost:8089 (modelo C4 + ADRs).

## CI/CD, Testes e Validação (ambiente 100% local)

> Esta solução **não publica imagens em registry** e **não roda em nuvem** — não há aplicação neste repositório. Todo o ciclo de validação é local: o que é automatizado roda no GitHub Actions; o que exige aplicação em execução é uma **etapa manual** descrita abaixo.

### Fluxo de branches

```
feat/fix/* → dev → master
```

### Automatizado no GitHub Actions

| PR | Etapas | Gate de merge |
|---|---|---|
| **`feat/fix/*` → `dev`** | SAST (SonarQube + análise estática do runtime) · SCA (OWASP Dependency-Check/Dependabot) · gitleaks | **0 risco crítico** (médios mitigáveis) |
| **`dev` → `master`** | Testes unitários + integração (containers: PostgreSQL + Kafka) · cobertura de código | 100% dos testes passando · cobertura ≥ 80% (meta 90% no núcleo outbox/inbox) |
| **`master`** | `terraform validate` + Trivy/checkov · release/tag (semantic-release) | — |

### Validação manual (local)

Como o ambiente é 100% local (sem cloud/registry), estas etapas **não rodam no CI** e são executadas manualmente. Os comandos exatos são consolidados quando os serviços existirem (Sprint 1+).

1. **DAST (análise dinâmica)** — OWASP ZAP contra a aplicação em execução local:

   ```bash
   docker run -t owasp/zap2docker-stable zap-baseline.py -t http://localhost:8080
   ```

2. **Testes end-to-end (e2e)** — fluxo completo escrita → mensageria → leitura:

   ```bash
   ./mvnw verify -Pe2e
   ```

3. **Load test** — provar 50 req/s no Consolidado (K6):

   ```bash
   k6 run load-test/consolidado.js
   ```

4. **IaC** — validação de segurança da infraestrutura:

   ```bash
   terraform validate && trivy config .
   ```

### Release e tags

- Release, versão, `CHANGELOG.md` e tag são derivados dos Conventional Commits pelo semantic-release (base `.github/workflows/release.yml`).
- A release é disparada no `push` a `master`/`dev` **após o merge** de um PR. A **aprovação do PR** é garantida por **branch protection** (`master` e `dev` com "Require pull request before merging" + ≥1 approval) — não pelo workflow.
- A pipeline **não** faz build/push de imagem Docker.

## Documentação

- `docs/adrs/` — decisões de arquitetura (MADR)
- `workspace.dsl` — modelo C4
- `docs/ROADMAP.md` — planejamento
