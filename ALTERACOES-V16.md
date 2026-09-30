# V16 — catálogo de alvos específicos da Ordem Maior

Base: V15, preservando os ajustes visuais e a correção da importação `size`.

- Catálogo ampliado de 2 para 12 IDs (10 novos), pesquisados no interpretador comunitário hd2api.py. Inclui Hulk, Devastator, Bile Titan e Voteless.
- Aplicação compartilhada no Início, Guerra e Ordem. Não foi adicionada uma API nem uma consulta de rede.
- A aba Ordem também resolve um alvo conhecido quando a API não informa a facção.
- Mantidos os valores `Long` recebidos da API, sem converter milhões em bilhões.
- Mantida a proteção para IDs desconhecidos ou com facção incompatível.
- Lista completa em `CATALOGO-ALVOS-ORDEM.md` e `catalogo-alvos-ordem.csv`, com fontes fixadas por commit e limites das variantes.
- Atribuição MIT incorporada em `app/src/main/assets/licenses/hd2api.txt`.
- Versão do aplicativo: 16.0.0 / versionCode 16.

## Validação realizada

Compilados na JVM com Kotlin 1.9.24 os arquivos reais `Models.kt`, `OrderTargets.kt` e `OrderTargetsTest.kt`, usando as bibliotecas de serialização 1.6.3 e JUnit 4.13.2. Executados com sucesso 6 testes, cobrindo os 12 IDs, campos reordenados, distinção entre ID de unidade/equipamento/planeta, facção ausente/zero/incompatível, IDs desconhecidos, arrays incompletos e metas de 25 milhões e 25 bilhões.

Os 30 arquivos Kotlin também passaram pela análise sintática. Isso não substitui o build Android: o APK completo e as telas Compose não foram compilados/renderizados aqui, pois o ambiente não possui SDK Android. O workflow existente continua executando os testes e compilando o APK.

## Aplicação

Copiar o conteúdo da pasta `Helldivers-BR-App` para a raiz do repositório Android, incluindo `.github`, e executar o workflow existente. Este ZIP é código-fonte, não APK. Os ajustes só chegam aos usuários depois da compilação e da instalação do novo APK.

Não altera o site nem publica downloads. O arquivo de atualização remota `versao-app.json` e o link de download foram preservados; publique o APK e ajuste esse manifesto apenas quando a versão estiver disponível. A assinatura permanente continua pendente, como na V15.
