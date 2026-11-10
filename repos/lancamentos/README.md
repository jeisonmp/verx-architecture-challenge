# Lançamentos (Microsserviço A — Escrita)

> Parte da solução **Fluxo de Caixa (CQRS)** (`src/`). Modelo C4 em `../../workspace.dsl`
> e decisões em `../../docs/adrs/`.

Microsserviço de **escrita**: expõe `POST /lancamentos` e publica eventos no tópico
`lancamento.criado` via **Transactional Outbox** (mesma transação DB + tabela outbox).

- **Stack:** Java 21 · Spring Boot 3 · JDBI + Spring Data JDBC · PostgreSQL · Kafka.
- **Execução local / testes:** Sprint 1+ (ver README do projeto pai).
