---
status: accepted
date: 2026-11-05
decision-makers: Max
---
# 0006. Runtime, linguagem e acesso a dados do backend

## Contexto e problema

A stack de runtime do backend precisava ser definida em conjunto com a forma de acesso a dados, pois é essa camada que sustenta o Transactional Outbox/Inbox (ADR-0003/0004). As ADRs 0003–0005 e o `ROADMAP.md` assumiam **Java 21 (LTS) + Spring Boot 3** como default, mas essa premissa não estava formalizada — e havia a alternativa do ecossistema .NET, predominante na organização.

O runtime exige justificativa formal (versão **LTS vigente** e correção de **vulnerabilidades transitivas**) e, mais importante, o padrão Outbox/Inbox impõe dois requisitos concretos à escolha: **transação declarativa com hook pós-commit** e **controle explícito do SQL que implementa a atomicidade**.

## Histórico da discussão (sessão de iteração — Q&A)

> Sessão de perguntas e respostas entre o time e o agente, registrada como subsídio à decisão.

### Q1 — "A ADR-0003 menciona que o MSK Serverless preserva a semântica do Kafka. Explique isso."

**R:** MSK Serverless roda o **Apache Kafka real** por baixo e expõe o mesmo protocolo/API — não é um "Kafka-compatível". O termo "serverless" muda apenas a **operação** (gestão de brokers, escalonamento de partições/vazão) e a **cobrança** (por uso: partition-hour + storage + transfer), não a semântica. Permanecem intactos: log imutável e durável (append-only + retention), replay (voltar a qualquer offset), ordenação por chave (partição), consumer groups/offsets e produtores idempotentes/transações. Conclusão: escolher MSK Serverless não sacrifica o replay/retention/ordenação que motivaram o Kafka — só elimina a carga operacional e torna o custo variável (FinOps).

### Q2 — "Outbox/Inbox é o mesmo que publish/subscribe?"

**R:** Não — são **ortogonais**, respondem a problemas diferentes. **Pub/sub** é padrão de *entrega* (um produtor publica num tópico; N consumidores independentes recebem, desacoplados pelo broker). **Transactional Outbox** é padrão de *consistência* no produtor (evento e transação de banco gravados na mesma transação ACID). **Inbox** é padrão de *idempotência* no consumidor (deduplicação de reentregas). No ADR-0003, pub/sub = a topologia (tópico `lancamento.criado`), Outbox = borda do produtor, Inbox = borda do consumidor.

### Q3 — "'O ADR usa os três juntos, cada um numa borda' — como assim?"

**R:** Cada padrão opera num **ponto específico do fluxo**. Na borda A (Lançamentos → Kafka), o risco é "commit no banco sem publicação do evento" → resolve o **Outbox**. Na borda B (Kafka → Consolidado), o risco é "reentrega/duplicata" → resolve o **Inbox**. No meio, o transporte desacoplado → **pub/sub**. Os três cobrem o caminho completo: consistência na saída → entrega desacoplada → idempotência na entrada.

### Q4 — "O consumidor (Inbox) consulta o estado no banco ACID; e se 2 consumidores consultarem ao mesmo tempo e ambos lerem 'pendente'?"

**R:** O Inbox **não faz** "consultar → decidir → agir" (check-then-act), justamente porque isso tem a corrida TOCTOU descrita. A reivindicação da mensagem é uma **escrita atômica arbitrada pelo banco**, não uma leitura: (a) **unique constraint** no `message_id` + `INSERT ... ON CONFLICT DO NOTHING` — o INSERT é a "claim"; só um vence, o outro recebe *duplicate key* e pula; ou (b) **update condicional** `UPDATE ... SET status='processing' WHERE message_id=:id AND status='pending'` — o `rowcount` decide o vencedor. Detalhe do Kafka: dentro de um consumer group, uma partição tem **um único consumidor ativo** por vez; a duplicidade real vem de redelivery (retry/rebalance/crash), e a constraint cobre igualmente.

### Q5 — "Então, para ler a mensagem, o consumidor precisa atualizar para 'processing'... não existe consumir sem registrar a leitura. Certo?"

