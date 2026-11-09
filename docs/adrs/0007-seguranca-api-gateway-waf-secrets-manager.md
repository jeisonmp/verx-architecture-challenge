---
status: accepted
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

### Q2 — "Como o Secrets Manager faz a rotação sem downtime? E como isso fica framework-agnóstico?"

**R:** A rotação é orquestrada no lado AWS por uma **Lambda de rotação** do Secrets Manager (cria a nova versão `AWSCURRENT` e atualiza o recurso dependente). Dois refinamentos:

1. **Downtime:** o "restart" no ECS é um **rolling update zero-downtime** (min/max healthy percent + health checks + connection draining no ALB): a task antiga segue servindo até a nova estar saudável. Sem janela de indisponibilidade — mas restart-por-rotação é pesado e evitável.
2. **Framework-agnóstico:** `@RefreshScope` é específico do Spring/Java e **não** é premissa válida para ambiente poliglota. O caminho agnóstico é eliminar a rotação na fonte: **RDS IAM auth** (banco) e **MSK IAM auth** (Kafka) tornam a credencial *passwordless* (token de vida curta, no nível de protocolo/driver, sem segredo a rotacionar). Para os segredos residuais (chaves de assinatura JWT, `api_key`), o refresh agnóstico é **driver wrapper** (JDBC/Python/Node) ou **sidecar/ESO** — não `@RefreshScope`.

### Q3 — "Front-end / página de consumo / login com AD são necessários?"

**R:** Não — fora do escopo do desafio (backend CQRS). O "consumo de relatórios" é `GET /consolidado` via `api_key` (máquina-a-máquina). A "camada UI/Cliente" do feedback refere-se ao C4 L1/L2, não a um app web. Identidade humana/AD seria scope creep (YAGNI); se desejado no futuro, seria Cognito + SAML 2.0 federado ao AD.

### Q4 — "Como o serviço valida o token? E por que não separar a autenticação num sidecar?"

**R:** A validação do JWT **não** é via Spring Cloud AWS — é via **Spring Security OAuth2 Resource Server**. O serviço baixa o JWKS do Cognito no startup e **verifica a assinatura** localmente (o JWT é *assinado*, não *criptografado* — o payload é legível; a assinatura garante integridade/autenticidade), **sem chamada ao IdP por requisição**. O Spring Cloud AWS fica restrito ao Secrets Manager.

O **sidecar de autenticação** (Envoy/Istio/App Mesh) é um padrão legítimo, mas foi **descartado nesta escala** por cinco razões: (1) a carga de verificação RSA é desprezível a 50 req/s; (2) a separação auth×negócio já existe dentro do serviço (filtro do framework + `@PreAuthorize` declarativo); (3) sidecar dobra containers e operação (YAGNI); (4) cria novo problema de confiança — o serviço confiaria em headers injetados, exigindo isolamento de rede; (5) framework-agnosticismo não é requisito — stack homogêneo Java/Spring. **Reabrir se o cenário virar poliglota/alta escala.**

## Decisão

Adotar **defesa em camadas (defense in depth)** sobre serviços gerenciados da AWS:

1. **Borda — Amazon API Gateway + AWS WAF**
   - API Gateway como **único ponto de entrada** para `POST /lancamentos` e `GET /consolidado`.
   - AWS WAF com **rate limiting** por IP/chave (bloqueia DDoS/força bruta antes de chegar à aplicação).
   - TLS 1.2+ (HTTPS) obrigatório em todo o tráfego.

