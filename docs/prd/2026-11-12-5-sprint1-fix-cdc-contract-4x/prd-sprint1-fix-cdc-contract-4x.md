---
project: raiz
---

# PRD: Sprint 1 — Correção do CDC no Spring Cloud Contract 4.x (build + runtime)

**Type:** bugfix
**Projeto afetado:** raiz — cross-repo (`repos/lancamentos`, `repos/consolidado`, `.github/workflows/quality.yml`).

## 1. Introduction/Overview

Os testes CDC do evento `lancamento.criado` (ADR-0015) foram escritos contra a API do
**Spring Cloud Contract 2.x/3.x**, mas o projeto pina `spring-cloud.version=2024.0.1`
→ **Spring Cloud Contract 4.2.x**. Nessa versão:

- a interface `MessageVerifier` **não existe mais** — foi desmembrada em
  `MessageVerifierSender` (envio) e `MessageVerifierReceiver` (recebimento);
- o stub messaging do consumidor (`StubTrigger` + Kafka/AMQP embutido) **foi removido**
  no Contract 4.0 — ver [Migration Guide](https://github.com/spring-cloud/spring-cloud-contract/wiki/Spring-Cloud-Contract-4.0-Migration-Guide);
- o `consolidado` não declarava o artefato `spring-cloud-contract-verifier`, então o
  `spring-cloud-starter-contract-stub-runner` sozinho não fornecia os tipos de messaging.

Consequência: `mvn -DskipTests package` falhava em **compile** (símbolo `MessageVerifier`
não encontrado) — o erro do `docker compose up --build`.

**Correção de compilação já aplicada (working tree, não commitada):**

- `repos/consolidado/pom.xml` — dependência de teste `spring-cloud-contract-verifier`.
- `repos/lancamentos/.../LancamentoCriadoBase.java` — `MessageVerifier` → `MessageVerifierSender`.
- `repos/consolidado/.../LancamentoCriadoConsumerTest.java` — `MessageVerifier` → `MessageVerifierReceiver`.

A compilação volta a passar, mas o **runtime** dos testes CDC continua quebrado no 4.x.
Esta PRD cobre o restante para deixar o CDC **verde de fato** (`mvn install`/`verify` e o
CI `quality.yml`), conforme as decisões:

- **Produtor (A):** canal em memória — sem broker real no Sprint 1.
- **Consumidor (B):** teste de desserialização *tolerant reader* — sem Stub Runner messaging.
- **CI + docs:** atualizar `quality.yml` e os `AGENTS.md` que citam o `MessageVerifier` antigo.

## 2. Goals

- `mvn -B -f repos/lancamentos verify` passa, com o teste de contrato gerado (`LancamentoCriadoTest`) executando e validando o payload de `lancamento.criado` contra um canal em memória (sem Kafka).
- `mvn -B -f repos/consolidado verify` passa, com um teste que prova que B desserializa o evento de A como *tolerant reader* (`schemaVersion` + campos que consome, ignorando o resto).
- `quality.yml` (dev → master) volta a refletir o fluxo real (B não consome mais o stub de A via Stub Runner LOCAL).
- Documentação (`AGENTS.md` de `lancamentos` e `consolidado`) atualizada para a API do Contract 4.x.
- Gate JaCoCo ≥ 80% preservado nos dois serviços (meta do núcleo outbox/inbox permanece deferida).

## 3. User Stories

### US-001: Tornar o teste CDC do produtor verde no 4.x (canal em memória)

**required_intelligence:** "pro"

**Description:** As a developer, I want the generated producer contract test to verify A's
`lancamento.criado` contract against an in-memory channel, so that ADR-0015 is enforced in
Sprint 1 without a real Kafka broker.

**Acceptance Criteria:**

- [x] `LancamentoCriadoBase` usa `MessageVerifierSender<Message<?>>` (migração já aplicada) e `send(message, "lancamento.criado")` continua roteando para o verifier.
- [x] Um canal em memória (`MessageChannel` + `PollableChannel`) nomeado `lancamento.criado` é registrado no contexto de teste do produtor, de modo que `SpringIntegrationStubMessages` (auto-config do `@AutoConfigureMessageVerifier`) consiga dar `send` e `receive` no destino `lancamento.criado`.
- [x] O canal é um helper de teste próprio (ex.: `PollableChannel` sobre `BlockingQueue`) **sem** adicionar `spring-integration-core` como dependência (YAGNI); usar `QueueChannel` de `spring-integration-core` apenas se o time preferir.
- [x] `mvn -B -f repos/lancamentos test` executa e **passa** o teste gerado `LancamentoCriadoTest` (contrato `lancamento-criado.groovy`).
- [x] `mvn -B -f repos/lancamentos verify` passa com o gate JaCoCo ≥ 80%.
- [x] Header de copyright presente em qualquer arquivo de teste novo.

### US-002: Reescrever o teste CDC do consumidor como desserialização tolerant reader

**required_intelligence:** "pro"

**Description:** As a developer, I want B to prove it deserializes A's `lancamento.criado`
event using its own tolerant-reader DTO, so the ADR-0015 contract remains enforceable in
Sprint 1 now that the stubbed Kafka Stub Runner was removed in Contract 4.0.

**Acceptance Criteria:**

- [x] `LancamentoCriadoConsumerTest` deixa de usar `@AutoConfigureStubRunner`, `StubTrigger`, `@AutoConfigureMessageVerifier` e `MessageVerifierReceiver`.
- [x] Remover do `repos/consolidado/pom.xml` as dependências de teste `spring-cloud-starter-contract-stub-runner` e `spring-cloud-contract-verifier` (não usadas após a reescrita).
- [x] Um fixture JSON (recurso em `src/test/resources/`) representa o corpo do contrato `lancamento.criado` de A, com todos os campos do payload (`schemaVersion`, `eventId`, `eventType`, `occurredAt`, `lancamento{id,tipo,valor,data,descricao}`), refletindo `repos/lancamentos/src/test/resources/contracts/lancamento-criado.groovy`.
- [x] O teste desserializa o fixture com `LancamentoCriadoEvent` (DTO próprio de B) e assere `schemaVersion()==1`, `eventType()=="lancamento.criado"` e `lancamento().id()/tipo()/valor()` presentes.
- [x] Tolerant reader: o fixture contém campos que B não consome (ex.: `eventId`, `occurredAt`, `data`, `descricao`) e o DTO os ignora via `@JsonIgnoreProperties(ignoreUnknown = true)` (anotação já presente no DTO).
- [x] O teste é um unit test simples (não precisa de `@SpringBootTest`/contexto Spring) — usa `ObjectMapper` local e o fixture como recurso.
- [x] `mvn -B -f repos/consolidado verify` passa com o gate JaCoCo ≥ 80%.
- [x] Header de copyright presente em qualquer arquivo de teste novo.

### US-003: Atualizar `quality.yml` e a documentação (AGENTS.md) para o fluxo CDC 4.x

**required_intelligence:** "flash"

**Description:** As a developer, I want CI and docs to reflect the new CDC flow, so that no
artifact still references the removed `MessageVerifier`/Stub Runner messaging.

**Acceptance Criteria:**

- [x] `.github/workflows/quality.yml`: remover a justificativa "B consome o stub CDC de A via Stub Runner LOCAL" e o passo "instala stub CDC para B"; A passa a rodar `mvn -B verify` (não mais `install` para publicar stub) e B roda `mvn -B verify` sem depender do `~/.m2` de A.
- [x] Os comentários do job (sequencial A→B) são atualizados para refletir que B não consome mais o stub de A via Stub Runner (a sequência pode ser mantida por simplicidade ou convertida em matrix paralela — registrar a escolha).
- [x] `repos/lancamentos/AGENTS.md` (§ Contrato CDC): trocar a menção a "`MessageVerifier` em memória" por "`MessageVerifierSender` + canal em memória `lancamento.criado`".
- [x] `repos/consolidado/AGENTS.md` (§ Contrato CDC): substituir a descrição de consumo via Stub Runner pela descrição do teste de desserialização *tolerant reader*.
- [x] Nenhuma referência restante a `MessageVerifier` (interface removida) em código ou docs (`grep -rn "MessageVerifier" repos docs .github` retorna apenas `MessageVerifierSender`/`MessageVerifierReceiver`).

## 4. Functional Requirements

- FR-1: O produtor (A) deve ter, no contexto de teste, um canal em memória nomeado `lancamento.criado` que implemente envio e recebimento síncrono de `Message<?>`.
- FR-2: O teste de contrato gerado para A deve validar o `outputMessage` de `lancamento.criado` (body + header `contentType`) contra o canal em memória, sem Kafka.
- FR-3: O consumidor (B) deve validar a desserialização do evento com o próprio DTO, ignorando campos desconhecidos (`@JsonIgnoreProperties(ignoreUnknown = true)`).
- FR-4: B deve rejeitar/tratar `schemaVersion` obrigatório — no Sprint 1, o teste valida que o campo é lido como `1`; a lógica de rejeição em runtime (DLQ) permanece do núcleo Inbox (Sprint 2/3).
- FR-5: `quality.yml` deve executar A e B com `mvn -B verify`, sem passos de publicação/consumo de stub CDC.

## 5. Non-Goals

- **Não** adotar Testcontainers ou broker real nos testes CDC no Sprint 1 (o Outbox/Inbox reais entram no Sprint 2/3; nesta ocasião, reavaliar o teste do consumidor com listener real).
- **Não** reintroduzir o `StubTrigger`/Stub Runner de messaging (feature removida no 4.0).
- **Não** adotar Schema Registry (AVRO/Glue) — permanece opção futura (ADR-0015).
- **Não** alterar o formato/payload do evento `lancamento.criado` nem o `schemaVersion=1` do contrato.
- **Não** mudar a meta de cobertura (≥ 80%; núcleo outbox/inbox deferido — PRD §9).

## 6. Technical Considerations

- **Risco de drift do contrato (consumidor):** com o teste de desserialização, B não consome mais o stub gerado por A; o fixture JSON deve ser mantido em paridade com `lancamento-criado.groovy`. Mitigação: comentário no fixture apontando para o contrato de A + reavaliação no Sprint 2/3 (listener real + Stub Runner sobre Kafka).
- **Canal em memória (produtor):** `SpringIntegrationStubMessages.send/receive` resolve o bean pelo nome do destino (`context.getBean("lancamento.criado", ...)`). O helper deve implementar `PollableChannel` (que estende `MessageChannel`); a auto-config usa `@ConditionalOnMissingBean`, então o bean do teste prevalece sobre o `NoOp`.
- **Sem dependência nova no produtor:** implementar o canal com `java.util.concurrent.BlockingQueue` (YAGNI) em vez de puxar `spring-integration-core`; alternativa aceita: `org.springframework.integration.channel.QueueChannel`.
- **Consumidor:** o teste de desserialização dispensa `@SpringBootTest` (DTO é record puro + Jackson); reduz o tempo de build do `consolidado` no CI.
- **CI:** A e B continuam rodando com PostgreSQL + Kafka como service containers (`quality.yml` já os provê) para os demais testes de integração; só o teste CDC deixa de depender de broker.

## 7. Success Metrics

- `docker compose up --build` conclui sem erro de compile (já corrigido).
- `quality.yml` fica verde em PR `dev → master` (hoje vermelho por compile + runtime do CDC).
- 0 referências à interface `MessageVerifier` (removida) no código e na documentação.
- Cobertura JaCoCo ≥ 80% mantida nos dois serviços.

## 8. Open Questions

- Manter `quality.yml` sequencial A→B (simples) ou converter para matrix paralela agora que B não depende do stub de A?
- No teste do consumidor, vale manter um teste negativo explícito (fixture sem `schemaVersion` → erro) já no Sprint 1, ou isso pertence ao núcleo Inbox do Sprint 2/3?
- Preferência por `QueueChannel` (`spring-integration-core`) vs. helper próprio sobre `BlockingQueue` no produtor?
