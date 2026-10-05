# Correção da compilação V40

A compilação Kotlin passou, mas o teste everyBundledAssetExists falhou porque a imagem de Pöpli IX não estava no repositório no caminho informado pelo catálogo.

Incluída a imagem original com nome sem acento: app/src/main/assets/map/planets/popli-ix-planet-icon.webp. Atualizado o caminho correspondente em map-planet-icons.json.

Todos os 367 caminhos dos dois catálogos apontam para arquivos existentes. O catálogo de planetas mantém 271 entradas. Projeto completo, baseado no commit 2c190bd2b9153a99a444f3d836f34072ccfd669a. Mantém versão 40 e os ajustes visuais.

Substitua o conteúdo do projeto por este ZIP. Execute novamente o workflow para repetir a suíte completa de 118 testes. Não foi realizada compilação completa Android neste ambiente.
