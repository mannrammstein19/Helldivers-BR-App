# V33 — interface e notícias Steam (patch sobre V32)

## Como aplicar sem trocar o projeto inteiro

1. Use o projeto V32 como base e guarde um backup.
2. Extraia o patch. Copie o conteúdo da pasta `Helldivers-BR-App` para a raiz do seu projeto existente, aceitando substituir os arquivos correspondentes. Não apague a pasta atual.
3. Inclua os arquivos novos no commit/push e execute o workflow habitual do GitHub para gerar o APK Release.
4. Instale e confira no celular antes de publicar. Use o mesmo certificado permanente.
5. Depois de testar, suba `helldivers-br-v33.apk` no R2. Só então publique `versao-app.json` na pasta habitual do R2.

Não existe arquivo a excluir. Este patch não altera workflows, Secrets, applicationId nem a assinatura. Não é necessário mudar as configurações do Actions. A correção de resultados da Ordem Maior no repositório do site permanece separada e deve continuar habilitada.

O patch contém apenas arquivos alterados e novos em relação ao ZIP V32 revisado. Ele não inclui APK, chaves, cache do Gradle ou arquivos locais de SDK.

## Ajustes aplicados

- Aviso inicial do gesto em cinza chumbo opaco, com demonstração em cinza escuro e o mesmo botão ENTENDI. Mantida a regra de aparecer uma vez.
- Tema padrão: imagem de fundo mais visível, com menos escurecimento. Meridian conserva a paleta, imagem e valores de transparência anteriores.
- Terceiro tema: Modo noturno. Fundo cinza chumbo sem wallpaper; Arsenal também não adiciona seu fundo decorativo. As ilustrações dos cartões, planetas e equipamentos continuam disponíveis.
- Configurações: seletor com Padrão, Meridian e Modo noturno. O menu lateral percorre os três temas, com preferência salva.
- Ícones dos controles de Configurações preenchem suas molduras de 38 dp. Logos de comunidade continuam respeitando a proporção original para não cortar imagens retangulares.
- Canais de alerta com ícones vetoriais distintos, prontos para receber PNGs personalizados.
- Ordem Maior recolhida na entrada da Guerra. A aba Ordem mantém sua apresentação própria.
- Quatro filtros de facção em uma linha: Todas, Insetos, Robôs e Iluminados, com símbolos e área de toque mínima. As informações dos planetas mantêm os nomes completos.
- Notícias Steam abaixo de Despachos recentes, seguindo o painel do site: três notícias, título, data e abertura da publicação. Títulos usam o mecanismo existente de tradução para PT-BR.

## Ícones personalizados dos alertas

Antes de compilar, se quiser substituir os ícones vetoriais, coloque imagens quadradas na pasta `app/src/main/assets/icones-alertas/`:

- `planetas.png`
- `regioes.png`
- `noticias.png`
- `ordem-maior.png`
- `dss.png`

Sugestão: 256 × 256 ou 512 × 512, fundo transparente e pouca margem interna. Os ícones vetoriais funcionam enquanto as imagens não forem adicionadas; não é obrigatório preparar imagens para compilar esta versão.

## Steam e proteção do carregamento

O site foi consultado como referência: `guerra.js`, função `renderSteam`, usa três notícias de `/api/v1/steam`, com intervalo de 15 minutos. O app tenta essa mesma fonte e, em falha, usa a API pública GetNewsForApp v2 da Steam para o app 553850, com o feed de anúncios da comunidade. A resposta direta foi obtida nesta revisão e seu formato entrou nos testes.

Documentação da API pública: https://partner.steamgames.com/doc/webapi/ISteamNews

As notícias possuem cache separado, preservando a data da consulta e da publicação. A atualização é independente da Ordem Maior e da Guerra; falhar na Steam não impede o carregamento principal. Este painel não acrescenta um novo canal de notificações push.

## Recursos preservados

Paredão Community → API direta → cache; datas dos dados salvos; confirmação regional por proprietário/disponibilidade; resultados da Ordem Maior; proteção dos alertas; navegação e gesto corrigidos; pulso dos planetas invadidos; DSS; Arsenal; preferências e temas existentes.

A revisão comparou com a V32 e confirmou, byte a byte, a preservação do workflow, OrderRepository, RegionTelemetry, WarAlertManager, GalaxyCanvas e da imagem local Meridian.

## Conferência no celular

- Configurações: selecione os três temas, feche e abra o app e confira o tema escolhido.
- Meridian: confira o visual aprovado. Padrão: confira a visibilidade do fundo. Noturno: confira o fundo sem wallpaper, inclusive no Arsenal.
- Confira se os ícones preenchem as molduras e se os logos retangulares continuam completos.
- Guerra: confirme Ordem recolhida, expansão por toque e quatro filtros na mesma linha.
- Início: role após os despachos e confira as notícias Steam; toque em uma publicação.
- Após uma leitura válida, desligue a rede e confira o cache sem perda das informações.
- Confira Início, gesto do menu, rotação e pulsos no mapa, preservados da base.

A validação automática está registrada ao final deste documento. A inspeção visual no aparelho continua necessária.

## Validação final

- 74 testes unitários aprovados, sem falhas, erros ou testes ignorados.
- Testes instrumentados Android compilados. Não executados nesta sessão; o workflow mantém sua etapa em emulador.
- Compilação limpa do APK Debug V33.0.0, versionCode 33, concluída.
- Android Lint concluído: zero erros, oito avisos de manutenção da base e quatro sugestões informativas.
- Gradle 8.7, JDK 17 e SDK Android 34. Comando: `:app:clean :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:lintDebug`.
- O APK Release assinado deve ser gerado no GitHub. Nenhuma chave privada foi usada ou incluída neste patch.

### Verificação opcional da base

O ZIP inclui, fora da pasta do projeto, `verificar-base-v33.py`. Se já tiver Python, pode executar `python verificar-base-v33.py "CAMINHO_DO_PROJETO_COMPLETO"` antes de copiar o patch. Ele só lê os arquivos e compara a base V32 ou os arquivos V33 já aplicados; não modifica nada. Não é obrigatório para compilar.

Se você tiver feito alterações próprias nos mesmos arquivos depois da V32, elas precisam ser comparadas antes da substituição. Um patch reduz o número de substituições, mas ainda contém o arquivo completo de cada item alterado.

## Arquivos incluídos no patch

- `ALTERACOES-V33.md` — novo
- `README.md` — alterado
- `app/build.gradle.kts` — alterado
- `app/src/main/assets/icones-alertas/LEIA-ME.md` — novo
- `app/src/main/java/br/com/helldiversbr/app/MainActivity.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/data/HelldiversApi.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/data/SteamNewsRepository.kt` — novo
- `app/src/main/java/br/com/helldiversbr/app/data/TelemetryCache.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/AppFirstRun.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/MainViewModel.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/screens/ArsenalScreen.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/screens/HomeScreen.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/screens/NotificationSettingsScreen.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/screens/SettingsScreen.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/screens/SteamNewsPanel.kt` — novo
- `app/src/main/java/br/com/helldiversbr/app/ui/screens/WarScreen.kt` — alterado
- `app/src/main/java/br/com/helldiversbr/app/ui/theme/Theme.kt` — alterado
- `app/src/main/res/drawable-nodpi/steam_logo.png` — novo
- `app/src/test/java/br/com/helldiversbr/app/SteamNewsTest.kt` — novo
- `app/src/test/java/br/com/helldiversbr/app/ThemeModesTest.kt` — novo
- `versao-app.json` — alterado
