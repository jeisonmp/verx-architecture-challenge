<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# BUSINESS — Fluxo de Caixa (CQRS)

> Mapeamento de domínios funcionais e capacidades de negócio da solução.
> Decisões-fonte em `docs/adrs/` (MADR) e planejamento em `docs/ROADMAP.md`.

## Visão geral

**Fluxo de Caixa** é uma solução de controle financeiro com **escrita e leitura
desacopladas (CQRS)**: registrar um lançamento (escrita) e consultar o saldo
consolidado (leitura) são capacidades de negócio independentes, ligadas por eventos.

O requisito estruturante do negócio é: **a operação de registro (controle) não pode
ficar indisponível se o serviço de leitura (consolidado) cair**. Isso impõe o
desacoplamento entre escrita e leitura via mensageria (ADR-0003).

## Domínios funcionais

| Domínio | Responsabilidade | Microsserviço |
|---|---|---|
| **Lançamentos** | Registrar entradas/saídas financeiras com atomicidade entre o lançamento e o evento | A (escrita) |
| **Consolidado** | Projetar e expor o saldo consolidado otimizado para leitura | B (leitura) |

## Capacidades de negócio

1. **Registrar lançamento** — `POST /lancamentos` (A). Grava o lançamento e o evento na
   mesma transação (Transactional Outbox — ADR-0003/0004).
2. **Consultar saldo consolidado** — `GET /consolidado` (B). Retorna a projeção de
   saldo desnormalizada, atendendo 50 req/s (ADR-0004).

## Regras de negócio (requisitos não funcionais de domínio)

- **Disponibilidade independente:** a queda do Consolidado não derruba a escrita
  (ADR-0003). O consumo do evento pode atrasar, mas o registro nunca é perdido.
- **Consistência eventual:** perder um lançamento na porta de entrada é inaceitável;
  exibir o saldo consolidado com pequeno atraso (~segundos) é aceitável (gabarito do
  desafio).
- **Idempotência:** um mesmo evento processado mais de uma vez não pode contar duas
  vezes no saldo (Inbox — ADR-0003/0006).
- **Auditoria financeira:** os eventos são imutáveis e re-reproduzíveis (replay) via
  Kafka (ADR-0003).

## Fora do escopo (Sprint 0)

Não há interface de usuário/web nem identidade humana (login/AD): o consumo é
máquina-a-máquina (`api_key`) e `GET /consolidado` (ADR-0007).
