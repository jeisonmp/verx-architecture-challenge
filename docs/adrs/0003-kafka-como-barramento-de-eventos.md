---
status: accepted
date: 2026-11-05
decision-makers: Max
---
# 0003. Kafka como barramento de eventos entre os serviços

## Contexto e problema

O requisito "o serviço de controle não deve ficar indisponível se o consolidado cair" exige desacoplamento entre escrita (Lançamentos) e leitura (Consolidado) via mensageria. Precisamos escolher o broker entre Kafka (MSK), AWS SQS/SNS e RabbitMQ (Amazon MQ).

## Histórico da discussão (sessão de iteração — Q&A)

### Q1 — "O MSK Serverless é premissa da solução ou decisão de arquitetura? Permite validar a solução localmente?"

**R:** É decisão de arquitetura (este ADR), não premissa — a premissa é o desacoplamento CQRS + Transactional Outbox/Inbox e a nuvem AWS. E sim: o MSK Serverless é **Apache Kafka real** por baixo (mesmo protocolo/API), então produtor/consumidor escritos contra o protocolo Kafka rodam idênticos num broker local (container) e no MSK — **zero alteração de lógica de negócio** entre o ambiente local de testes e a produção. A única diferença é de **borda**: o MSK Serverless exige autenticação IAM (`AWS_MSK_IAM`) ou mTLS, ausente num broker local PLAINTEXT.

### Q2 — "Como provisionar o ambiente de teste local, mantendo os testes de integração com containers (PostgreSQL + Kafka)?"

**R:** Duas trilhas: **Testcontainers** para os testes de integração no CI (PostgreSQL + Kafka efêmeros por execução) e **docker-compose** (Kafka single-node KRaft + PostgreSQL) para e2e/load manual. O MSK Serverless permanece alvo de produção; como o protocolo Kafka é o mesmo, não há fork de código nem provider distinto entre local e nuvem.

### Q3 — "Dado zero alteração de código, podemos aprovar a decisão? E devemos registrar explicitamente que não usaremos um cluster Kafka autogerenciado?"

**R:** Sim — aprova-se o Kafka (MSK Serverless). Registra-se também que **não** se adotará um cluster Kafka autogerenciado (EC2/EKS ou MSK Provisioned): o MSK Serverless entrega **escalabilidade nativa** (vazão dimensionada por partição, sem redimensionar brokers) e **downtime operacional eliminado** (patches/upgrades/redimensionamento gerenciados pela AWS, multi-AZ), **reduz o custo de manutenção** (sem gestão de brokers/patches) e **elimina a complexidade de infraestrutura** de um Kafka autogerenciado em Kubernetes para escalabilidade horizontal — preservando o mesmo protocolo, o que mantém zero alteração de lógica de negócio entre local e produção (a autenticação IAM/mTLS é a única diferença de borda).

## Decisão

Adotar **Kafka (Amazon MSK Serverless)** como barramento de eventos. O padrão Transactional Outbox publica eventos de lançamento no tópico `lancamento.criado`; o Consolidado consome via Inbox com idempotência.

**Não** adotar um cluster Kafka autogerenciado (EC2/EKS ou MSK Provisioned). O MSK Serverless entrega escalabilidade nativa e downtime operacional eliminado com o mesmo protocolo Kafka, reduz o custo de manutenção (sem gestão de brokers) e elimina a complexidade de infraestrutura de um Kafka autogerenciado em Kubernetes para escalabilidade horizontal — mantendo zero alteração de lógica de negócio entre o broker local de testes e a produção (a autenticação IAM/mTLS é a única diferença de borda).

Justificativa: para um domínio financeiro, o Kafka oferece **log imutável com replay e retenção** (auditoria/reprocessamento de eventos de caixa), **ordenação por chave** e ecossistema maduro de clients Kafka e de padrões outbox/inbox. O MSK Serverless preserva a semântica do Kafka sem gerenciar brokers, com escalabilidade automática, downtime operacional eliminado e cobrança por uso (alinhado a FinOps).

Alternativas descartadas:

* **Cluster Kafka autogerenciado (EC2/EKS/MSK Provisioned):** mantém replay/retention e o protocolo Kafka, mas impõe gestão de brokers, Zookeeper/KRaft, patching, janelas de manutenção (downtime), custo de manutenção contínuo e a complexidade de operar Kubernetes para escalabilidade horizontal (StatefulSets, rebalanceamento de partições, dimensionamento de volumes) — incompatível com a meta de eliminar downtime operacional e contrário ao FinOps.
* **SQS/SNS:** mais barato e sem operação, porém sem replay/log durável e com ordenação limitada (apenas FIFO) — insuficiente para auditoria financeira.
* **RabbitMQ (Amazon MQ):** maduro e simples, porém sem log de replay/retention e menos adequado a streaming de eventos de longo prazo.

## Consequências

* Bom, porque replay e retention permitem reprocessar eventos e reconstruir estado (auditoria financeira).
* Bom, porque a ordenação por chave (id do lançamento) preserva a ordem das operações.
* Bom, porque o MSK Serverless elimina a gestão de brokers, entrega escalabilidade nativa e downtime operacional eliminado, reduz o custo de manutenção e remove a complexidade de um Kafka autogerenciado em Kubernetes (escalabilidade horizontal), cobrando por uso (FinOps).
* Bom, porque o protocolo Kafka é idêntico ao de um broker local, mantendo zero alteração de lógica de negócio entre teste e produção.
* Ruim, porque o MSK Serverless impõe autenticação IAM/mTLS, ausente num broker local — a configuração de borda difere entre teste e produção (a lógica de negócio não muda).
* Ruim, porque Kafka é mais complexo que SQS (tópicos, consumers, offsets).
* Ruim, porque o custo do MSK, ainda que serverless, tende a superar SQS/SNS em baixa volumetria.
