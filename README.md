# HELLDIVERS-BR — App Android nativo

Versão **4.0.0** do aplicativo Android do HELLDIVERS-BR, feita em **Kotlin + Jetpack Compose**.

## O que já está funcional nesta versão
- Home redesenhada com identidade do HELLDIVERS-BR.
- Terminais de acesso rápido: Central de Guerra, Mapa, Estratagemas, Warbonds e Facções.
- Ordem Maior com progresso, objetivos, recompensa e prazo.
- Resumo da Guerra Galáctica na Home.
- **Central de Guerra nativa** com telemetria, Helldivers no front, liberações, defesas e frentes ativas.
- Lista nativa das campanhas/planetas com progresso, facção, jogadores e prazo/regeneração.
- Busca por planeta/setor e filtros combinados por operação e facção inimiga.
- Despachos recentes do Alto Comando.
- Atualização automática a cada 60 segundos.
- Barra inferior com a seção ativa destacada em amarelo.
- Sistema de aviso de atualização do APK.
- Mapa e Arsenal continuam como etapa seguinte; por enquanto abrem a área correspondente do site.

## Dados
O app consome a API comunitária `api.helldivers2.dev` com os cabeçalhos de identificação recomendados e usa o snapshot público da Ordem Maior do projeto HELLDIVERS-BR como fallback.

## Gerar o APK pelo GitHub
1. Substitua o conteúdo do repositório `Helldivers-BR-App` pelos arquivos desta versão.
2. Abra **Actions → Build APK Helldivers BR → Run workflow**.
3. Quando o workflow terminar com o sinal verde, baixe **Helldivers-BR-apk** na área de Artifacts.
4. Dentro estará `Helldivers-BR.apk`.

## Releases e atualização automática
Ao publicar uma tag no formato `v4.0.0`, o workflow também anexa `Helldivers-BR.apk` à Release.
O aplicativo consulta o `versao-app.json` deste próprio repositório para detectar novas versões.

Para uma próxima versão, aumente `versionCode` e `versionName` em `app/build.gradle.kts` e atualize `versao-app.json`.

## Próxima etapa
Transformar **Mapa Galáctico** e **Arsenal/Estratagemas** em telas nativas, mantendo a mesma linguagem visual consolidada nas versões atuais.

## V3 — Central de Guerra visual
A V3 aproxima a experiência nativa da Central de Guerra do visual do site: campanhas com imagem do bioma, facção, condições ambientais, progresso, telemetria 2x2 e dossiê tático. A Home também ganhou uma frente em destaque com imagem e progresso. Consulte `CHANGES-V3.md` para os detalhes.


## V4 — Facções, busca e filtros
A V4 corrige a leitura de controle planetário e da facção inimiga, aplica cores/logos corretos nas frentes, identifica a facção-alvo nos objetivos da Ordem Maior e adiciona pesquisa e filtros combinados na Central de Guerra. Consulte `CHANGES-V4.md`.
