<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# GLOSSARY — Fluxo de Caixa (CQRS)

> Vocabulário do domínio. Termos em ordem alfabética.

- **Anti-monólito distribuído** — regra que proíbe compartilhar biblioteca de código
  entre os serviços A e B; o único acoplamento em runtime é o contrato do evento
  (ADR-0015).
- **Consistência eventual** — o saldo consolidado reflete o lançamento com pequeno
  atraso após a publicação do evento; a escrita nunca é perdida (ADR-0003).
- **Consolidado (saldo)** — projeção de leitura desnormalizada, otimizada para
  `GET /consolidado` a 50 req/s (ADR-0004).
- **Contrato de evento** — forma do payload do tópico `lancamento.criado`, versionada
  por um campo `schemaVersion` obrigatório (ADR-0015).
- **CQRS** — segregação entre o modelo de escrita (Lançamentos) e o de leitura
  (Consolidado), desacoplados por eventos.
- **database-per-service** — cada serviço é dono do próprio schema; sem tabela/FK
  compartilhada entre A e B (ADR-0004/0013).
- **DLQ (Dead Letter Queue)** — tópico `lancamento.dlq` para eventos não processáveis
  após retries (ADR-0003/0014).
- **Evento `lancamento.criado`** — evento publicado quando um lançamento é criado
  (ADR-0003).
- **Fluxo de Caixa** — domínio financeiro da solução: registrar lançamentos e consultar
  saldo consolidado.
- **Idempotência** — garantia de que reprocessar um evento não duplica o efeito,
  via Inbox + unique constraint (ADR-0003/0006).
- **Inbox** — padrão no consumidor que deduplica reentregas com
  `INSERT ... ON CONFLICT DO NOTHING` (ADR-0006).
- **Lançamento** — registro financeiro de entrada/saída (escrita, serviço A).
- **MSK Serverless** — Kafka gerenciado da AWS, cobrança por uso (ADR-0003/0014).
- **Projeção (de leitura)** — tabela desnormalizada de saldo, reconstruível por replay
  do Kafka (ADR-0004).
- **schemaVersion** — campo obrigatório do payload que versiona o contrato do evento
  (ADR-0015).
- **Transactional Outbox** — padrão que grava lançamento + evento na mesma transação
  ACID; um worker publica no Kafka após o commit (ADR-0003/0006).
