---
status: accepted
date: 2026-11-05
decision-makers: Max
---
# 0008. Vert.x vs Spring Boot — o que muda no contexto Outbox/Inbox

## Contexto e problema

A ADR-0006 aprovou **Java 21 + Spring Boot 3 + SQL leve**. Esta ADR reabre a decisão de runtime para avaliar **Vert.x** como alternativa, com foco no que muda no núcleo de consistência (Outbox/Inbox) e sob carga.

A diferença central é o **modelo de concorrência**: o Spring Boot (arquitetura tradicional) usa **thread-per-request** (bloqueante), enquanto o Vert.x usa um modelo **assíncrono/reativo baseado em Event Loop** (não-bloqueante).

> Pela regra do ADR-0001, um ADR aceito não é editado: se esta discussão concluir por Vert.x, um novo ADR substituirá a ADR-0006.

## Alternativas consideradas

* **Spring Boot + SQL leve** (ADR-0006) — mantida.
* **Vert.x + reactive SQL client** — descartada.

## Histórico da discussão (sessão de iteração — Q&A)

### 1. Controle cirúrgico de concorrência e conexões

- **Spring Boot:** num pico de transações (ex.: milhares de PIX por segundo), cada requisição segura uma thread do servidor e uma conexão do pool de banco (HikariCP) enquanto aguarda o processamento do SQL da tabela de Contas e da tabela Outbox. Se o banco oscilar, as threads acumulam e podem exaurir a memória do microsserviço.
- **Vert.x:** usa drivers de banco reativos/nativos (ex.: `vertx-pg-client`). Uma única thread do Event Loop gerencia milhares de transações simultâneas — o Vert.x não "espera" o banco travando a thread: envia o comando SQL (Conta + Outbox) de forma assíncrona e libera a thread para processar o próximo PIX imediatamente. Quando o banco responde, um callback/reactive stream finaliza a transação.

### 2. Facilidade de auditoria e implementação do Inbox/Outbox

- **Spring Boot:** auditoria e Outbox dependem fortemente do gerenciador de transações (`@Transactional`). Para auditar mesmo quando a transação principal falha (ou vice-versa), é preciso manipular contextos transacionais (`Propagation.REQUIRES_NEW`), o que pode complicar e gerar deadlocks ocultos sob alta concorrência.
- **Vert.x (Event Bus nativo):** o Event Bus interno (in-memory, alto desempenho) torna o Outbox/Inbox elegante:
  1. A transação reativa altera o saldo e grava o evento na tabela `OutboxMessage` num único batch atômico e assíncrono.
  2. Assim que o commit reativo é confirmado, a aplicação dispara uma mensagem no Event Bus local.
  3. Um **Verticle** escuta esse barramento e despacha a mensagem para Kafka/RabbitMQ sem bloquear nenhuma thread de API.
  4. A auditoria pode disparar eventos de log em paralelo sem adicionar latência à jornada principal.

### 3. Facilidade de uso e curva de aprendizado

- **Spring Boot:** vence massivamente em ecossistema e facilidade — o modelo mental linear (imperativo) é muito mais fácil de codificar, testar e auditar por desenvolvedores tradicionais.
- **Vert.x:** exige domínio de programação reativa (Futures/Promises ou RxJava/Mutiny). Rastrear o fluxo para auditoria (Distributed Tracing / Correlation IDs) exige ferramentas específicas (ex.: OpenTelemetry integrado), pois o ID da transação não fica mais preso a uma única thread (ThreadLocal).

### Pontos a aprofundar (contrapontos)

- **Tensão com o requisito de volume:** a ADR-0004 define 50 req/s — modesto. O argumento central do Vert.x (item 1) assume "milhares de PIX/s", um cenário de escala ainda não exigido. Decidir por Vert.x por throughput violaria o YAGNI, salvo se houver requisito de pico não documentado.
- **Atomicidade continua no banco:** o Outbox/Inbox segue dependendo de transação ACID + unique constraint — o modelo reativo muda a *entrega* (não-bloqueante), não a garantia de "exatamente uma vez".
- **Event Bus é interno ao processo:** o Event Bus do Vert.x não substitui o Kafka (ADR-0003); seria uma ponte local outbox → broker. Vale comparar com o `afterCommit` do Spring (ADR-0006), que resolve o mesmo problema com menos peças móveis.
- **Operação e observabilidade:** custo de operar um runtime reativo (tracing, debugging, backpressure) vs. o ecossistema consolidado do Spring.

