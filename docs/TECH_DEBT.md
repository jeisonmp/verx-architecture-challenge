<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# TECH_DEBT — Fluxo de Caixa (CQRS)

> Débitos técnicos e pendências arquiteturais. Sprint 0 (arquitetura) — ainda sem
> código de produto; dívidas de código serão registradas a partir da Sprint 1.

## Pendências arquiteturais (decisões adiadas)

| Pendência | O que foi adiado | Reabrir quando | ADR |
|---|---|---|---|
| Schema Registry (AVRO/Glue) | versionamento de contrato via `schemaVersion` manual + contract testing | ecossistema de eventos crescer (muitos eventos/consumidores) ou a evolução de contrato ficar frequente | 0015 |
| Enforcement de contrato (consumer-driven contracts / Spring Cloud Contract) | testes de contrato ficaram para o Sprint 1 | implementação dos serviços A/B | 0015 |
| Separação física do banco | database-per-service em dev é lógico (schemas), não físico | exigir isolamento de falha/escala em produção | 0013 |
| DAST (OWASP ZAP) | fora do CI por ora (sem ambiente em execução) | existir ambiente efêmero de execução | 0005 |
| e2e e load test automatizados | manuais no `README.md` (ambiente 100% local) | houver deploy/registry | 0005 |

## Sem dívida de código (Sprint 0)

Nenhum arquivo de código-fonte de produto existe ainda (apenas `workspace.dsl`,
`repos/infra/*.tf` e documentação). Dívidas de código (complexidade, acoplamento,
duplicação) serão registradas aqui a partir da Sprint 1.
