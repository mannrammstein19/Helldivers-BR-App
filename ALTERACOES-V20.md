# HELLDIVERS-BR App — V20

Base: V19. Alterações pontuais; o restante do projeto foi preservado.

## Ordem Maior
- Cartão da Ordem Maior na tela **Início** reorganizado no padrão visual do site mobile.
- Tempo restante e objetivos concluídos em caixas separadas.
- Cada objetivo passa a mostrar ritmo observado e conclusão estimada em blocos próprios.
- Na tela **Ordem**, ritmo e previsão ficam lado a lado e os dados de tempo/objetivos também recebem caixas arredondadas.
- Na **Central de Guerra**, a Ordem Maior inicia recolhida; o usuário expande os objetivos pelo botão do próprio cartão.

## Central de Guerra
- Cards de efetivo/frentes/liberações/defesas mais arredondados.
- Imagens de fundo mais visíveis, mantendo texto legível.
- Cor de cada card concentrada na borda superior e esquerda, em vez de uma faixa atravessando todo o card.
- O card **Defesas** pulsa em vermelho enquanto houver defesa planetária ativa.
- Filtros de facção reorganizados em duas linhas para manter **Iluminados** alinhado com **Autômatos**.

## Alertas de invasão
- Nova opção **Configurações > Alertas de Guerra > Alerta de Invasão Planetária**.
- No Android 13+, a permissão de notificações é solicitada quando o alerta é ativado.
- Com o app aberto, novas defesas são comparadas com a telemetria recebida pelo app.
- Em segundo plano, o WorkManager executa verificações periódicas com rede disponível.
- A primeira leitura após ativar o recurso vira referência para não notificar como “nova” uma defesa que já estava em andamento.

## DSS — primeira fase
- O Mapa Galáctico agora lê o objeto completo da DSS pelo endpoint v2 de estações espaciais.
- O marcador da DSS continua acompanhando o planeta hospedeiro.
- Novo botão contextual da DSS no mapa.
- Ao tocar, abre um painel inferior com localização, eleição, ações táticas, estado das ações, contribuição/progresso, taxa por hora e prazo quando disponível.
- Caso a telemetria da DSS não esteja disponível, o mapa continua funcionando e o painel informa o estado indisponível.

## Versão
- `versionCode`: 20
- `versionName`: 20.0.0
- `versao-app.json`: atualizado para V20.

## Não incluído nesta etapa
A parede de dados **Community API → API direta/bruta → cache persistente com horário real** permanece como uma etapa técnica separada, para não misturar uma alteração de infraestrutura com este pacote visual/DSS/notificações.
