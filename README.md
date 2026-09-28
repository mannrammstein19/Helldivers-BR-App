# HELLDIVERS-BR — Android V7.0.0

Projeto Kotlin + Jetpack Compose, evoluído a partir da V6 enviada. Este pacote contém código-fonte; não contém APK compilado.

## Novidades
- Arsenal nativo: 110 registros reais do site, códigos, aquisição, busca, categorias e favoritos.
- Mapa nativo: planetas da API, zoom, arraste, seleção, rotas, pesquisa e filtros.
- Facções nativas com conteúdo do site e acesso aos dossiês completos.
- Base da V6 preservada: Home, Guerra, Ordem Maior, temas, menu compacto e ícones originais.

Veja CHANGES-V7.md para escopo completo, recursos ainda exclusivos do site e limites de validação.

## Gerar o APK no GitHub
1. Faça backup da V6.
2. Copie o conteúdo da pasta Helldivers-BR-App deste ZIP para a raiz do repositório do aplicativo, mantendo a pasta .github/workflows incluída no ZIP.
3. Confirme o envio dos arquivos alterados e novos, incluindo app/src/main/assets e tools.
4. Abra Actions → Build APK Helldivers BR → Run workflow.
5. Se a compilação terminar com sucesso, baixe Helldivers-BR-apk em Artifacts. Dentro estará Helldivers-BR.apk.

Não publique tag/release antes de validar o APK no celular. O fluxo existente produz APK debug.

## O que depende de internet
Telemetria, mapa, imagens e fichas completas. O catálogo de estratagemas, textos de facções e favoritos funcionam sem rede. As últimas leituras de telemetria são mantidas apenas em memória.

## Atualizar o catálogo a partir do site
Use Python 3, sem bibliotecas adicionais:

```sh
python tools/import_site_catalog.py /caminho/estratagemas.html
```

O comando gera app/src/main/assets/stratagems.json. Assim, os valores são editados no site e importados para o app. As alterações entram no próximo APK; não há sincronização automática do catálogo em instalações existentes.

## Verificação no aparelho
- Buscar “canhao” e conferir resultados com acentos; filtrar e favoritar, reiniciar e confirmar favorito.
- Abrir uma ficha específica; verificar nomes/códigos/imagens SVG e registros de missão sem link.
- Abrir o mapa; arrastar, ampliar com dois dedos, centralizar, filtrar e selecionar pela lista.
- Interromper a conexão após uma leitura; verificar aviso e conservação dos dados do mapa.
- Abrir Facções pela Home e menu; mudar entre Padrão e Meridia.
- Conferir Guerra e Ordem Maior da V6, rotação, fonte ampliada e botão Voltar.

A sintaxe foi analisada localmente, mas este pacote ainda requer compilação pelo Actions e teste Android.
