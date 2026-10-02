# V32 — revisão da base V31

## Correções

- Mapa e Guerra usam o mesmo enriquecimento do controle regional. O mapa conserva os contadores previamente confirmados quando uma fonte omite um campo.
- Uma campanha antiga da Home não sobrescreve o controle ou o evento de invasão de uma leitura mais recente do mapa. O dossiê e o cartão flutuante usam o planeta selecionado dessa mesma leitura.
- Cache de abertura identifica regiões e contadores como dados salvos, conservando as datas originais. A leitura de abertura não substitui uma atualização concorrente na memória.
- Uma resposta regional com proprietário confirmado, mas sem disponibilidade, conserva a confirmação anterior do mesmo proprietário como leitura salva. Mudança de proprietário não herda disponibilidade do antigo.
- Alertas preservam a referência de regiões ausentes ou antigas. Campanhas salvas não geram defesa regional nova; proprietário desconhecido não gera alerta de planeta perdido.
- Atualizações da Home e do worker são serializadas; gravações do cache usam AtomicFile e exclusão mútua para preservar o arquivo válido em falhas de escrita.
- Consultas periódicas da Home acompanham o ciclo de vida visível da Activity. O worker de notificações continua com seu agendamento próprio.
- O envio de notificações trata a revogação da permissão pelo Android sem derrubar o app.
- JSON de atualização vazio, inválido ou com URL de download inválida mostra falha de verificação.

## Preservado

Navegação corrigida, gestos, assinatura permanente, preferências, notificações, DSS, Ordem Maior, Arsenal, comunidades, imagens regionais e temas. O pulso vermelho do mapa permanece nas coordenadas do planeta, acompanha zoom e deslocamento e pausa quando a tela sai do primeiro plano.

A animação dos números continua sendo apresentação calculada a partir de amostras reais comparáveis. Cache, alertas e resultados usam os dados confirmados; projeções não são persistidas.

## Publicação

1. Copie o conteúdo de Helldivers-BR-App para o repositório, incluindo arquivos novos e .github.
2. Faça commit/push e execute Actions → Build APK Helldivers BR.
3. Instale o APK assinado sobre a versão anterior e confirme o funcionamento.
4. Publique helldivers-br-v32.apk no mesmo diretório do R2.
5. Somente depois publique versao-app.json no R2. A URL desse JSON permanece a mesma.

O APK Release deve continuar sendo gerado no GitHub com os Secrets existentes. O ZIP não contém a chave privada.

## Conferência no aparelho

- Início → Mapa → dossiê de Martale: confira os estados das regiões e a separação do progresso planetário.
- Sem rede, confira a data da leitura salva; ao voltar, confira atualização sem repetição indevida de alerta.
- Confira um planeta sob ataque: pulso vermelho acompanhando zoom/arraste e seleção por toque.
- Vá para segundo plano, retorne e confira a atualização; teste também a rotação.
- Verifique a atualização depois de publicar APK e JSON no R2.

A validação de compilação e testes desta revisão está registrada ao final deste documento. Os testes de toque dependem de aparelho ou emulador e não são substituídos pela compilação.

## Validação concluída nesta revisão

- 64 testes unitários aprovados, sem falhas, erros ou testes ignorados.
- Compilação dos testes instrumentados Android aprovada.
- APK Debug V32.0.0, versionCode 32, compilado com sucesso.
- Android Lint aprovado: zero erros. Permanecem oito avisos de manutenção e quatro sugestões informativas da base, relacionados a convenções de Compose, recursos, ícone monocromático e imagens duplicadas.
- Build limpo e validação final com Gradle 8.7, JDK 17 e SDK Android 34.
- Testes instrumentados de toque não executados nesta sessão; o workflow do GitHub mantém essa etapa em emulador.
- Nenhum APK Release foi assinado nesta sessão. A assinatura permanente continua no workflow e nos Secrets do usuário.

Comandos verificados: `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:lintDebug`.

Pendência visual já prevista: `discord_oficial.webp` e `logo_whatsapp.webp` ainda têm o mesmo conteúdo na base. Foram preservados para a substituição futura pelas imagens definitivas.
