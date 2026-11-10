# Índice dos ADRs

| ADR | Decisão | Status | Data |
|---|---|---|---|
| 0001 | [Registrar decisões de arquitetura como ADRs no repositório](adrs/0001-registrar-decisoes-como-adrs.md) | accepted | 2026-11-05 |
| 0002 | [Usar Structurizr como gestor de C4 e ADRs](adrs/0002-structurizr-como-gestor-de-c4-e-adrs.md) | accepted | 2026-11-05 |
| 0003 | [Kafka como barramento de eventos entre os serviços](adrs/0003-kafka-como-barramento-de-eventos.md) | accepted | 2026-11-05 |
| 0004 | [PostgreSQL como banco de dados dos microsserviços](adrs/0004-postgresql-como-banco-de-dados.md) | accepted | 2026-11-05 |
| 0005 | [Pipeline de CI no GitHub Actions (dev → master)](adrs/0005-pipeline-de-ci-no-github-actions.md) | accepted | 2026-11-05 |
| 0006 | [Runtime, linguagem e acesso a dados do backend](adrs/0006-runtime-linguagem-e-acesso-a-dados.md) | accepted | 2026-11-05 |
| 0007 | [Segurança em camadas: API Gateway + WAF + Secrets Manager + refresh tokens](adrs/0007-seguranca-api-gateway-waf-secrets-manager.md) | accepted | 2026-11-05 |
| 0008 | [Vert.x vs Spring Boot — o que muda no contexto Outbox/Inbox](adrs/0008-vertx-vs-spring-boot.md) | accepted | 2026-11-05 |
| 0009 | [Consultar a documentação oficial do Structurizr antes de qualquer alteração no modelo C4](adrs/0009-consultar-documentacao-oficial-structurizr.md) | accepted | 2026-11-06 |
| 0010 | [Orquestração dos microsserviços — ECS Fargate vs EKS](adrs/0010-orquestracao-ecs-fargate-vs-eks.md) | accepted | 2026-11-09 |
| 0011 | [Observabilidade — logs JSON, métricas CloudWatch e tracing X-Ray (ADOT)](adrs/0011-observabilidade-logs-metricas-tracing.md) | accepted | 2026-11-09 |
| 0012 | [IaC Terraform + FinOps — organização repos/, infra declarada em .tf (sem apply)](adrs/0012-iac-terraform-finops.md) | accepted | 2026-11-09 |
| 0013 | [Instância RDS única (schemas por serviço) em dev — consolidação FinOps do database-per-service](adrs/0013-instancia-rds-unica-schemas-por-servico.md) | accepted | 2026-11-09 |
| 0014 | [FinOps do barramento e do RDS em dev — MSK Serverless pay-per-use, MSK Connect rejeitado, RDS 10h/dia](adrs/0014-finops-msk-serverless-connect-rds.md) | accepted | 2026-11-09 |
| 0015 | [Regra anti-monólito distribuído — sem libs compartilhadas entre A e B](adrs/0015-regra-anti-monolito-distribuido.md) | accepted | 2026-11-10 |
| 0016 | [FinOps da NAT Instance em dev — stop/start 10h/dia (ENI fixa + EIP)](adrs/0016-finops-nat-instance-stop-start.md) | accepted | 2026-11-10 |
| 0017 | [Ingress em dev via VPN + ALB interno — paridade de roteamento com produção](adrs/0017-ingress-dev-vpn-alb-interno.md) | accepted | 2026-11-10 |
