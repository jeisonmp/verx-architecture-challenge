---
status: accepted
date: 2026-11-10
decision-makers: Max
---
# 0017. Ingress em dev via VPN + ALB interno — paridade de roteamento com produção

## Contexto e problema

A borda de produção foi definida pela ADR-0007 (API Gateway + WAF como único ponto de
entrada) e a orquestração pela ADR-0010 (ECS Fargate), mas o **caminho de entrada dentro
da VPC nunca foi decidido**: o que fica na frente dos containers Fargate? A ADR-0012 Q4
isolou `dev`/`qas` atrás de VPC + VPN (notebook), mas não disse **aonde** a VPN chega
dentro da VPC. O modelo C4 (`workspace.dsl`) e o `ARCHITECTURE.md` já desenham um ALB,
porém sem decisão formal nem definição do esquema (interno vs internet-facing).

Há ainda o requisito de **paridade de roteamento**: o desenvolvedor deve exercitar em
`dev` exatamente o mesmo roteamento ALB→ECS de produção, sem expor superfície de ataque
à internet.

## Alternativas consideradas

* **ALB interno (`internal = true`)** — escolhida.
* **ALB internet-facing em dev** — rejeitada: expõe superfície pública, viola o isolamento.
* **NLB interno (L4)** — rejeitada: não faz roteamento L7/health check por path.
* **Sem LB (expor task ECS diretamente)** — rejeitada: perde health check/connection
  draining e não reproduz produção.

## Histórico da discussão (sessão de iteração — Q&A)

> Definição de arquitetura do ambiente dev fornecida como requisito (2026-11-10):

> "No ambiente de desenvolvimento, isolei a VPC completamente da internet pública. O
> tráfego de saída (Egress) passa por um NAT Instance para zerar o custo do NAT Gateway
> gerenciado. O tráfego de entrada (Ingress) é feito estritamente via VPN, que se
> comunica com um Application Load Balancer (ALB) marcado como 'Internal'. Isso garante
> que os desenvolvedores testem o roteamento exatamente como ele será em Produção, mas
> sem expor nenhuma superfície de ataque para a internet."

## Critérios de decisão

| Critério | ALB interno | ALB internet-facing | NLB interno |
|---|---|---|---|
| Superfície de ataque em dev | nenhuma | alta | nenhuma |
| Roteamento L7 (path/health check) | sim | sim | não |
| Paridade dev↔prod | sim | parcial | não |
| Custo/operação | baixo | baixo | baixo |

## Decisão

Adotar um **Application Load Balancer interno** (`internal = true`) como ingress dos
containers ECS Fargate (A e B), com dois caminhos de entrada:

1. **Produção:** API Gateway + WAF (borda pública, ADR-0007) → **VPC Link** → ALB
   interno → ECS Fargate. A borda pública fica toda no API Gateway; o ALB nunca é
   internet-facing.
2. **Dev/qas:** **VPN** → ALB interno → ECS Fargate (sem API Gateway, sem IP público).
3. **Paridade de roteamento:** o ALB (target groups, health checks, connection draining)
   é **o mesmo** em dev e prod — muda apenas o que o precede (VPN vs API Gateway).
4. **VPC sem ingress da internet** em dev (sem Internet Gateway inbound); egress via NAT
   Instance (ADR-0012) com stop/start (ADR-0016).

## Consequências

* Bom, porque dev exercita o mesmo roteamento ALB→ECS de produção (paridade), reduzindo
  surpresas em deploy.
* Bom, porque zero superfície de ataque pública em dev (ALB e tasks sem IP público).
* Bom, porque o ALB traz health checks + connection draining no rolling update (ADR-0007).
* Ruim, porque acessar dev exige VPN (setup/onboarding do desenvolvedor).
* Ruim, porque o ALB é mais um recurso a provisionar/operar (custo baixo em dev).
