# V15 — paridade visual e alvos da Ordem Maior

Base V14-Corrigido, preservando a importação size em Components.kt.

- Catálogo compartilhado de alvos no Kotlin, transcrito de order-targets.js do ZIP Helldivers-BR(8): Atropeladores (2651633799) e Tanques Autômatos (2664856027). Aplicado no início, na aba Ordem e no cartão da Guerra. Outros IDs específicos são exibidos como não identificados, sem trocar por toda a facção.
- Metas seguem valueTypes 3 da API, com Long. Não há multiplicação de milhões para bilhões: o site fornecido documenta 25 milhões na missão de referência. Se a API trouxer 25 bilhões, o app preserva esse valor.
- Cartões compartilhados mais arredondados (18 dp); cartão principal da aba Ordem com 20 dp.
- Títulos externos das facções desenhados depois da transformação do mapa, em 14 sp fixos e mantendo o arco.
- Sem ícone de efetivo nos rótulos do mapa e no dossiê/regiões. Mantido em busca e ficha flutuante.
- Busca de planetas com miniatura no espaço de 28 dp existente.
- Ficha com paisagem deslocada para a faixa da barra de progresso, degradê nas duas extremidades e botões visualmente discretos com área de toque de 48 dp.
- Paisagem: ficha lateral, seleção enquadrada à direita, altura limitada com rolagem, barra inferior mais baixa. Retrato continua com ficha superior.
- Busca do menu consulta os 110 registros locais de estratagemas, além dos nomes das seções. Resultados abrem a ficha do equipamento no site. Não é uma busca de toda a wiki; o rótulo agora descreve o alcance real.
- Menu mantém a capa aprovada; versão no rodapé acompanha BuildConfig.

## Validação e limites

Kotlin analisado sintaticamente e ZIP comparado com V14-Corrigido. Incluídos testes para IDs, ordem dos valueTypes, metas de 25 bilhões e fallback de alvos desconhecidos. Os testes não puderam ser executados aqui: ambiente sem Gradle/SDK Android. Compilar no workflow existente e testar orientação/zoom/busca no aparelho. Não foi realizada renderização nativa local.

Esta versão continua usando o workflow debug existente. Assinatura permanente de distribuição segue pendente e não foi alterada nesta entrega.

## Aplicar

Substituir arquivos do projeto pelo conteúdo da pasta Helldivers-BR-App, incluindo .github, preservando demais pastas do repositório. Executar GitHub Actions. Versão 15.0.0 / versionCode 15.
