---
status: accepted
date: 2026-11-09
decision-makers: Max
---
# 0010. Orquestração dos microsserviços — ECS Fargate vs EKS

## Contexto e problema

Os microsserviços A (Lançamentos) e B (Consolidado) precisam de um modelo de orquestração de containers na AWS. A escolha afeta o custo de operação (FinOps), a carga de sustentação da equipe e a forma de injeção de segredos (ADR-0007). Precisamos decidir entre ECS Fargate, EKS (Kubernetes gerenciado) e serverless puro.

## Alternativas consideradas

* **ECS Fargate** — escolhida.
* **EKS (managed Kubernetes):** controle fino e ecossistema K8s, mas cobra control plane fixo e impõe custo de equipe/sustentação.
* **Serverless puro (Lambda):** descartado — o núcleo é Spring Boot com outbox worker/consumer contínuo, que não é o fit natural do modelo de invocação por evento do Lambda.

## Histórico da discussão (sessão de iteração — Q&A)

### Q1 — "Qual o custo do ECS frente ao EKS para manter esses serviços?"

**R:** O **EKS cobra o control plane** em USD 0,10/hora (~USD 73/mês), **mesmo ocioso**. O **ECS não tem custo de control plane** — paga-se apenas o compute Fargate por uso (vCPU/GB por segundo). A diferença maior é operacional: EKS exige gestão de versão do cluster, add-ons (CoreDNS, kube-proxy, VPC CNI, load balancer controller), IRSA e rede — custo de equipe/sustentação que o ECS (task definitions + auto-scaling) não tem.

### Q2 — "E se a estrutura EKS já existir?"

**R:** Aí o control plane é *sunk cost* e faria sentido reutilizar o cluster. **Premissa desta decisão: não há EKS pré-existente.** Como a premissa é de que EKS não existe, adotá-lo adicionaria custo de equipe e de sustentação sem benefício para dois serviços a 50 req/s — logo, ECS pela simplicidade.

## Critérios de decisão

| Critério | ECS Fargate | EKS |
|---|---|---|
| Control plane | USD 0 | USD 0,10/h (~USD 73/mês) |
| Compute | Fargate por uso | Fargate ou EC2 (nodes a gerir) |
| Sustentação/equipe | baixa (AWS gerencia) | alta (upgrades, add-ons, IRSA) |
| Injeção de segredos | task role (ADR-0007) | IRSA ou ESO |
| Adequação ao escopo (2 serviços, 50 req/s) | suficiente | além do necessário (YAGNI) |

## Decisão

Adotar **ECS Fargate** para os dois microsserviços. Premissa registrada: **não há cluster EKS pré-existente** na conta; se houvesse um reaproveitável, a decisão seria reaberta (control plane como *sunk cost*).

## Consequências

* Bom, porque elimina o custo fixo do control plane EKS (~USD 73/mês) — paga-se apenas o compute consumido.
* Bom, porque reduz o custo de equipe/sustentação (sem gestão de cluster, upgrades e add-ons).
* Bom, porque a injeção de segredos via **task role** (ADR-0007) dispensa ESO/IRSA e sidecar.
* Ruim, porque reduz a portabilidade (menos neutro que Kubernetes numa eventual saída da AWS).
* Ruim, porque abre mão do ecossistema Helm/operators — desnecessário nesta escala (YAGNI).
