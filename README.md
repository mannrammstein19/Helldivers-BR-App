# HELLDIVERS-BR — App Android nativo

Versão **6.0.0** do aplicativo Android do HELLDIVERS-BR, feita em **Kotlin + Jetpack Compose**.

## O que já está funcional nesta versão
- Home nativa inspirada diretamente no visual mobile do HELLDIVERS-BR.
- Terminais com imagem para Central de Guerra, Mapa, Estratagemas, Warbonds e Facções.
- **Barra inferior com 6 itens**: Início, Guerra, Ordem, Mapa, Arsenal e Menu.
- Ícones da barra carregados do próprio HELLDIVERS-BR, sem sobreposição com ícones genéricos.
- **Menu lateral nativo compacto**, com logo, busca de opções, tema e grupos inspirados no menu mobile do site.
- **Tema Meridia** ativável no menu e salvo no Android.
- **Ordem Maior em tela própria**, com banner, prazo, medalhas, briefing e objetivos por facção.
- **Arsenal nativo** com hero visual, permissões Ofensiva/Suprimento/Defensiva e categorias expansíveis.
- Central de Guerra nativa com telemetria, Helldivers no front, liberações, defesas e frentes ativas.
- Campanhas/planetas com imagens de bioma, dono/facção correta, cores semânticas, jogadores, progresso, pressão e dossiê tático.
- Pesquisa por planeta/setor e filtros combinados por operação e facção inimiga.
- Despachos recentes do Alto Comando.
- Atualização automática a cada 60 segundos.
- Sistema de aviso de atualização do APK.

## Dados
O app consome a API comunitária `api.helldivers2.dev` com os cabeçalhos de identificação recomendados e usa o snapshot público da Ordem Maior do projeto HELLDIVERS-BR como fallback.

## Gerar o APK pelo GitHub
1. Substitua o conteúdo do repositório `Helldivers-BR-App` pelos arquivos desta versão.
2. Abra **Actions → Build APK Helldivers BR → Run workflow**.
3. Quando o workflow terminar com o sinal verde, baixe **Helldivers-BR-apk** na área de Artifacts.
4. Dentro estará `Helldivers-BR.apk`.

## Releases e atualização automática
Ao publicar uma tag no formato `v6.0.0`, o workflow também anexa `Helldivers-BR.apk` à Release.
O aplicativo consulta o `versao-app.json` deste próprio repositório para detectar novas versões.

Para uma próxima versão, aumente `versionCode` e `versionName` em `app/build.gradle.kts` e atualize `versao-app.json`.

## Evolução
- `CHANGES-V3.md`: Central de Guerra visual e dossiê tático.
- `CHANGES-V4.md`: facções/donos corrigidos, pesquisa e filtros.
- `CHANGES-V5.md`: shell visual, barra inferior, drawer, Meridia, Ordem dedicada e Arsenal nativo.
- `CHANGES-V6.md`: densidade visual, ícones originais sem sobreposição e drawer inspirado no mobile do site.

## Próximas etapas
- Portar o **Mapa Galáctico** para Compose/nativo preservando a lógica do mapa do site.
- Migrar as fichas individuais de Estratagemas para o app, mantendo os mesmos dados do HELLDIVERS-BR.
