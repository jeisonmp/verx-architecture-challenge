---
project: consolidado
---

# PRD: Testes de cobertura do logging LGPD (consolidado)

**Projeto afetado:** `repos/consolidado` (Microsserviço B — leitura).

## 1. Introduction/Overview

O gate JaCoCo do `quality.yml` (≥ 80% de cobertura de linhas) reprovará o `mvn verify`
do microsserviço B pelo mesmo motivo do A (~52%): o scaffold do Sprint 1 entregou a
infra de logging LGPD (`SensitiveDataMasker`, `CorrelationIdFilter`,
`SensitiveDataMessageConverter` — ADR-0011), mas apenas `SensitiveDataMasker` tem teste
(`SensitiveDataMaskerTest`).

Esta PRD fecha a lacuna adicionando testes unitários para `CorrelationIdFilter` e
`SensitiveDataMessageConverter`, elevando a cobertura do bundle acima de 80% e
completando o US-005 (structured logging + redação LGPD §2.1).

## 2. Goals

- Cobertura de linhas do bundle `consolidado` ≥ 80% no `mvn verify` (gate JaCoCo atual).
- `CorrelationIdFilter` coberto em todos os ramos (sanitize válido/inválido, ciclo do MDC, log de acesso).
- `SensitiveDataMessageConverter` coberto (aplica `SensitiveDataMasker` à mensagem formatada).
- Zero alteração em código de produção.

## User Stories

### US-001: Testar CorrelationIdFilter

**type:** feature

**required_intelligence:** "flash"

**Description:** Como engenheiro, quero testar o `CorrelationIdFilter` para garantir que o `correlationId` é sanitizado e propagado ao MDC, e que credenciais nunca alcançam a linha de acesso do log (LGPD §2.1).

**Acceptance Criteria:**

- [x] Header `X-Correlation-Id` válido (`[A-Za-z0-9._-]{1,128}`) é preservado e propagado ao MDC sob a chave `correlationId`.
- [x] Header inválido (fora do padrão) ou ausente gera um UUID, e esse valor é usado no MDC.
- [x] O response header `X-Correlation-Id` recebe o correlationId sanitizado.
- [x] A cadeia `FilterChain.doFilter` é invocada e o MDC é removido no `finally`.
- [x] O método privado `sanitize` é exercitado nos dois caminhos (válido e inválido) — cobertura total de ramos.
- [x] `mvn -B verify` em `repos/consolidado` passa com cobertura de linhas ≥ 80%.

### US-002: Testar SensitiveDataMessageConverter

**type:** feature

**required_intelligence:** "flash"

**Description:** Como engenheiro, quero testar o `SensitiveDataMessageConverter` para garantir que a conversion word Logback `%maskedMsg` aplica `SensitiveDataMasker` à mensagem antes da serialização JSON.

**Acceptance Criteria:**

- [x] Mensagem formatada com token sensível (ex.: `Authorization: Bearer abc.def`) é redigida (`[REDACTED]`).
- [x] O método `convert` atinge cobertura total.
- [x] `mvn -B verify` em `repos/consolidado` passa com cobertura de linhas ≥ 80%.

## 4. Functional Requirements

- FR-1: Criar `CorrelationIdFilterTest` em `repos/consolidado/src/test/java/com/verx/cashflow/consolidado/infra/logging/`.
- FR-2: Criar `SensitiveDataMessageConverterTest` no mesmo pacote.
- FR-3: Usar apenas dependências já presentes em `spring-boot-starter-test` (JUnit 5, Mockito, spring-test) — nenhuma dependência nova no `pom.xml`.
- FR-4: Manter o gate JaCoCo em `0.80` (sem relaxar).

## 5. Non-Goals

- NÃO alterar código de produção (`CorrelationIdFilter`, `SensitiveDataMessageConverter`, `SensitiveDataMasker`).
- NÃO excluir `ConsolidadoApplication` (main) do JaCoCo — permanece sem teste; o bundle ≥ 80% é atingido sem excluí-la.
- NÃO tocar `repos/lancamentos` (PRD própria).
- NÃO adicionar testes de contrato CDC (já existem em `contract/`).
- NÃO incluir headers de licença em arquivos já versionados além dos novos testes.

## 6. Technical Considerations

- `CorrelationIdFilter` estende `OncePerRequestFilter`; testar via `MockHttpServletRequest`/`MockHttpServletResponse`/`MockFilterChain` (spring-test).
- `SensitiveDataMessageConverter` estende `MessageConverter` (logback); mockar `ILoggingEvent` com Mockito e asserir o retorno de `convert`.
- Cobertura coletada pelo `jacoco-maven-plugin` 0.8.12, gate `check` na fase `verify`.
- Todo arquivo de teste criado DEVE iniciar com o header de copyright proprietário (AGENTS.md §5.1).

## 7. Success Metrics

- Step "Testes B (consolidado)" do `quality.yml` verde: `mvn -B -U verify` com bundle ≥ 80%.
- Cobertura de `CorrelationIdFilter` e `SensitiveDataMessageConverter` ≈ 100%.

## 8. Open Questions

- Nenhuma — escopo fechado pelo gate de cobertura existente.
