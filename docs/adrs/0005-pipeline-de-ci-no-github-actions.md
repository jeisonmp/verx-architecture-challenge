---
status: proposed
date: 2026-11-05
decision-makers: Max
---
# 0005. Pipeline de CI no GitHub Actions (dev → qas → master)

## Contexto e problema

A solução exige CI automatizado no GitHub Actions, testes cobrindo cenários de falha (DLQ, autorização, idempotência), load test provando 50 req/s e correção de vulnerabilidades em dependências transitivas. Precisamos definir o fluxo da pipeline, os gates de qualidade e onde cada etapa roda — sem gerar imagens no registry (não há aplicação neste repositório).

## Alternativas consideradas

* Pipeline única sem gates de qualidade por branch.
* Gates idênticos em todas as branches (`dev`, `qas` e `master`).
* Gates escalonados por branch — análise de código (`dev`) → qualidade (`qas`) → release (`master`), com validações manuais no ambiente local (escolhida).

## Histórico da discussão (sessão de iteração — Q&A)

> Histórico de iteração Q&A não registrado — decisão anterior à adoção deste padrão.

## Decisão

Adotar um workflow GitHub Actions com fluxo de branches `dev → qas → master`, mantendo o sistema de tags existente (base `.github/workflows/release.yml`).

### 1. PR → `dev` — análise de código

* **SAST (estática):** SonarQube + linters/analisadores estáticos do runtime definido no ADR-0006.
* **SCA (dependências):** OWASP Dependency-Check / Dependabot (vulnerabilidades transitivas).
* **Segredos:** gitleaks.
* **DAST (dinâmica):** movida para a validação manual (§4) — exige aplicação em execução e o ambiente é 100% local.
* Relatório publicado na execução da pipeline (artefato de resumo).
* **Gate:** nenhum risco **crítico** (blocker). Riscos **médios** podem ser mitigados/aceitos com justificativa documentada.

### 2. PR `dev → qas` — qualidade

* Todos os testes unitários e de integração (containers: PostgreSQL + Kafka) com sucesso.
* Cobertura de código: gate **80% de linhas** (mínimo bloqueante), meta **90%** no núcleo de domínio (outbox/inbox) — ferramenta conforme o runtime do ADR-0006.

### 3. PR `qas → master` — release

* Validação de IaC: `terraform validate` + tfsec/checkov (automático).
* Release/tag mantida pelo workflow existente (semantic-release — `release.yml`).

### 4. Validação manual (ambiente 100% local)

O ambiente é totalmente local (docker-compose, sem cloud/registry). As etapas que exigem aplicação em execução são **manuais** e ficam documentadas no `README.md` (seção "CI/CD, Testes e Validação"):

* **DAST (dinâmica):** OWASP ZAP contra a aplicação em execução local.
* **Testes end-to-end (e2e):** fluxo completo escrita → mensageria → leitura.
* **Load test:** K6/JMeter provando 50 req/s no Consolidado.

### Sem imagens no registry

A pipeline compila e testa, mas não faz build/push de imagem Docker (não há aplicação neste repositório).

## Consequências

* Bom, porque os gates (0 crítico; testes + cobertura) bloqueiam regressões e vulnerabilidades críticas antes da mainline.
* Bom, porque SAST + SCA atacam diretamente as vulnerabilidades transitivas.
* Bom, porque o load test (manual) valida os 50 req/s exigidos.
* Ruim, porque DAST e e2e não são automatizados no CI (exigem aplicação local em execução) — dependem da disciplina da etapa manual.
* Ruim, porque cobertura de 90% global penaliza código de integração/consumer — o gate de 80% mitiga isso.
