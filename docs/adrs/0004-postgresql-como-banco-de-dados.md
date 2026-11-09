---
status: accepted
date: 2026-11-05
decision-makers: Max
---
# 0004. PostgreSQL como banco de dados dos microsserviços

## Contexto e problema

Cada microsserviço precisa persistir seus dados de forma independente (database-per-service), evitando acoplamento no nível de dados (anti-padrão "monólito distribuído"). O Lançamentos exige transações ACID para o Transactional Outbox; o Consolidado exige uma projeção de saldo otimizada para 50 req/s de leitura.

## Alternativas consideradas

* **PostgreSQL (AWS Aurora/RDS)** — escolhida.
* **MongoDB/NoSQL:** schema flexível, porém transações multi-documento menos robustas para o Outbox e sem ganho para a volumetria exigida.

## Histórico da discussão (sessão de iteração — Q&A)

> Histórico de iteração Q&A não registrado — decisão anterior à adoção deste padrão.

## Decisão

Adotar **PostgreSQL (AWS Aurora/RDS)** para ambos os microsserviços, com **um banco por serviço**:

* **Lançamentos (escrita):** schema relacional normalizado + tabela `outbox` (grava lançamento e evento na mesma transação ACID).
* **Consolidado (leitura):** tabela de projeção desnormalizada de saldo, atualizada pelo consumer com idempotência.

Justificativa: PostgreSQL oferece as transações ACID necessárias ao Outbox (atomicidade entre lançamento e evento), é maduro para dados financeiros relacionais e atende com folga os 50 req/s. Um único motor relacional nos dois serviços reduz a superfície operacional (YAGNI — não há necessidade de NoSQL nesta escala).

## Consequências

* Bom, porque as transações ACID garantem a atomicidade do Transactional Outbox.
* Bom, porque um único motor relacional simplifica operação e migrações (Flyway/Liquibase).
* Bom, porque Aurora/RDS é gerenciado e compatível com o ecossistema AWS.
* Ruim, porque a projeção de leitura exige disciplina para manter a desnormalização consistente (mitigada pelo Inbox/idempotência).
* Ruim, porque database-per-service duplica a gestão de dois bancos (custo e operação levemente maiores).
