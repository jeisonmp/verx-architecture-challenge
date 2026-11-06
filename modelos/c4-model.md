# 1.9 - C4 Model: Uma arquitetura, quatro níveis de abstração

**Sistema: Verx - Challenge**

---

## O diagrama que não explicava nada

> A Verx - Challenge recebe uma proposta de investimento e o investidor pede um diagrama da arquitetura.

A Verx - Challenge recebeu uma proposta de investimento, e o investidor pediu um diagrama da arquitetura. Roberto enviou um desenho cheio de setas, caixas sobrepostas e siglas sem legenda. A resposta veio curta: *não entendi nada, mas parece complexo*. 

Ana percebeu que o problema não era a arquitetura, era a comunicação dela. Um modelo simples de quatro níveis teria resolvido aquilo em quinze minutos, e talvez mudado o tom da conversa de investimento.

---

## Quatro níveis para públicos diferentes
*Do contexto de negócio ao código*

*   **Context (Contexto):** Mostra o sistema e quem interage com ele. É o nível que um CEO ou investidor entende.
*   **Container:** Mostra as aplicações e os bancos de dados. Evidencia onde o código roda e como as partes se conectam.
*   **Component (Componente):** Detalha o que existe dentro de um container. Revela os blocos internos e suas responsabilidades.
*   **Code (Código):** Chega ao desenho de classes. É raramente necessário, sendo útil só onde a complexidade interna justifica o esforço.

---

## Qual nível para qual conversa?

### Nível alto (Negócio)
*   **Context** é voltado para investidores e diretoria.
*   Foco em entrega de valor e integrações externas.
*   Sem jargão técnico ou menção a ferramentas específicas.
*   Uma imagem que representa a visão inteira.

### Nível baixo (Engenharia)
*   **Container** e **Component** são voltados para o time técnico.
*   Foco em tecnologia e delegação de responsabilidade.
*   Utiliza nomes reais de serviços, bancos de dados e protocolos.
*   Contém detalhe suficiente para implementar a solução.

---

## Os fundamentos em uma página

*   Arquitetura é decisão estrutural, não apenas um diagrama.
*   Cada decisão técnica é, no fundo, uma decisão de negócio.
*   O arquiteto define a visão geral e atua para não virar um gargalo.
*   Atributos de qualidade transformam-se em requisitos mensuráveis.
*   Todo *trade-off* sacrifica algo: faça escolhas conscientes.
*   **ADR** (Architecture Decision Records) e **C4 Model** preservam e comunicam essas decisões.

---