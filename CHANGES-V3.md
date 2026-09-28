# HELLDIVERS-BR App — V3

## Paridade visual com a Central de Guerra web
- Cards de campanhas refeitos para seguir a estrutura visual do site.
- Imagem grande do planeta/bioma em cada frente usando os mesmos assets publicados no HELLDIVERS-BR.
- Cabeçalho com LIBERTAÇÃO/DEFESA, estado da frente e prazo/estimativa.
- Ícone e nome da facção no cabeçalho.
- Condições ambientais exibidas sobre a imagem com os ícones do site.
- Barras separadas para defesa Helldiver e invasão inimiga quando a campanha é de defesa.
- Grade 2x2 de métricas: Helldivers, avanço líquido, pressão inimiga e estimativa/prazo.
- Rodapé com bioma e tendência.
- Toque no card abre um Dossiê Tático nativo com imagem, setor, facção, bioma, controle e condições planetárias.

## Dados
- Catálogo de planetas passou a carregar bioma, environmentals e weather_effects além dos nomes.
- Pressão inimiga de libertação agora é convertida de regenPerSecond para %/h, como no site.
- Defesas calculam o relógio da invasão e a pressão inimiga por hora a partir do intervalo do evento.
- O app guarda amostras de progresso em memória e calcula avanço líquido por hora após nova leitura (atualização automática a cada 60 s).
- Estimativa de vitória usa o ritmo observado; na primeira leitura aparece "coletando" até haver histórico suficiente.

## Home
- Nova Frente em Destaque abaixo da Ordem Maior, com imagem real do bioma, progresso e efetivo.
- Mantidos os terminais com imagens e a identidade visual já aprovada na V2.

## Projeto
- versionCode = 3
- versionName = 3.0.0
- Sem WebView para Home/Central de Guerra: interface permanece 100% Jetpack Compose nativo.
