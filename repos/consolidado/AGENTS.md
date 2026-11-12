<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# AGENTS.md — Consolidado (Microsserviço B · Leitura)

> Repo de produto (`family: filho`). Herda de `verx-architecture-challenge-dev`
> (projeto pai — raiz). Regras globais do motor: `<raiz>/AGENTS.md` (ralph-engine).

## Persona

- **persona_id:** `consolidado-dev`
- **display_name:** Consolidado (Leitura)
- **active_skills:** `clean-code`, `security-auditor`, `architect-review`, `code-reviewer`
- **activation_log_format:** `[Contexto: Consolidado (Leitura) — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]`
- **inheritance:** Completa de `verx-architecture-challenge-dev` (projeto pai)

## Copyright e Licença

- Todo arquivo de código-fonte criado/modificado deve iniciar com o header de
  copyright (`MAX JEISON PRASS TECNOLOGIA LTDA`, CNPJ `26.360.770/0001-39`).
- O `LICENSE` deste repo segue o **Modelo Filho** e contém a cláusula
  **"Powered by Backforend"**.
- Skill de governança de PI: `backforend-copyright`.

## Contrato CDC (`lancamento.criado`)

- B prova o contrato de A por um teste unitário próprio
  (`com.verx.cashflow.consolidado.contract.LancamentoCriadoConsumerTest`) que
  desserializa o fixture JSON `src/test/resources/contracts/lancamento-criado.json`
  (espelho do corpo de `lancamento-criado.groovy` de A) — sem Stub Runner, sem
  `@SpringBootTest` e sem depender de A em execução (ADR-0015).
- O fixture reflete o contrato de A (`schemaVersion`, `eventId`, `eventType`,
  `occurredAt`, `lancamento{id,tipo,valor,data,descricao}`).
- O DTO do evento é cópia própria (test-scoped) e tolerant reader
  (`@JsonIgnoreProperties(ignoreUnknown = true)`) — nunca importar classes de A.
- O teste assere `schemaVersion == 1`, `eventType == "lancamento.criado"` e a
  presença de `lancamento.id/tipo/valor`; os campos não consumidos são ignorados.

## Execução local (Docker)

- `Dockerfile` builda com `maven:3.9.9-eclipse-temurin-21` e roda em
  `eclipse-temurin:21-jre-alpine` como usuário não-root (`app`).
- O `./mvnw` depende de `.mvn/wrapper/maven-wrapper.properties` (não versionado);
  por isso o Dockerfile usa a imagem Maven oficial, não o wrapper.
- Health check do container consulta `GET /actuator/health`
  (`spring-boot-starter-actuator`, exposto em `application.yml`).
- Compose completo em `docker-compose.yml` da raiz (build context `./repos/consolidado`).

## Logs estruturados e redação LGPD (ADR-0011)

- `src/main/resources/logback-spring.xml` publica JSON no console
  (`LoggingEventCompositeJsonEncoder`): `@timestamp`, `severity`, `logger`, `thread`,
  `message`, `mdc` e `stackTrace` — o driver `awslogs` leva o mesmo JSON ao CloudWatch.
- O conversion word `%maskedMsg` (`SensitiveDataMessageConverter`) aplica
  `SensitiveDataMasker` à mensagem: `Authorization`, `x-api-key`, `Cookie`/`set-cookie`,
  `Bearer`/`Basic` e pares `token`/`password`/`secret` saem como `[REDACTED]`
  (LGPD §2.1). Teste de regressão: `SensitiveDataMaskerTest`.
- `CorrelationIdFilter` injeta `correlationId` no MDC (header `X-Correlation-Id`
  sanitizado) e loga apenas método/path/status/duração — nunca headers ou body.
- Formatos novos de credencial entram em `SensitiveDataMasker` e DEVEM ser cobertos
  por `SensitiveDataMaskerTest`.
- Nunca use `System.out`/`System.err`/`print()` em código de produto: use SLF4J.
- Health check exposto em `GET /actuator/health` (consumido pelo `HEALTHCHECK` do Dockerfile).
