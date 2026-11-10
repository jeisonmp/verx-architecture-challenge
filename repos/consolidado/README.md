# Consolidado (Microsserviço B — Leitura)

> Parte da solução **Fluxo de Caixa (CQRS)** (`src/`). Modelo C4 em `../../workspace.dsl`
> e decisões em `../../docs/adrs/`.

Microsserviço de **leitura**: consome o tópico `lancamento.criado`, aplica **Inbox**
com idempotência e mantém a projeção de saldo. Expõe `GET /consolidado` (50 req/s).

- **Stack:** Java 21 · Spring Boot 3 · JDBI + Spring Data JDBC · PostgreSQL · Kafka.
- **Execução local / testes:** Sprint 1+ (ver README do projeto pai).
