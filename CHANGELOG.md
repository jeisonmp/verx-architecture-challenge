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
