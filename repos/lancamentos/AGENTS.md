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

## Transação JDBI (Transactional Outbox)

- Atomicidade: grave `lancamento` + `outbox_event` na MESMA transação via método
  `default` anotado com `@Transaction` no `LancamentoRepository`
  (`gravarLancamentoEOutbox`) — nunca dois `@SqlUpdate` fora de transação. O relay
  (US-003) segue o mesmo padrão para claim (`FOR UPDATE SKIP LOCKED`) + update de
  status/`published_at`.
- O payload do evento (`lancamento.criado`) é serializado pelo `ObjectMapper` do Spring
  (ISO-8601) a partir de `LancamentoCriadoPayload`, que espelha o contrato CDC sem
  expor `criadoEm`.
- Teste CDC DB-free (`LancamentoCriadoBase`) exige um bean `LancamentoRepository`
  (mock em `LancamentoCriadoTestChannelConfig`) porque `CriarLancamentoService` o
  injeta — ao adicionar beans que dependam de repositório, forneça um stub no config
  de teste CDC. O `OutboxRelay` (US-003) exige o MESMO tratamento para
  `KafkaTemplate<String, String>` (mock no `LancamentoCriadoTestChannelConfig`), já
  que a base exclui `KafkaAutoConfiguration`.

## Outbox Relay (US-003)

- `OutboxRelay` (`infra/relay`) é um `@Component` com `@Scheduled` que publica os
  eventos `PENDING` do outbox no tópico `lancamento.criado` (key = `aggregate_id`,
  value = `payload` jsonb já serializado na escrita) — claim → publish → mark.
- Claim: `LancamentoRepository.claimPending(batch)` combina `@SqlQuery` (SELECT
  `FOR UPDATE SKIP LOCKED`) + `@RegisterRowMapper` + `@Transaction` num único método
  SQL Object; `payload::text AS payload` devolve o JSON do contrato CDC. O
  `OutboxEventMapper` (RowMapper aninhado) mapeia as colunas para o record
  `OutboxEvent` sem ORM.
- At-least-once: sucesso → `markPublished` (`PUBLISHED` + `published_at=now()`);
  falha/`ExecutionException`/`TimeoutException` → `markFailed` (`tentativas+1`,
  mantém `PENDING`). O `KafkaTemplate.send(...).get(5, SECONDS)` bloqueia para
  surfacer a falha assíncrona no mesmo tick.
- `@EnableScheduling` fica em `LancamentosApplication`; `@Scheduled` usa
  `initialDelayString` (default 5000ms) para o relay não disparar em contextos de
  teste curtos (CDC).
- O IT (`OutboxRelayIT`) usa Testcontainers PostgreSQL (`disabledWithoutDocker=true`)
  + `MockProducer` de `kafka-clients` (transitiva do spring-kafka) como "broker de
  teste" — SEM nova dependência (`spring-kafka-test`/`testcontainers-kafka`), o que
  evita `manifest_drift` na imagem BYOI. Envolva o `MockProducer` num
  `ProducerFactory` anônimo (só `createProducer`/`getConfigurationProperties`/
  `transactionCapable`) para construir o `KafkaTemplate`.
- Nunca logue o `payload` do outbox (contém valor/descrição do lançamento — LGPD);
  logue apenas `evento.id()` e `evento.topic()`.

## Execução local (Docker)

- `Dockerfile` builda com `maven:3.9-eclipse-temurin-21` e roda em
  `eclipse-temurin:21-jre-alpine` como usuário não-root (`app`).
- O `./mvnw` depende de `.mvn/wrapper/maven-wrapper.properties` (não versionado);
  por isso o Dockerfile usa a imagem Maven oficial, não o wrapper.
- Health check do container consulta `GET /actuator/health`
  (`spring-boot-starter-actuator`, exposto em `application.yml`).
- Compose completo em `docker-compose.yml` da raiz (build context `./repos/lancamentos`).

## Testes — Surefire / JUnit Platform (cache offline BYOI)

- A versão do surefire (3.5.4) e o provider JUnit Platform são **geridos pelo
  `spring-boot-starter-parent` 3.4.13** — o `pom.xml` NÃO declara
  `maven-surefire-plugin` nem `surefire-junit-platform`.
- **Anti-pattern (não repetir):** declarar `surefire-junit-platform` como
  `<dependency>` do `maven-surefire-plugin` força `junit-platform-launcher:1.12.1`
  na classpath e conflita com o JUnit 5.11.4 do BOM (`junit-platform-commons/engine:1.11.4`),
  falhando `mvn test` com "conflicting versions detected". O provider é
  auto-detectado pelo surefire e o launcher é resolvido na versão **1.11.4**
  (alinhada ao JUnit 5.11.4 do BOM).
- O cache offline BYOI (`.ralph/images/Dockerfile.test`) deve pré-aquecer AMBOS:
  `surefire-junit-platform:3.5.4` (provider, resolvido no runtime do `test`) e
  `junit-platform-launcher:1.11.4` (launcher alinhado). `go-offline`/`resolve-plugins`
  NÃO pré-aquecem esses artefatos.
- `junit-platform-launcher` está declarado como **dependência de teste explícita no
  `pom.xml`** (sem `<version>` → 1.11.4 do BOM): isso faz o `dependency:resolve
  -DincludeScope=test` do Dockerfile.test pré-aquecer o launcher — o `dependency:get
  junit-platform-launcher:1.11.4` no Dockerfile.test torna-se opcional. O provider
  `surefire-junit-platform:3.5.4` continua exigindo `dependency:get` (não é dep do
  projeto).

## Logs estruturados e redação LGPD (ADR-0011)

- `src/main/resources/logback-spring.xml` publica JSON no console
  (`LoggingEventCompositeJsonEncoder`): `@timestamp`, `severity`, `logger`, `thread`,
  `message`, `mdc` e `stackTrace` — o driver `awslogs` leva o mesmo JSON ao CloudWatch.
- O conversion word `%maskedMsg` (`SensitiveDataMessageConverter`) aplica
  `SensitiveDataMasker` à mensagem: `Authorization`, `x-api-key`, `Cookie`/`set-cookie`,
  `Bearer`/`Basic` e pares `token`/`password`/`secret` saem como `[REDACTED]`
  (LGPD §2.1). Testes de regressão: `SensitiveDataMaskerTest` (padrões de
  credencial) e `SensitiveDataMessageConverterTest` (conversion word aplica o
  masker à mensagem formatada, preservando mensagens sem credenciais).
- `CorrelationIdFilter` injeta `correlationId` no MDC (header `X-Correlation-Id`
  sanitizado) e loga apenas método/path/status/duração — nunca headers ou body.
  Teste de regressão: `CorrelationIdFilterTest` (MDC no `finally`, UUID para header
  inválido/ausente, response header e linha de acesso sem credenciais/query string).
- Formatos novos de credencial entram em `SensitiveDataMasker` e DEVEM ser cobertos
  por `SensitiveDataMaskerTest`.
- Nunca use `System.out`/`System.err`/`print()` em código de produto: use SLF4J.
- Health check exposto em `GET /actuator/health` (consumido pelo `HEALTHCHECK` do Dockerfile).