**R:** Quase — com uma distinção: **"ler" (poll do Kafka) não toca no banco**; o que exige escrita atômica é **"processar exatamente uma vez"**. E a claim precisa estar na **mesma transação** do efeito de negócio (claim + efeito commitam/rolam juntos — sem o estado "reivindiquei mas não processei"). O "update para `processing`" é uma variante para máquina de estados visível (processamento longo/lease com timeout); no caso simples do ADR-0003 basta o `INSERT ... ON CONFLICT` na mesma transação do efeito.

### Q6 — "O Java/Spring Boot resolve o controle de transações necessário ao Outbox/Inbox? Como?"

**R:** Sim. Spring tem transação **declarativa** (`@Transactional` + `PlatformTransactionManager`); todas as escritas dentro do método compartilham a mesma `Connection`/`EntityManager`, então negócio + outbox/inbox são atômicos. Publicação pós-commit via `TransactionSynchronization.afterCommit` ou `@TransactionalEventListener(AFTER_COMMIT)`. No consumidor, `@Transactional` + `INSERT ... ON CONFLICT`. **Limites importantes:** o Spring resolve a atomicidade *local* (mesmo banco); a corrida dos consumidores é arbitrada pelo **banco** (unique constraint), não pelo framework; e o dual-write DB↔Kafka continua exigindo outbox relay + idempotência (não XA). **Conclusão central:** o padrão Outbox/Inbox é **centrado no banco, não na linguagem** — .NET tem o equivalente direto (EF Core + `TransactionScope` / `IEnlistmentNotification` para o pós-commit). A linguagem é neutra para o padrão; o que muda é a *ergonomia* do suporte transacional e do ecossistema de mensageria.

## Decisão

Adotar **Java 21 (LTS) + Spring Boot 3**, com **SQL leve** (`JdbcTemplate` / `jOOQ`) para acesso a dados — **sem ORM** (Hibernate/JPA) no núcleo transacional.

### Runtime/linguagem — Java 21 + Spring Boot 3

- Transação declarativa nativa (`@Transactional` + `PlatformTransactionManager`) e hook pós-commit (`afterCommit` / `@TransactionalEventListener(AFTER_COMMIT)`), que sustentam o Transactional Outbox sem abstração extra.
- Ecossistema Kafka maduro (Spring Kafka + outbox/inbox consolidados).
- Java 21 LTS com suporte e atualização vigente.

Alternativa descartada: **.NET 8 / .NET 10 LTS** — tem o equivalente (`TransactionScope` + `IEnlistmentNotification`), mas o núcleo de consistência já estava modelado sobre as primitivas do Spring (ADRs 0003–0005) e o suporte transacional + pós-commit do Spring é o mais ergonômico para o Outbox/Inbox.

### Acesso a dados — SQL leve, sem ORM

- O núcleo crítico (outbox/inbox/projeção) depende de escritas SQL exatas (`INSERT ... ON CONFLICT DO NOTHING`, `UPDATE ... WHERE status='pending'`), que o SQL leve expõe e torna auditáveis — sem a "magic" do ORM (dirty-checking, cache de 1º nível, N+1).
- Menor overhead por operação e controle fino do SQL gerado (índices, upserts, batching).
- **Não** justificado por "alto volume": o requisito é 50 req/s (modesto — YAGNI). O driver é **controle e explicitude da atomicidade**, não throughput.

ORM (Hibernate/JPA) fica descartado para o núcleo transacional; pode ser usado em código auxiliar de CRUD se/onde a produtividade compensar (regra de três).

## Consequências

- Bom, porque a transação declarativa + pós-commit do Spring sustentam o Transactional Outbox sem abstração extra.
- Bom, porque o SQL leve dá controle e auditabilidade sobre a atomicidade do Outbox/Inbox.
- Bom, porque Java 21 LTS + Spring Boot 3 consolidam o stack já assumido pelas ADRs 0003–0005 (CI, testes, observabilidade).
- Ruim, porque se afasta do ecossistema .NET predominante na organização (dois stacks a manter, se houver outros serviços .NET).
- Ruim, porque o SQL leve aumenta o boilerplate (mapeamento manual, migrations) em relação ao ORM — mitigado pelo escopo reduzido do domínio.
- A decisão **não altera** a arquitetura CQRS + Outbox/Inbox (ADR-0003/0004): a consistência "exatamente uma vez" continua garantida pelo banco (unique constraint no Inbox), não pela linguagem/ORM.
