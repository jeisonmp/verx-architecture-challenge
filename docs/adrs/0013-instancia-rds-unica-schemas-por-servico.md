---
status: accepted
date: 2026-11-09
decision-makers: Max
---
# 0013. Instância RDS única (schemas por serviço) em dev — consolidação FinOps do database-per-service

## Contexto e problema

A estimativa FinOps (ADR-0012, `docs/finops/`) aponta as **duas instâncias RDS**
(`db.t4g.micro` — Lançamentos e Consolidado) como o maior driver de custo em `dev`
(~US$ 23,36/mês de um total de ~US$ 44,96), estourando o alvo de ~US$ 30/mês.
A pergunta é se o database-per-service (ADR-0004) exige, de fato, **duas instâncias
físicas** ou se a mesma garantia de isolamento pode ser obtida com **uma instância
única e dois schemas lógicos**.

## Alternativas consideradas

* **Duas instâncias RDS (status quo)** — isolamento físico total, porém ~US$ 23,36/mês.
* **Uma instância RDS + dois schemas** — escolhida.
* **Aurora Serverless v2 (min 0,5 ACU)** — escalável, mas cobra por ACU-hora e não
  elimina a duplicidade lógica; alternativa válida, não adotada agora (YAGNI).

## Decisão

Adotar, em `dev`, **uma única instância RDS PostgreSQL** (`db.t4g.micro`) com **dois
schemas lógicos** (`lancamentos` e `consolidado`), cada um com **role RDS IAM própria**:

* **Lançamentos (escrita):** schema `lancamentos` + tabela `outbox` (Transactional Outbox).
* **Consolidado (leitura):** schema `consolidado` + projeção desnormalizada de saldo.

A separação física é adiada (YAGNI): o isolamento lógico que o ADR-0004 persegue
(anti-monólito distribuído — sem tabela compartilhada) é preservado no nível de schema;
a comunicação entre A e B continua exclusivamente via Kafka (ADR-0003), sem transação ou
FK cross-schema. O requisito "o controle não pode cair se o consolidado cair" (ADR-0003)
já é garantido pelo barramento, não por bancos separados — e a projeção de leitura é
reconstruível via replay do Kafka.

## Consequências

* Bom, porque reduz o custo RDS de ~US$ 23,36 para ~US$ 11,68/mês, fechando dev em ~US$ 33/mês.
* Bom, porque mantém o contrato lógico do database-per-service (schema ownership + roles IAM próprias).
* Bom, porque reduz a superfície operacional em dev (uma instância para backup/PITR).
* Ruim, porque perde isolamento físico de falha e escala independente entre escrita e leitura
  — aceitável a 50 req/s e reconstruível via replay; em produção, separar de volta é uma
  opção que permanece aberta se a carga ou o isolamento de falha real exigirem.
* Ruim, porque a criação dos schemas/roles passa a depender de migração (Flyway), não só do
  provisionamento do RDS.
