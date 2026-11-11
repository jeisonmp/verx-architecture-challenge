---
status: accepted
date: 2026-11-09
decision-makers: Max
---
# 0014. FinOps do barramento e do RDS em dev — MSK Serverless pay-per-use, MSK Connect rejeitado, RDS 10h/dia

## Contexto e problema

Ao consolidar a estimativa FinOps (ADR-0012, `docs/finops/`), três pontos do barramento
e do banco precisavam de fechamento: (1) o custo real do MSK Serverless, (2) se o MSK
Connect (Kafka Connect gerenciado) deveria substituir o Outbox/worker hand-rolled, e
(3) o custo do RDS em dev com a janela de uso reduzida.

## Decisão

1. **MSK Serverless é pay-per-use, sem piso de cluster.** O custo é `partition-hour`
   (US$ 0,0015) + `storage` (US$ 0,10/GB-mês) + data in/out. Correção registrada: a
   leitura de um "cluster-hour de US$ 0,75/h" (≈ US$ 547/mês ocioso) pertence ao
   **MSK Express**, não ao Serverless — o ADR-0003 permanece válido em sua justificativa
   de cobrança por uso.
2. **Partições: 4 no total** — `lancamento.criado` com 1–3 partições (ordenação por
   `id` do lançamento) + 1 no DLQ. Custo ≈ US$ 4,38/mês. **10 partições é
   super-provisionamento** (YAGNI) a 50 req/s e infla o partition-hour sem ganho.
3. **MSK Connect: rejeitado.** Seria *cost-negative*: o Outbox Worker e o Consumer já
   rodam como código dentro da task ECS Fargate paga; trocá-los por connectors gerenciados
   adicionaria ~US$ 80/mês por connector (1 MCU mínimo, cobrado mesmo ocioso) e exigiria
   `rds.logical_replication = 1` no RDS (WAL/storage adicionais) para CDC/Debezium.
4. **RDS em dev: 10h/dia via stop/start.** Compute `db.t4g.micro` = 300h/mês ≈ US$ 4,80
   + storage 20GB ≈ US$ 2,30 (cobrado 24/7) = **~US$ 7,10/mês** (vs US$ 11,68 24/7).
   Janela RDS (10h) é superconjunto da janela Fargate (8h); stop/start exige regra
   EventBridge + Lambda (limite de 7 dias parado não é atingido com cron diário).

## Consequências

* Bom, porque fecha a estimativa dev em ~US$ 21/mês (abaixo do alvo de ~US$ 30).
* Bom, porque confirma o MSK Serverless como opção FinOps-viável (o piso de US$ 547/mês era atribuição incorreta ao Express).
* Bom, porque mantém o Outbox/worker hand-rolled (zero infra extra) e rejeita o MSK Connect (YAGNI).
* Ruim, porque o stop/start do RDS introduz automação operacional (EventBridge + Lambda) e cold start de minutos.
* Ruim, porque o valor "US$ 0" exibido pelo AWS Pricing Calculator para configs mínimas é um
  artefato de arredondamento — o piso real é o partition-hour (~US$ 4,38/mês com 4 partições).
