---
status: proposed
date: 2026-11-05
decision-makers: Max
---
# 0007. Segurança em camadas: API Gateway + WAF + Secrets Manager + refresh tokens

## Contexto e problema

A solução exige tratar **segurança** (autenticação, autorização, criptografia e proteção contra ataques) e lista como diferencial os **critérios de segurança para consumo (integração) de serviços**. Os pontos críticos a mitigar são: credenciais estáticas em texto puro, persistência de autenticação inadequada, falta de criptografia/refresh tokens e ausência de proteção ativa (rate limiting/WAF).

Precisamos de uma estratégia de segurança de ponta a ponta — borda (DDoS), autenticação/autorização, gestão de segredos e criptografia — sem credencial hardcoded e sem depender apenas de um JWT básico de longa duração.

## Alternativas consideradas

* **Defesa em camadas sobre serviços gerenciados da AWS** (API Gateway + WAF + Secrets Manager + refresh token) — escolhida.
* **JWT único de longa duração:** simples, porém sem rotação/revogação — insuficiente para autenticação segura.
* **Credenciais em variável de ambiente ou arquivo versionado:** falha básica de segurança, explicitamente penalizada.
* **IAM-only sem API Gateway:** expõe os microsserviços diretamente, sem WAF/rate limiting na borda.

## Histórico da discussão (sessão de iteração — Q&A)

### Q1 — "A integração dos serviços (containers) com o Secrets Manager seria via sidecar? Exigiria Kubernetes?"

**R:** Não — sidecar não é necessário nem obriga Kubernetes. O padrão idiomático AWS é a app buscar o segredo no startup via **Spring Cloud AWS** (`spring-cloud-aws-starter-secrets-manager`, dependência Maven — não um container extra), autorizada pela **task role** (ECS Fargate) ou IRSA (EKS). Zero sidecar: o app de negócio fica agnóstico de credencial; a segurança vive na borda (Gateway/WAF) e no IAM. Sidecar só se houvesse requisito de "a app não fala com AWS".

### Q2 — "Como o Spring Cloud AWS Secrets Manager faz a rotação, se o segredo é lido só no startup?"

**R:** A rotação é **orquestrada no lado AWS** por uma **Lambda de rotação** do Secrets Manager (cria a nova versão `AWSCURRENT` e atualiza o recurso dependente). A lib **não "assiste" a rotação sozinha** — a app repõe a nova versão de duas formas: (a) **restart/redeploy** (padrão ECS: rolling update pós-rotação; o startup relê `AWSCURRENT`) ou (b) **`@RefreshScope`** (refresh via `/actuator/refresh` ou Spring Cloud Bus, sem restart). Para credencial de banco, o caminho que elimina a rotação é **RDS IAM auth** (token de vida curta) ou o **AWS JDBC wrapper** (recarrega no reconnect).

### Q3 — "Front-end / página de consumo / login com AD são necessários?"

**R:** Não — fora do escopo do desafio (backend CQRS). O "consumo de relatórios" é `GET /consolidado` via `api_key` (máquina-a-máquina). A "camada UI/Cliente" do feedback refere-se ao C4 L1/L2, não a um app web. Identidade humana/AD seria scope creep (YAGNI); se desejado no futuro, seria Cognito + SAML 2.0 federado ao AD.

## Decisão

Adotar **defesa em camadas (defense in depth)** sobre serviços gerenciados da AWS:

1. **Borda — Amazon API Gateway + AWS WAF**
   - API Gateway como **único ponto de entrada** para `POST /lancamentos` e `GET /consolidado`.
   - AWS WAF com **rate limiting** por IP/chave (bloqueia DDoS/força bruta antes de chegar à aplicação).
   - TLS 1.2+ (HTTPS) obrigatório em todo o tráfego.

2. **Autenticação e autorização**
   - OAuth2/OIDC: **access token** (JWT) de curta duração (~5 min) + **refresh token rotativo**, com detecção e revogação por reuso.
   - Autorização por escopo: `cashflow:write` (Lançamentos) e `cashflow:read` (Consolidado).
   - **`api_key`** para integrações máquina-a-máquina (consumo de serviços), com rate limit próprio — atende o diferencial de segurança para consumo.

3. **Gestão de segredos — AWS Secrets Manager**
   - Credenciais de banco, chaves do Kafka e segredos de assinatura JWT são injetados em runtime via **Secrets Manager + IAM**. Nada em texto puro no código ou em variável de ambiente versionada.

4. **Criptografia**
   - **Em trânsito:** TLS 1.2+ (mTLS na comunicação serviço→MSK, quando aplicável).
   - **Em repouso:** criptografia ativada no banco (Aurora/RDS) e no Kafka, com KMS para gestão de chaves.

Justificativa: o API Gateway + WAF atendem diretamente a "proteção ativa contra ataques"; o Secrets Manager elimina credencial hardcoded; o refresh token rotativo resolve o ponto de "persistência de autenticação/criptografia". Tudo é gerenciado na AWS, alinhado ao restante da solução.

## Consequências

- Bom, porque a borda única centraliza rate limiting, WAF e observabilidade de acesso.
- Bom, porque Secrets Manager + IAM eliminam credenciais hardcoded.
- Bom, porque o refresh token rotativo mitiga roubo de sessão e resolve a persistência segura de autenticação.
- Bom, porque `api_key` + escopos cobrem o consumo/integração de serviços (requisito diferencial).
- Ruim, porque adiciona componentes na borda (gateway + WAF), elevando custo e complexidade operacional.
- Ruim, porque a rotação de refresh tokens exige storage de sessão (ex.: Redis/DynamoDB) e tratamento de revogação.
- Ruim, porque WAF/rate limiting precisam de calibração para não derrubar tráfego legítimo (meta ≤5% de perda nos 50 req/s).
