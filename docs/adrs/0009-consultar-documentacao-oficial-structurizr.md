---
status: accepted
date: 2026-11-06
decision-makers: Max
---
# 0009. Consultar a documentação oficial do Structurizr antes de qualquer alteração no modelo C4

## Contexto e problema

O `workspace.dsl` passou por erros sucessivos de parse ao subir o container (`StructurizrDslParserException`): primeiro por identificadores duplicados (`api`/`db` em escopo flat) e depois por keys de view com caracteres fora do conjunto `a-zA-Z0-9_-`. Ambos decorreram de editar o modelo sem validar a sintaxe contra a referência oficial da DSL. A documentação oficial não estava disponível localmente, forçando consultas externas durante cada correção.

## Histórico da discussão (sessão de iteração — Q&A)

- **Q:** o container acusa `The identifier "api" is already in use`. **A:** no modo `flat` (padrão), todo identifier é global; `api` e `db` estavam declarados em dois software systems. Corrigido habilitando `!identifiers hierarchical`, que escopa identifiers ao elemento pai e valida as referências pontilhadas (`lancamentos.api`, `consolidado.db`, …) já usadas no arquivo.
- **Q:** novo erro `View keys can only contain the following characters: a-zA-Z0-9_-` em `systemLandscape "Visão do Ecossistema"`. **A:** o string posicional estava sendo interpretado como key, e keys de view não aceitam acentos/unicode. Corrigido definindo keys ASCII explícitas (`ecossistema`, `contexto-lancamentos`, …) e movendo o texto legível para a posição de descrição.
- **Q:** `theme default` é válido no modo `local`? **A:** não — o modo `local` não tem temas instalados por padrão e `default` não é um tema pré-construído; a estilização padrão já é aplicada automaticamente. A linha foi removida.

## Decisão

Vender a documentação oficial do Structurizr em `docs/refs/structurizr.github.io` (cópia do repositório oficial) e tornar obrigatória a consulta a essa referência **antes de qualquer alteração** em `workspace.dsl` ou em qualquer outro artefato Structurizr/DSL. Regras-chave consolidadas a partir dessa referência:

- Identificadores de elemento têm escopo `flat` por padrão (globais); usar `!identifiers hierarchical` quando o mesmo nome é reutilizado sob pais diferentes.
- Keys de view aceitam apenas `a-zA-Z0-9_-`; o texto legível pertence à posição de descrição, nunca à key.
- `theme`/`themes` resolvem por nome instalado, URL ou arquivo; no modo `local` nenhum tema vem pré-instalado.
- `!adrs <path>` exige caminho relativo dentro do diretório do arquivo pai.

## Consequências

* Bom, porque elimina erros de parse recorrentes e round-trips de correção, tornando a edição do modelo determinística.
* Bom, porque a referência fica versionada no repositório, disponível offline e revisável em pull request.
* Ruim, porque adiciona uma dependência de documentação vendored que precisa ser mantida em sincronia com a imagem do Structurizr.
* Ruim, porque a regra é processual e depende de o editor consultá-la antes de cada mudança.
