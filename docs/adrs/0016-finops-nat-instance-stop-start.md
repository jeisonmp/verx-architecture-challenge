---
status: accepted
date: 2026-11-10
decision-makers: Max
---
# 0016. FinOps da NAT Instance em dev — stop/start 10h/dia (ENI fixa + EIP)

## Contexto e problema

A ADR-0012 (Q5/Decisão 9) definiu o egress de `dev` como **NAT Instance `t3.micro`**
(free tier), em vez de NAT Gateway (~US$ 32/mês). Faltava, porém, decidir a **janela de
execução** da NAT: como `dev` não roda 24/7 (regra FinOps do motor §6.3), manter a
instância ligada o dia todo desperdiça ~14h/dia de compute ocioso. A ADR-0014 já havia
resolvido o mesmo problema para o RDS (stop/start 10h/dia), mas a NAT tem uma
particularidade que o RDS não tem: o **egress das sub-redes privadas depende da rota
apontando para a ENI da NAT** — se a instância parar e voltar com IP/ENI diferente, a
rota privada quebra silenciosamente.

## Alternativas consideradas

* **NAT 24/7 em dev** — simples, mas paga compute ocioso (~14h/dia), violando a regra
  "dev nunca roda 24/7".
* **NAT Instance on-demand + stop/start 10h/dia com ENI fixa + EIP** — escolhida.
* **NAT Gateway (~US$ 32/mês)** — já descartado na ADR-0012 (custo fixo alto para dev).
* **Spot** — descartado: reclaim de ~2min derrubaria o egress do Fargate.

## Critérios de decisão

| Critério | NAT 24/7 | Stop/start 10h/dia (escolhida) |
|---|---|---|
| Custo compute | 24h/dia (fora do free tier após 12 meses) | 10h/dia (300h/mês < 750h free tier) |
| Sobrevivência do egress | trivial | exige ENI fixa + EIP |
| Complexidade operacional | nenhuma | EventBridge + Lambda + cloud-config |
| Alinhamento FinOps (dev) | viola | atende |

## Decisão

Adotar **stop/start da NAT Instance em `dev`, 10h/dia** (janela 08:00–18:00
America/Sao_Paulo), com **ENI primária fixa + EIP** para que a rota privada sobreviva
ao stop/start:

1. **Janela 08:00–18:00 BRT** (11:00–21:00 UTC), idêntica ao RDS (ADR-0014) e
   superconjunto do Fargate (8h/dia). Cron: start `cron(0 11 * * ? *)`, stop
   `cron(0 21 * * ? *)`.
2. **Ativo somente em `dev`** — condicionado a `nat.stop_start: true` em
   `environments/<env>.yaml`; `qas`/`prd` mantêm a NAT 24/7.
3. **ENI primária fixa** (`aws_network_interface` com IP privado fixo `10.0.0.10`,
   `source_dest_check = false`) — a rota privada (`0.0.0.0/0 → network_interface_id`)
   aponta para a ENI, que **não** é destruída no stop/start; a instância reanexa a ENI
   a cada boot.
4. **EIP associado à ENI** — IP público estável; o custo do EIP ocioso (~US$ 2,10/mês)
   é aceito como trade-off pela estabilidade da rota (a alternativa sem EIP — IP
   auto-assign + rota por `instance_id` — é legado e frágil).
5. **cloud-config `bootcmd`** reaplica `ip_forward` + MASQUERADE a **cada** boot (não
   só no 1º), pois o stop/start zera `iptables`/`sysctl`.
6. **Scheduler**: EventBridge (2 cron rules) → Lambda `python3.12` →
   `ec2:StartInstances`/`ec2:StopInstances` com IAM least-privilege (Resource =
   `aws_instance.nat.arn`, ADR-0007). Idempotente: `IncorrectInstanceState` → no-op.

## Consequências

* Bom, porque o compute da NAT fica 10h/dia (300h/mês < 750h free tier = US$ 0 por 12
  meses; ≈ US$ 3,16/mês depois) — coerente com a regra "dev nunca roda 24/7".
* Bom, porque a ENI fixa + EIP mantêm a rota privada estável através do stop/start,
  sem reconfiguração manual.
* Bom, porque a janela é idêntica ao RDS (ADR-0014), deixando uma única janela mental
  de operação em dev.
* Ruim, porque o EIP ocioso custa ~US$ 2,10/mês enquanto a instância está parada —
  trade-off aceito pela estabilidade da rota.
* Ruim, porque introduz automação operacional (EventBridge + Lambda) e cold start de
  ~1–2min na subida da NAT (impacto no egress do Fargate nesse intervalo).
* Ruim, porque a janela fixa não cobre uso esporádico fora de 08:00–18:00 (ex.: sessão
  de debug noturna exigiria ligar a NAT manualmente).
