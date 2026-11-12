# Fluxo de Caixa (CQRS)

Sistema de fluxo de caixa com desacoplamento entre escrita e leitura via CQRS,
mensageria e os padrões Transactional Outbox / Inbox.

## Arquitetura (resumo)

| Serviço | Papel | Stack |
|---|---|---|
| **CashFlow · Lançamentos** (A) | Escrita — `POST /lancamentos` + Transactional Outbox | Java 21 · Spring Boot 3 · PostgreSQL · Kafka |
| **Kafka (MSK Serverless)** | Barramento de eventos (escrita → leitura) | — |
| **CashFlow · Consolidado** (B) | Leitura — Inbox/idempotência + projeção, `GET /consolidado` (50 req/s) | Java 21 · Spring Boot 3 · PostgreSQL · Kafka |
| **Structurizr** | Modelo C4 + ADRs | Structurizr DSL |

- **CQRS:** o serviço A grava o lançamento e publica o evento `lancamento.criado` via
  **Transactional Outbox**; o serviço B consome o evento com **Inbox/idempotência** e
  mantém a projeção de saldo consultada por `GET /consolidado`.
- Decisões em `docs/adrs/` · modelo C4 em `workspace.dsl` (Structurizr) ·
  planejamento em `docs/ROADMAP.md`.

## Execução local

Pré-requisito: Docker + Docker Compose. O ambiente completo sobe com **um comando**:

```bash
cp .env.example .env      # credenciais de dev local (gitignored)
docker compose up --build
```

> O `docker compose` carrega automaticamente o `.env` do diretório do compose (raiz),
> sem `--env-file`.

| Serviço | Endpoint local | Health check |
|---|---|---|
| Lançamentos (A) | http://localhost:8080 | http://localhost:8080/actuator/health |
| Consolidado (B) | http://localhost:8081 | http://localhost:8081/actuator/health |
| PostgreSQL | `localhost:5432` (schemas `lancamentos` e `consolidado` — ADR-0013) | — |
| Kafka | `kafka:9092` (rede interna do compose) | — |
| Structurizr | http://localhost:8089 | — |

- Schemas criados na primeira subida do volume por `postgres/init.sql` (ADR-0013).
- As variáveis vêm do `.env` (gitignored); o compose usa `${VAR:?}` obrigatório —
  sem credencial hardcoded.

## Testes e validação

### Automatizado no GitHub Actions

| PR | Etapas | Gate de merge |
|---|---|---|
| **`feat/fix/*` → `dev`** | SAST (SonarQube + análise estática do runtime) · SCA (OWASP Dependency-Check/Dependabot) · gitleaks | **0 risco crítico** (médios mitigáveis) |
| **`dev` → `master`** | Testes unitários + integração (containers: PostgreSQL + Kafka) · cobertura de código | 100% dos testes passando · JaCoCo ≥ 80% (meta 90% no núcleo outbox/inbox) |
| **`master`** | `terraform validate` + Trivy/checkov · release/tag (semantic-release) | — |

### Comandos locais

```bash
# Testes + cobertura (gate JaCoCo ≥ 80%) — cada serviço
mvn -B -f repos/lancamentos verify
mvn -B -f repos/consolidado verify
```

### Validação manual (local)

Como o ambiente é 100% local (sem cloud/registry), estas etapas **não rodam no CI** e
são executadas manualmente:

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

## Fluxo de branches

```
feat/fix/* → dev → master
```

## Release e tags

- Release, versão, `CHANGELOG.md` e tag são derivados dos Conventional Commits pelo
  semantic-release (base `.github/workflows/release.yml`).
- A release é disparada no `push` a `master`/`dev` **após o merge** de um PR. A
  **aprovação do PR** é garantida por **branch protection** (`master` e `dev` com
  "Require pull request before merging" + ≥1 approval) — não pelo workflow.
- A pipeline **não** faz build/push de imagem Docker.

## Documentação

- `docs/adrs/` — decisões de arquitetura (MADR)
- `workspace.dsl` — modelo C4
- `docs/ROADMAP.md` — planejamento
- `repos/infra/README.md` — IaC/cloud (Terraform, state, FinOps, bootstrap OIDC)