## Critérios de decisão

| Critério | Spring Boot (ADR-0006) | Vert.x |
|---|---|---|
| Concorrência sob pico | thread-per-request (pool limitado) | Event Loop (não-bloqueante) |
| Atomicidade Outbox/Inbox | `@Transactional` + `afterCommit` | transação reativa + Event Bus |
| Curva / ecossistema | maduro, imperativo | reativo, mais íngreme |
| Observabilidade | tracing convencional | OpenTelemetry obrigatório |
| Alinhamento com o requisito (50 req/s) | suficiente | além do necessário (YAGNI?) |

## Decisão

Manter **Spring Boot + SQL leve (JDBI / Spring Data JDBC)**. **Vert.x + reactive SQL client é descartado** para este domínio.

- A arquitetura **não tem alto volume transacional** e **não é um core bancário**: o requisito é 50 req/s (ADR-0004). A concorrência não-bloqueante do Vert.x resolve um problema de escala que não existe aqui.
- Vert.x + reactive SQL client só se justificaria para **volumes massivos de transações por segundo em nível core banking global** com o menor custo de infra possível — aceitando o trade-off de **curva de aprendizado maior** e **maior complexidade no rastreio de logs de auditoria** (fluxo assíncrono).
- A **escalabilidade horizontal** continua atendida com Spring Boot (replicar instâncias + partições Kafka), sem adotar o modelo reativo.
- Mantemos Spring Boot pela **manutenção simples** e **depuração fácil** (stack traces lineares).

SQL leve é definido como **combo JDBI + Spring Data JDBC** (ver refinamento abaixo), refinando a ADR-0006 (que citava `JdbcTemplate` / `jOOQ`).

## Refinamento — acesso a dados (combo JDBI + Spring Data JDBC)

A ADR-0006 citava `JdbcTemplate` / `jOOQ` como SQL leve. A discussão subsequente comparou as opções SQL-first:

| | JdbcTemplate | JDBI | Spring Data JDBC |
|---|---|---|---|
| Natureza | wrapper JDBC do Spring | lib SQL-first de terceiros | repositório orientado a agregado (leve) |
| Você escreve | SQL + `RowMapper` manual | SQL + anotações declarativas | entidade + repositório (`@Query`) |
| Mapeamento objeto↔tabela | manual | semiautomático | automático por convenção |
| Abstração | mínima | mínima–média | média (sem lazy/dirty-checking) |

**Decisão (combo):** adotar **JDBI + Spring Data JDBC**, cada um no seu território:

- **JDBI** no núcleo transacional crítico (outbox/inbox/projeção), onde o SQL exato precisa ser explícito e auditável — `INSERT ... ON CONFLICT DO NOTHING`, `UPDATE ... WHERE status='pending'`.
- **Spring Data JDBC** nos repositórios de agregado (lançamento, projeção de saldo), delegando o CRUD simples e mantendo os SQL delicados via `@Query`.

Justificativa: o JDBI entrega o controle cirúrgico que o Outbox/Inbox exige (parâmetros nomeados e menos boilerplate de mapeamento que o `JdbcTemplate`); o Spring Data JDBC elimina o código repetitivo de CRUD sem o peso do JPA (sem lazy loading, sem dirty-checking). Juntos cobrem "controle onde importa, produtividade onde não".

Descartados: `jOOQ` (camada de DSL de tipos sem ganho neste escopo — YAGNI) e `JdbcTemplate` (substituído pelo JDBI, de API mais ergonômica).

## Consequências

- A ADR-0006 permanece válida e é **reforçada** (Spring Boot + SQL leve); esta ADR confirma a decisão e refina a biblioteca de acesso a dados.
- **Vert.x fica descartado** e não é adotado.
- A escalabilidade horizontal é obtida por replicação de instâncias + particionamento Kafka (ADR-0003), não por modelo reativo.
- Se no futuro houver requisito de volume massivo (core banking global), a decisão é reaberta em um novo ADR — esta ADR não é editada (regra do ADR-0001).
