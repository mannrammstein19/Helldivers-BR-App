# V30 — abertura e alerta visual no mapa

Base: V29 Regiões. Mantém contadores, progressos independentes do planeta e regiões, Configurações, navegação Início e assinatura permanente.

## Abertura
- Usa a imagem Helldiver Beneath Super Earth.png enviada pelo usuário, incluída localmente no APK como startup_helldiver.png. Não precisa baixar a imagem na abertura.
- Logo atual do aplicativo centralizado, renderizado separadamente da arte.
- Degradê escuro inferior, nome HELLDIVERS-BR e mensagens de inicialização, telemetria e preparação da Central de Guerra.
- Zoom visual suave de 1,00x a 1,045x; tela de abertura dura cerca de quatro segundos por criação do ViewModel, sem reaparecer ao alternar abas ou apenas retornar do segundo plano.
- A animação e sua barra indicam a apresentação de abertura, não uma porcentagem de download ou de resposta da API.
- Cache e consulta inicial começam imediatamente no ViewModel existente. Nenhuma nova consulta foi adicionada. Após quatro segundos, entra na tela normal, com dados disponíveis ou o carregamento/erro já existente. A rede lenta não prolonga a abertura.
- Layout adaptado para orientação horizontal, com logo e espaçamentos menores.

## Mapa
- Planetas com evento de defesa recebido da API recebem halo e anel vermelho pulsantes. Planetas desabilitados, locais especiais e libertações comuns não recebem o pulso.
- Alerta segue a posição do planeta, zoom e arraste. As cores de facção e os anéis de progresso existentes permanecem.
- Animação executada em camada separada e leve, a 20 atualizações por segundo, apenas quando há um alvo de defesa visível no filtro. Suspende fora do estado STARTED da tela.
- Não executa chamadas extras à API e não gera novas notificações. Se os dados forem salvos, o evento continua representando a última leitura disponível.

## Publicar
1. Envie o conteúdo da pasta Helldivers-BR-App para a raiz do repositório, incluindo os arquivos novos e .github/workflows.
2. Execute Actions → Build APK Helldivers BR → Run workflow, mantendo os Secrets de assinatura existentes.
3. Instale o APK Release assinado e confira abertura, orientação, Início, toques no mapa, zoom e pulso durante uma defesa.
4. Após aprovação, publique helldivers-br-v30.apk em helldivers-br/apps/ no R2.
5. Só depois do APK disponível, publique versao-app.json desta versão em helldivers-br/apps/versao-app.json no R2.

A validação visual em aparelho e os testes no emulador continuam necessários. O APK local de verificação é Debug; a atualização das instalações existentes deve usar o Release assinado do GitHub.

Validação local: 44 testes unitários aprovados, compilação limpa do APK Debug aprovada e testes de navegação compilados. O novo teste distingue defesa, libertação, planeta desabilitado e local especial.
