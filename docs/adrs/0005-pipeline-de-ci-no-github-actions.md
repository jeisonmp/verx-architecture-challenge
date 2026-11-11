---
status: accepted
date: 2026-11-05
decision-makers: Max
---
# 0005. Pipeline de CI no GitHub Actions (dev → master)

## Contexto e problema

A solução exige CI automatizado no GitHub Actions, testes cobrindo cenários de falha (DLQ, autorização, idempotência), load test provando 50 req/s e correção de vulnerabilidades em dependências transitivas. Precisamos definir o fluxo da pipeline, os gates de qualidade e onde cada etapa roda — sem gerar imagens no registry (não há aplicação neste repositório).

## Alternativas consideradas

* Pipeline única sem gates de qualidade por branch.
* Gates idênticos em todas as branches (`dev` e `master`).
* Gates escalonados por branch — análise de código (PR → `dev`) → qualidade (PR `dev` → `master`) → release (`master`), com validações manuais no ambiente local (escolhida).

## Histórico da discussão (sessão de iteração — Q&A)

### Q1 — "O repositório é público e roda no agente GitHub-hosted. Conseguimos inserir os gates de teste e cobertura propostos?"

**R:** Sim. Repositórios públicos têm **minutes grátis ilimitados** no GitHub Actions e o runner padrão (`ubuntu-latest`: 2 vCPU / 7 GB RAM / 14 GB SSD) já traz Docker. Os gates rodam no próprio agente: testes unitários + integração (Testcontainers: PostgreSQL + Kafka efêmeros), cobertura via **JaCoCo** (goal `check` com regra ≥ 80%), SAST/SCA e gitleaks. Não há necessidade de runner auto-hospedado.

### Q2 — "E o DAST?"

**R:** Fica **fora do escopo** por ora — o DAST exige a aplicação em execução, incompatível com o CI atual (sem deploy/registry). Registrado como **pendência futura** (§4): será revisitado quando houver ambiente efêmero de execução.

### Q3 — "E quanto aos testes de integração?"

**R:** Mantidos **no CI** (§2), via **Testcontainers** (PostgreSQL + Kafka efêmeros por execução), conforme ADR-0003. Viáveis no runner público: o Docker já está disponível e os containers sobem/derrubam dentro do job. *Service containers* do GitHub ficam como alternativa documentada caso o Testcontainers fique pesado.

## Decisão

Adotar um workflow GitHub Actions com fluxo de branches `dev → master`, mantendo o sistema de tags existente (base `.github/workflows/release.yml`).

### 1. PR → `dev` — análise de código

* **SAST (estática):** SonarCloud (SaaS, gratuito para repos públicos) + linters/analisadores estáticos do runtime definido no ADR-0006.
* **SCA (dependências):** OWASP Dependency-Check / Dependabot (vulnerabilidades transitivas).
* **Segredos:** gitleaks.
* **DAST (dinâmica):** **fora do escopo** do CI por ora — pendência futura (ver §4).
* Relatório publicado na execução da pipeline (artefato de resumo).
* **Gate:** nenhum risco **crítico** (blocker). Riscos **médios** podem ser mitigados/aceitos com justificativa documentada.

### 2. PR `dev → master` — qualidade

* Todos os testes unitários e de integração com sucesso — integração via **Testcontainers** (PostgreSQL + Kafka efêmeros, ADR-0003), rodando no próprio runner GitHub-hosted (Docker disponível).
* Cobertura de código via **JaCoCo** (goal `check` com regra de cobertura): gate **80% de linhas** (mínimo bloqueante), meta **90%** no núcleo de domínio (outbox/inbox) — o job falha se ficar abaixo do gate.

### 3. `master` — release

* Validação de IaC: `terraform validate` + Trivy/checkov (automático).
* Release/tag mantida pelo workflow existente (semantic-release — `release.yml`).

### 4. Validação manual (ambiente 100% local)

O ambiente é totalmente local (docker-compose, sem cloud/registry). As etapas que exigem aplicação em execução são **manuais** e ficam documentadas no `README.md` (seção "CI/CD, Testes e Validação"):

* **Testes end-to-end (e2e):** fluxo completo escrita → mensageria → leitura.
* **Load test:** K6/JMeter provando 50 req/s no Consolidado.

> **Pendência futura — DAST:** OWASP ZAP contra a aplicação em execução foi **removido do escopo atual** (exige ambiente efêmero de execução, indisponível no CI 100% local). Reaberto como ADR quando houver deploy de um ambiente de validação.

### Sem imagens no registry

A pipeline compila e testa, mas não faz build/push de imagem Docker (não há aplicação neste repositório).

## Consequências

* Bom, porque os gates (0 crítico; testes + cobertura) bloqueiam regressões e vulnerabilidades críticas antes da mainline.
* Bom, porque SAST + SCA atacam diretamente as vulnerabilidades transitivas.
* Bom, porque o load test (manual) valida os 50 req/s exigidos.
* Ruim, porque DAST e e2e não são automatizados no CI (exigem aplicação local em execução) — dependem da disciplina da etapa manual.
* Ruim, porque cobertura de 90% global penaliza código de integração/consumer — o gate de 80% mitiga isso.
