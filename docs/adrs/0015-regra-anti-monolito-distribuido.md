---
status: accepted
date: 2026-11-10
decision-makers: Max
---
# 0015. Regra anti-monólito distribuído — sem libs compartilhadas entre os serviços A e B

## Contexto e problema

O Sprint 0 congelou a stack (ADR-0003 a ADR-0014), mas a última pendência — a regra que
impede os microsserviços A (Lançamentos) e B (Consolidado) de se tornarem um **monólito
distribuído** — ainda não estava registrada como decisão própria. O risco concreto: se A e B
passarem a compartilhar uma biblioteca (jar de domínio/DTO comum), a evolução de um serviço
passa a arrastar o outro, e a independência de deploy que o CQRS persegue se perde. A regra
aparece hoje apenas como diretriz interna de anti-padrão e como citação em ADR-0004/0013 —
precisa virar ADR para fechar o Sprint 0.

## Alternativas consideradas

* **Biblioteca compartilhada A↔B** (`common`/`shared`/`domain-events`) — rejeitada: acopla
  build, versionamento e deploy dos dois serviços.
* **Sem lib compartilhada; contrato só via evento Kafka** — escolhida.
* **Schema Registry (AVRO/Glue) para versionar o contrato** — adiada (YAGNI): o volume de
  evolução de contrato ainda não justifica infra/registro extra no Sprint 0.

## Histórico da discussão (sessão de iteração — Q&A)

> Sessão de perguntas e respostas entre o time e o agente (2026-11-10), registrada como subsídio à decisão.

### Q1 — "O que garante que uma atualização em A não quebra o contrato com B?"

**R:** A regra escrita é política, não garantia — o enforcement precisa ser executável, em camadas:
(1) **CDC — Consumer-Driven Contracts** executado no CI de A (B define o contrato, A prova que o
satisfaz); (2) **tolerant reader** no consumidor (ignora campos desconhecidos, campo opcional → null);
(3) roteamento por `schemaVersion` (quebra real vira migração com janela, não quebra silenciosa);
(4) DLQ + observabilidade como rede de segurança. Caveat: schema ≠ semântica — compatibilidade
estrutural não captura mudança de significado.

### Q2 — "CDC não gera acoplamento?"

**R:** Gera, mas de tipo benigno. A e B já estão acoplados pelo schema do evento (ADR-0003); o CDC
torna esse acoplamento **explícito e testável**, não o cria. O stub de contrato é **test-only**
(metadata, não código compilado): A nunca linka classes de B em runtime, e A pina a versão do stub e
atualiza quando quer — sem lockstep de deploy. O acoplamento nocivo só surgiria se B contratasse campos
que não usa (mitigação: *contract minimalism*) ou se o contrato virasse gate até para mudança aditiva
(mitigação: matchers soltos + backward-compat).

### Q3 — "O que é Schema Registry (AVRO/Glue)?"

**R:** Serviço central que guarda, versiona e valida schemas, impondo compatibilidade na escrita. No
nosso stack a opção nativa seria o **Glue Schema Registry** (suporta JSON Schema, sem trocar para AVRO
binário). Adiado por YAGNI (2 serviços, 1 evento, 1 tópico): `schemaVersion` + CDC já cobrem
versionamento e compatibilidade sem infra nova. Permanece opção **aberta**, não rejeitada.

### Q4 — "O 'único ponto de acoplamento' continua correto depois do CDC?"

**R:** Sim, mas **em runtime** — a frase foi refinada para qualificar "em runtime". O evento Kafka é o
único acoplamento de runtime (sem lib, sem DB compartilhado, sem chamada síncrona). O stub de CDC
(Sprint 1) adiciona um acoplamento de **build/teste** (test-only, não deployado), mas é a *mesma*
interface tornada executável, não um acoplamento novo — por isso não viola a regra anti-monólito.

## Critérios de decisão

| Critério | Lib compartilhada | Contrato via Kafka (escolhida) | Schema Registry |
|---|---|---|---|
| Independência de deploy | Baixa (lockstep) | Alta | Alta |
| Acoplamento de runtime | Alto (jar linkado) | Nenhum | Nenhum (schema é metadata) |
| Garantia de compatibilidade | Fraca (build) | Forte (CDC no CI) | Mais forte (runtime + CI) |
| Infra/custo extra | Nenhum | Nenhum | +1 serviço AWS (Glue) + serde |
| Duplicação de contrato | Nenhuma | Pequena (aceitável) | Nenhuma (central) |
| Escala atual (2 serviços, 1 evento) | Ruim | Boa (YAGNI) | Over-provisioning |

## Decisão

Adotar a **regra anti-monólito distribuído**: os serviços A e B **não compartilham biblioteca
de código** (nenhum jar `common`, `shared`, `core` ou `domain-events`). O **único** ponto de
acoplamento **em runtime** entre eles é o **contrato do evento** do tópico `lancamento.criado`
(ADR-0003), que é o payload versionado por um campo `schemaVersion`. Em build/teste, esse mesmo
contrato é exercitado via stub de CDC (Sprint 1) — a mesma interface tornada executável, não um
acoplamento novo.

Regras decorrentes:

* **Comunicação A→B só via Kafka** (ADR-0003) — sem chamada síncrona HTTP entre serviços.
* **Dados: sem tabela/FK cross-schema** (ADR-0004/0013) — cada serviço é dono do próprio schema.
* **Se o DTO do evento existir como código**, cada serviço mantém a **própria cópia** — a
  duplicação pequena e controlada é preferível ao acoplamento (Regra de Três do YAGNI).
* **Deploy independente**: cada serviço tem pipeline, imagem e release próprios (ADR-0005).
* **Evolução do contrato** deve ser **backward-compatible** (adicionar campo opcional) ou
  introduzir nova versão de `schemaVersion` — nunca uma quebra silenciosa do consumidor.
* Schema Registry (AVRO/Glue) permanece opção futura, se a evolução de contrato ficar
  frequente; **não adotado agora**.

## Refinamento — `schemaVersion` obrigatório desde o início

O campo `schemaVersion` é **obrigatório no payload** do `lancamento.criado` **desde a versão 1**:

* Todo evento publicado por A carrega `schemaVersion` (inteiro), sempre presente.
* O consumidor B **rejeita** (→ DLQ, ADR-0014) qualquer mensagem sem `schemaVersion` ou com
  versão não suportada — nunca processa mensagem de versão desconhecida.
* A v1 já nasce versionada para que o roteamento por versão (breaking change = nova versão em
  paralelo) exista **desde o dia zero**, sem acumular passivo de mensagens sem versão.

## Consequências

* Bom, porque preserva deploy e evolução independentes de A e B (sem propagação de mudança via lib).
* Bom, porque torna o contrato explícito e versionado — o evento Kafka é a única interface entre serviços.
* Bom, porque consolida em uma decisão única o que ADR-0003 (comunicação) e ADR-0004/0013 (dados) já apontavam.
* Ruim, porque gera pequena duplicação de DTOs/contrato entre os dois serviços — aceitável e intencional.
* Ruim, porque exige disciplina na evolução do schema do evento (versionamento + compatibilidade).
