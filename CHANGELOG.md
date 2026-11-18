# [1.0.0-beta.3](https://github.com/jeisonmp/verx-architecture-challenge/compare/v1.0.0-beta.2...v1.0.0-beta.3) (2026-11-18)


### Features

* US-001 - Testar CorrelationIdFilter ([961c702](https://github.com/jeisonmp/verx-architecture-challenge/commit/961c702aa2931b8f189853ba8bdef76b07a6191d))
* US-001 - Testar CorrelationIdFilter ([8bab575](https://github.com/jeisonmp/verx-architecture-challenge/commit/8bab5753d951c0e68dadce0c68ae351d2652d56b))
* US-002 - Testar SensitiveDataMessageConverter ([3324bad](https://github.com/jeisonmp/verx-architecture-challenge/commit/3324badf21c7888db677e5d93c615c2d69d25449))
* US-002 - Testar SensitiveDataMessageConverter ([90902fb](https://github.com/jeisonmp/verx-architecture-challenge/commit/90902fbefaaf34fd30dcbefd006bd1214e0d7baa))

# [1.0.0-beta.2](https://github.com/jeisonmp/verx-architecture-challenge/compare/v1.0.0-beta.1...v1.0.0-beta.2) (2026-11-17)


### Bug Fixes

* corrige destino do COPY do jar nos Dockerfiles (/app.jar) ([7e386cb](https://github.com/jeisonmp/verx-architecture-challenge/commit/7e386cb84ef864cb54bb9e60967cc96fea91ab5e))
* corrige provider <pattern> do logback para JSON pattern ([b20232a](https://github.com/jeisonmp/verx-architecture-challenge/commit/b20232a75a21b2c0e64b99ff8d39dec38304cc58))
* US-001 - Tornar o teste CDC do produtor verde no 4.x (canal em memória) ([5cc58bb](https://github.com/jeisonmp/verx-architecture-challenge/commit/5cc58bb0ea4cb1ca66a92cde8bfee0e2a7af5cbd))
* US-002 - Reescrever o teste CDC do consumidor como desserialização tolerant reader ([e0f07c4](https://github.com/jeisonmp/verx-architecture-challenge/commit/e0f07c4eb9aa99d0719b0cceef984b2305619e85))
* US-003 - Atualizar quality.yml e a documentação (AGENTS.md) para o fluxo CDC 4.x ([1aa18c6](https://github.com/jeisonmp/verx-architecture-challenge/commit/1aa18c6efb36adfeed3407da9cd8beeec8b4198a))


### Features

* US-001 - Consolidar o compose local na raiz ([b70737e](https://github.com/jeisonmp/verx-architecture-challenge/commit/b70737e3647b788e76d6a727d17d52236104a55b))
* US-001 - Scaffold do monorepo (Lançamentos A e Consolidado B) ([3a0dcf8](https://github.com/jeisonmp/verx-architecture-challenge/commit/3a0dcf886d52f93b344849731f59397b658700d8))
* US-001 - Workflow de segurança — SAST + SCA + gitleaks (feat/* → dev) ([eddee3c](https://github.com/jeisonmp/verx-architecture-challenge/commit/eddee3c8809e22cb6895ec5032e5ecb91b2e5d05))
* US-002 - Contrato CDC do evento lancamento.criado ([36f0a02](https://github.com/jeisonmp/verx-architecture-challenge/commit/36f0a02b8fae482310ea64ca1ba4fc410d7b68fa))
* US-002 - Contrato CDC do evento lancamento.criado ([53a68cd](https://github.com/jeisonmp/verx-architecture-challenge/commit/53a68cd07b11e8a392f0373f1af670a1955b1b20))
* US-002 - Mover os artefatos de dev para a raiz ([e5afbba](https://github.com/jeisonmp/verx-architecture-challenge/commit/e5afbba684b39dd8416edf6388f67c14548c7b68))
* US-002 - Workflow de testes + cobertura (dev → master) ([fc35119](https://github.com/jeisonmp/verx-architecture-challenge/commit/fc35119e823389fdb3406bfd7ed12e6bd0c5b9e0))
* US-003 - Reescrever o README da raiz ([73b9d05](https://github.com/jeisonmp/verx-architecture-challenge/commit/73b9d050bbb7293d39bed5cc295934aa5a6bc35a))
* US-004 - docker-compose completo (PostgreSQL + Kafka + apps A/B) ([4292ed9](https://github.com/jeisonmp/verx-architecture-challenge/commit/4292ed9c4b52911432d5e198602c0d9fcd3f801d))
* US-004 - Reduzir o README de infra ao escopo IaC ([e55b24d](https://github.com/jeisonmp/verx-architecture-challenge/commit/e55b24d178683fdbe8072346e8e99cf794db0906))
* US-005 - Atualizar AGENTS.md e referências cruzadas ([f3ae9b1](https://github.com/jeisonmp/verx-architecture-challenge/commit/f3ae9b199bb3fc7bee8788c1ea0b566a517f622d))
* US-005 - Health checks + structured logging (JSON) ([7953c33](https://github.com/jeisonmp/verx-architecture-challenge/commit/7953c33bf12efeb00020a9550c54c6d2a4d60e17))

# 1.0.0-beta.1 (2026-11-11)


### Bug Fixes

* **infra:** aplicar quick-wins de segurança e compliance (checkov) ([17ec038](https://github.com/jeisonmp/verx-architecture-challenge/commit/17ec0389877b04edd1e01f9f8ebf61a51c34a587))
* **infra:** corrigir MSK Serverless e resolução de ambiente para o validate ([eb1bd54](https://github.com/jeisonmp/verx-architecture-challenge/commit/eb1bd549a695ed2452cf2463c615aaa9c5a39ebe))
* **infra:** corrigir referência ADR-0014 → ADR-0016 no stop/start da NAT ([ec47da1](https://github.com/jeisonmp/verx-architecture-challenge/commit/ec47da1ba6b10228dce1e924886c19feaee5f54f))
* **infra:** endurecer IMDS v2 na NAT e documentar egress irrestrito ([385b48b](https://github.com/jeisonmp/verx-architecture-challenge/commit/385b48bfcff191b8fdbcc7ae21e615de62b3632d))
* **infra:** Fargate 1 vCPU / 2 GB (0,5 GB não roda Spring Boot 3) ([2c6d39d](https://github.com/jeisonmp/verx-architecture-challenge/commit/2c6d39de06f0e68a0c48fffefe997b29f7632752))


### Features

* **infra:** NAT Instance stop/start 10h/dia (ENI fixa + scheduler) ([6754cc4](https://github.com/jeisonmp/verx-architecture-challenge/commit/6754cc403b3b4dc71117c5ab3653b4ba1e1eefee))
* **infra:** RDS único (schemas), stop/start 10h/dia, config por ambiente e NAT instance ([2140113](https://github.com/jeisonmp/verx-architecture-challenge/commit/2140113bcf086bd686666053051472c8f25fd03e))
* **infra:** Terraform real (ECS/MSK/RDS) + locking nativo S3 (use_lockfile) e FinOps CSV ([8ed5cf5](https://github.com/jeisonmp/verx-architecture-challenge/commit/8ed5cf519aa23ef4d1a9ecd26b73ef916adee943))
* sprint 0 ([#2](https://github.com/jeisonmp/verx-architecture-challenge/issues/2)) ([50defad](https://github.com/jeisonmp/verx-architecture-challenge/commit/50defad8f3a5dd5466ce98174cb31210143014ac))

# Changelog do Cliente

Registro de decisões técnicas e entregas do projeto do cliente (a evolução do framework ralph-engine é machine-owned e não vive neste arquivo).
