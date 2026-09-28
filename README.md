# Helldivers BR — App Android nativo (Kotlin + Jetpack Compose)

## Fase 1 (esta versão)
- Estrutura do app, tema visual do site (amarelo `#D7D52C` sobre `#090909`)
- Navegação inferior: Início · Guerra · Mapa · Arsenal
- **Início**: Ordem Maior (ao vivo + snapshot do `dados/major-order.json`) e Despachos
- Atualização automática dos dados a cada 60 s
- **Aviso de nova versão do APK** (lê `versao-app.json` do seu site)
- Guerra / Mapa / Arsenal: telas provisórias que abrem a página equivalente do site

## Como gerar o APK (sem instalar nada no PC)
1. Crie um repositório novo no GitHub (ex.: `Helldivers-BR-App`) e envie **todo o conteúdo desta pasta**.
2. Aba **Actions → Build APK Helldivers BR → Run workflow**.
3. Ao terminar (5-10 min), baixe o `Helldivers-BR-apk` em *Artifacts*.
4. Para ter um link fixo de download (para WhatsApp/Discord): crie uma tag `v1.0.0`
   (`git tag v1.0.0 && git push origin v1.0.0`) — o APK vai para **Releases**.

## Aviso de atualização dentro do app
O app consulta `https://mannrammstein19.github.io/Helldivers-BR/versao-app.json`.
1. Copie o arquivo `versao-app.json` desta pasta para a **raiz do repositório do site** (`Helldivers-BR`).
2. Ao lançar uma versão nova: aumente `versionCode`/`versionName` em `app/build.gradle.kts`,
   gere o APK e atualize o `versao-app.json` do site com o novo `versionCode`, `versionName` e `notes`.
3. Quem já tem o app instalado verá o banner "Nova versão disponível" e baixa o APK novo.

## Ainda não testado
Este código foi escrito sem poder compilar no ambiente de desenvolvimento.
O primeiro build no GitHub Actions pode acusar erros de compilação; envie o log e eu corrijo.
