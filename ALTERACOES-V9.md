# V9 — mapa e Central de Guerra

- Filtros em linhas, separados em Exibição e Facções.
- Fundo e setores rasterizados uma vez por alteração de dados/opções; rotas e textos pré-calculados.
- Planetas fora da tela não são desenhados; rotas fora da tela são descartadas.
- Frente ativa com marcador maior e prioridade de nomes; colisões ocultam grupos de legendas.
- Aliados sem combate: ícones discretos e detalhes a partir de zoom 5; inimigos a partir de 2,3 e frentes de 1,6.
- Libertação: um aro de progresso; defesa: aro de progresso da defesa e aro de avanço temporal inimigo.
- Indicador discreto de regiões no mapa e contagem nos cartões/lista.
- Quatro fundos da Central de Guerra com os mesmos caminhos do CSS do site.
- Métricas táticas sem altura fixa de 90 dp, menos preenchimento e textos maiores.
- Ordem Maior mantém a seleção ativa/vitória/derrota conforme o estado confirmado pela fonte. Prazo expirado sem resultado permanece pendente.

Validação: análise sintática Kotlin, JSON, comparação de mudanças com V8 corrigida e integridade do ZIP.
Não houve compilação Android nem medição de FPS neste ambiente. Validar build e fluidez no aparelho.
