# V22 — Revisão 2

Base: `Helldivers-BR-App-V22-REVISADA.zip`.

## Navegação
- Botão **Início** agora volta para a instância raiz já existente no back stack (`popBackStack("inicio", false)`) em vez de remover/recriar a rota.
- Ao retornar para Início a telemetria continua sendo revalidada sem descartar o último snapshot válido.

## Barra inferior
- Altura reduzida no retrato de 80dp para 68dp.
- Ícone e nome ficaram mais próximos, preservando boa leitura e área de toque.
- Configurações permanece marcada quando a tela interna de Notificações está aberta.

## Configurações
- Visual reorganizado em blocos compactos, inspirado na hierarquia das referências enviadas sem copiar o layout.
- Versão do app visível no topo.
- Área **Atualizações** com verificação manual, indicação de versão disponível e opção de verificar ao abrir o app.
- Área **Preferências** com acesso à nova central de Notificações e seletor de tema.
- Área **Comunidade** com compartilhar app, site, Discord, WhatsApp, YouTube e feedback/desenvolvedor.
- Área **Sobre** compacta preservando `DarylDixon_19` e os créditos do projeto.

## Central de Notificações
- Interruptor geral de notificações.
- Preferências individuais e por grupo para:
  - Planetas: nova campanha, liberado, sob ataque, defendido e perdido.
  - Regiões: sob ataque, liberada, defesa e perdida.
  - Notícias.
  - Ordem Principal: nova ordem, progresso e conclusão.
  - DSS: realocação, ação tática ativa e votação de realocação.
- Contador `ativos/total` por grupo.
- Atalho para as configurações de notificações do Android.
- Migração da antiga preferência de alerta de invasão.
- Primeira leitura após ativação vira baseline e não gera alertas antigos.
- Trabalho periódico antigo `war-invasion-watch` é cancelado para impedir verificação duplicada.

## Telemetria usada pelos alertas
- `HomeData` agora preserva também a lista completa de planetas para detectar mudanças de proprietário e regiões.
- Planetas seguem o mesmo paredão: Community API -> API direta -> cache persistente.
- Dados marcados como cache/stale não avançam baselines de alertas.
- Progresso de Ordem notifica por faixas de 10% somente quando esse canal está ativado.

## Atualizações
- `UpdateChecker` diferencia: atualização disponível, versão atual e falha de consulta.
- A preferência "Verificar ao abrir" passa a controlar a consulta automática no `MainViewModel`.
- O botão de download usa o `apkUrl` publicado em `versao-app.json`.

## Observações
- O Pix não foi incluído porque nenhuma chave Pix foi fornecida; não foi criado dado fictício.
- A seção/aba "Dados" também não foi criada ainda porque o conteúdo definitivo não foi especificado; o Arsenal atual foi preservado.

## Limites desta revisão
- Alertas de **região** são derivados das transições de proprietário/vida/disponibilidade presentes na telemetria recebida. Eles não dependem de um feed privado de eventos do Diver Hub, portanto a disponibilidade real varia conforme o que a API expuser.
- O download de atualização ainda abre o `apkUrl` publicado; o instalador interno no estilo Telegram (download + validação + chamada do instalador Android) fica para a etapa seguinte.
- Esta revisão recebeu validação estrutural/estática e de integridade do ZIP; a compilação Android completa deve ser confirmada pelo GitHub Actions/Android Studio, pois este ambiente não possui o Android SDK configurado.
