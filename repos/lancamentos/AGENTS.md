<!--
Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
All rights reserved.
PROPRIETARY AND CONFIDENTIAL.
No part of this software may be reproduced, distributed, or transmitted
in any form or by any means without express written permission.
-->

# AGENTS.md — Lançamentos (Microsserviço A · Escrita)

> Repo de produto (`family: filho`). Herda de `verx-architecture-challenge-dev`
> (projeto pai — raiz). Regras globais do motor: `<raiz>/AGENTS.md` (ralph-engine).

## Persona

- **persona_id:** `lancamentos-dev`
- **display_name:** Lançamentos (Escrita)
- **active_skills:** `clean-code`, `security-auditor`, `architect-review`, `code-reviewer`
- **activation_log_format:** `[Contexto: Lançamentos (Escrita) — Java 21 · Spring Boot 3 · Kafka · PostgreSQL]`
- **inheritance:** Completa de `verx-architecture-challenge-dev` (projeto pai)

## Copyright e Licença

- Todo arquivo de código-fonte criado/modificado deve iniciar com o header de
  copyright (`MAX JEISON PRASS TECNOLOGIA LTDA`, CNPJ `26.360.770/0001-39`).
- O `LICENSE` deste repo segue o **Modelo Filho** e contém a cláusula
  **"Powered by Backforend"**.
- Skill de governança de PI: `backforend-copyright`.

## Contrato CDC (`lancamento.criado`)

- Contrato em `src/test/resources/contracts/*.groovy` (Spring Cloud Contract, ADR-0015);
  `schemaVersion` é obrigatório e fixado em `1` (asserção literal).
- Classe-base do teste gerado: `com.verx.cashflow.lancamentos.contract.LancamentoCriadoBase`
  (referenciada por `spring-cloud-contract-maven-plugin` no `pom.xml`).
- Testes de contrato sobem o contexto com
  `spring.autoconfigure.exclude=...DataSourceAutoConfiguration,...KafkaAutoConfiguration`
  e publicam no `MessageVerifierSender` + canal em memória `lancamento.criado` — sem
  banco/broker reais.
- O canal em memória é registrado por
  `com.verx.cashflow.lancamentos.contract.LancamentoCriadoTestChannelConfig`
  (bean `lancamento.criado` = `PollableChannel` sobre `BlockingQueue`, listado no
  `classes` do `@SpringBootTest` da base) — sem `spring-integration-core` (YAGNI).
- O plugin gera o teste de contrato a partir de
  `src/test/resources/contracts/*.groovy`; o stub gerado não é consumido por B —
  B prova o mesmo contrato desserializando um fixture JSON próprio (ver
  `repos/consolidado/AGENTS.md` § Contrato CDC).

## Execução local (Docker)

- `Dockerfile` builda com `maven:3.9-eclipse-temurin-21` e roda em
  `eclipse-temurin:21-jre-alpine` como usuário não-root (`app`).
- O `./mvnw` depende de `.mvn/wrapper/maven-wrapper.properties` (não versionado);
  por isso o Dockerfile usa a imagem Maven oficial, não o wrapper.
- Health check do container consulta `GET /actuator/health`
  (`spring-boot-starter-actuator`, exposto em `application.yml`).
- Compose completo em `docker-compose.yml` da raiz (build context `./repos/lancamentos`).

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
