# GITFLOW — Norma de Versionamento e Branches (backforend / holding)

> Norma operacional única de gitflow da holding Backforend (Client-Zero). Documento
> **não-CI**: rege o versionamento de tags e o fluxo de branches da holding.
> Padrão **replicado** do motor `ralph-engine` (`ralph-engine/docs/GITFLOW.md`), que
> por sua vez o embute nos projetos filhos. Fonte da decisão:
> `ralph-engine/docs/adr/ADR-003-semantic-release-conventional-commits-gitflow.md`.

## 1. Modelo de branches

| Branch | Papel | Release no merge |
| --- | --- | --- |
| `master` | produção (estável) | `vX.Y.Z` → GitHub Release estável |
| `dev` | integração / pré-release | `vX.Y.Z-beta.N` → GitHub pre-release |
| `feat/<x>`, `fix/<x>` | efêmeras | nenhum (build local apenas) |

## 2. Fluxo obrigatório: toda mudança entra por Pull Request

Não há push direto em `dev` nem em `master`. Toda mudança entra por **PR**:

| De (origem) | Para (destino) | Quando | Efeito após o merge |
| --- | --- | --- | --- |
| `feat/<x>` / `fix/<x>` | `dev` | feature/fix pronta para integrar | push em `dev` → **pré-release** `vX.Y.Z-beta.N` |
| `dev` | `master` | promover release estável | push em `master` → **release estável** `vX.Y.Z` |

- Branches efêmeras (`feat/`/`fix/`) são deletadas após o merge.
- `master` só recebe código vindo de `dev` (PR `dev` → `master`).

### 2.1 O título da PR é o commit que o semantic-release lê

O semantic-release deriva a versão dos **commits**, não do nome da branch nem do
título da PR em si. No merge por **squash**, o **título da PR vira a mensagem do
commit** que entra em `dev`. Por isso o título da PR precisa ser um **Conventional
Commit** (§3) — no mesmo formato da mensagem de commit — e **não** o nome da branch.

> Se o merge não for squash (merge commit ou rebase), a regra recai sobre a
> mensagem de **cada commit**: todos devem ser conventional commits. Em qualquer
> caso, manter o título da PR como conventional commit é o caminho seguro.

| Título da PR | Válido? | Por quê |
| --- | --- | --- |
| `Feat/semantic release gitflow` (padrão do GitHub) | ❌ | não tem `feat:`; o parser não reconhece → sem bump de versão |
| `feat(release): implementa semantic-release e gitflow` | ✅ | conventional commit válido → MINOR |

## 3. Conventional Commits (mensagem e título da PR)

Formato — partes entre `[ ]` são opcionais; o restante é obrigatório:

```
<tipo>(<escopo>)?[!]?: <descrição>

[corpo]

[rodapé]
```

| Parte | Obrigatória? | Regra |
| --- | --- | --- |
| `<tipo>` | **sim** | minúsculo: `feat`, `fix`, `docs`, `chore`, `refactor`, `perf`, `test`, `ci` |
| `(<escopo>)` | não | rótulo livre, minúsculo, indicando a área afetada — **não altera a versão** |
| `!` | não | só para quebra de contrato (MAJOR) |
| `: <descrição>` | **sim** | dois-pontos + espaço após o tipo/escopo |
| `[corpo]`, `[rodapé]` | não | detalhes; `BREAKING CHANGE:` no rodapé também gera MAJOR |

### SemVer por tipo

| Tipo | SemVer | Uso |
| --- | --- | --- |
| `feat` | MINOR | nova funcionalidade retrocompatível |
| `fix` | PATCH | correção de bug |
| `docs`, `chore`, `refactor`, `perf`, `test`, `ci` | — | sem bump de versão |
| `BREAKING CHANGE:` (rodapé) ou `!` no tipo | MAJOR | quebra de contrato/API |

### Exemplos

```
feat(release): implementa semantic-release e gitflow
feat(holding): adiciona pipeline de versionamento de tags
fix(pipeline): corrige gatilho de push em master
feat!: remove flag --legacy (breaking)
```

### O merge em `dev` faz release?

**Sim — pré-release.** Todo merge em `dev` gera um `push` em `dev`, que dispara o
`release.yml` e o semantic-release publica um **pré-release** `vX.Y.Z-beta.N`.
O release estável (`vX.Y.Z`) só acontece no merge em `master`.

- A PR trouxe um `feat:` → `dev` sobe MINOR.
- Trouxe só `fix:` → `dev` sobe PATCH.
- Trouxe só `docs`/`chore`/`ci`/etc → **nenhuma** nova versão é publicada.
- O sufixo `-beta.N` incrementa a cada novo pré-release em `dev` (`.1`, `.2`, …).

## 4. Semantic Release (dono da versão e do CHANGELOG)

- A versão é derivada dos commits desde a última tag (`commit-analyzer` +
  `release-notes-generator`).
- O `CHANGELOG.md` é gerado (`@semantic-release/changelog`) e commitado
  (`@semantic-release/git`, com `[skip ci]`), e a tag `vX.Y.Z` é criada.
- A GitHub Release é criada (`@semantic-release/github`): estável em `master`,
  pre-release em `dev`.
- Config: `.releaserc.json` (`branches: [master, dev(prerelease: beta)]`).

> **Migração de prefixo de tag:** as tags legadas da holding usam o prefixo `pub/`
> (`pub/vX.Y.Z`). A partir da adoção do semantic-release, as novas tags usam o
> prefixo `v` (`vX.Y.Z`), alinhado ao motor. As tags `pub/*` permanecem como
> histórico e **não** são regeradas.

## 5. Pipeline (`release.yml`)

Um único workflow, disparado por `push` em `master`/`dev`, com um único job:

1. `release` — semantic-release (versão/CHANGELOG/tag/Release).

A holding não embarca imagens OCI nem binários Go: os jobs `build-push`
(Docker/cosign) e `release-cli` (binários Go) existem apenas no `ralph-engine`
e **não** se aplicam aqui. O build roda no mesmo run da tag — não há workflow
secundário a disparar (resolve a restrição anti-loop do `GITHUB_TOKEN`).

## 6. Proibições

- ❌ Versionamento manual (`vX.Y.Z` decidido à mão).
- ❌ `git tag`/`git push --tags` manual (humano ou agente).
- ❌ Push direto em `dev` ou `master` (toda mudança entra por PR).
- ❌ Título de PR fora do padrão Conventional Commits (vira o commit no squash).
- ❌ Edição manual do `CHANGELOG.md`.
- ❌ Geração de fragmentos `.changelog/`.

O agente IA escreve **apenas** conventional commits; versão, CHANGELOG, tag e
Release são responsabilidade exclusiva do semantic-release no CI.

## 7. Canais de release

| Tag | GitHub Release |
| --- | --- |
| `vX.Y.Z` (estável) | sim (release estável) |
| `vX.Y.Z-beta.N` | pre-release |