2. **Autenticação e autorização**
   - **Autenticação (quem é):** **Cognito** (User Pools) emite access token (JWT, ~5 min) + refresh token rotativo (detecção/revogação por reuso). O **API Gateway** valida o JWT na borda (Cognito Authorizer).
   - **Autorização (o que pode):** o Cognito **fornece as claims** (`cashflow:write`/`cashflow:read`); a enforcement é **dupla** — **API Gateway** (OAuth scopes na borda) **e serviço** (filtro, zero-trust).
   - **Validação no serviço (offline):** **Spring Security OAuth2 Resource Server** baixa o JWKS do Cognito no startup e **verifica a assinatura** (o JWT é assinado, não criptografado) localmente — sem chamada ao IdP por requisição. Escopo via `@PreAuthorize("hasAuthority('SCOPE_…')")`.
   - **`api_key`** (integrações máquina-a-máquina): validado **100% na borda** (usage plan + rate limit próprio) — o serviço não vê.
   - **Sidecar de autenticação — avaliado e descartado** (razões no Histórico Q4); reabrir se o cenário virar poliglota/alta escala.

3. **Gestão de segredos — IAM auth (passwordless)**
   - **Banco e Kafka passwordless via IAM:** **RDS IAM auth** (PostgreSQL) e **MSK IAM auth** (Kafka) usam token de vida curta no nível de protocolo/driver — elimina segredo estático e a necessidade de rotação. O acesso é autorizado pela **task role** (ECS Fargate).
   - **Segredos estáticos eliminados nos serviços:** a chave de assinatura JWT vive no **Cognito** (o serviço só tem a chave pública do JWKS); o `api_key` vive no **API Gateway**. O serviço, portanto, **não guarda segredo estático**.
   - **Secrets Manager como fallback:** só para segredos que não podem ser IAM-ificados (ex.: credencial de integração externa futura), via **task role**, com refresh framework-agnóstico (driver wrapper ou sidecar/ESO) — nunca `@RefreshScope`.
   - Nada em texto puro no código ou em variável de ambiente versionada.

4. **Criptografia**
   - **Em trânsito:** TLS 1.2+ em todo o tráfego — borda HTTPS, serviço→RDS (SSL), serviço→MSK (TLS).
   - **mTLS — não adotado** (substituído por IAM + `api_key`):
     1. **Sobreposição de identidade:** o mTLS garante que a máquina "A" é a máquina "A"; se `api_key` + IAM já validam com segurança que a requisição vem de um cliente autorizado, o mTLS se torna uma camada redundante de autenticação.
     2. **Custo operacional:** manter mTLS exige gerenciar o ciclo de vida de certificados (que expiram e podem derrubar o ambiente se não renovados); se IAM + `api_key` já resolvem o problema de segurança com menos esforço, opta-se pela simplicidade.
     3. **Falta de granularidade do TLS puro:** o mTLS diz qual máquina está conectando, mas não entende nativamente regras de negócio (ex.: "esta chave só pode acessar a rota `/lancamentos` via método POST"); o IAM é desenhado especificamente para esse tipo de política granular.
   - **Em repouso:** criptografia ativada no banco (Aurora/RDS) e no Kafka (MSK), com **KMS (Key Management Service)** para gestão de chaves.

Justificativa: o API Gateway + WAF atendem diretamente a "proteção ativa contra ataques"; o Secrets Manager elimina credencial hardcoded; o refresh token rotativo resolve o ponto de "persistência de autenticação/criptografia". Tudo é gerenciado na AWS, alinhado ao restante da solução.

## Consequências

- Bom, porque a borda única centraliza rate limiting, WAF e observabilidade de acesso.
- Bom, porque Secrets Manager + IAM eliminam credenciais hardcoded.
- Bom, porque o refresh token rotativo mitiga roubo de sessão e resolve a persistência segura de autenticação.
- Bom, porque `api_key` + escopos cobrem o consumo/integração de serviços (requisito diferencial).
- Ruim, porque adiciona componentes na borda (gateway + WAF), elevando custo e complexidade operacional.
- Ruim, porque a rotação de refresh tokens exige storage de sessão (ex.: Redis/DynamoDB) e tratamento de revogação.
- Ruim, porque WAF/rate limiting precisam de calibração para não derrubar tráfego legítimo (meta ≤5% de perda nos 50 req/s).
