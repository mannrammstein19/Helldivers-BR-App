# HELLDIVERS-BR App — V2

## Interface
- Nova Home inspirada na versão mobile do site HELLDIVERS-BR.
- Nova Central de Guerra 100% nativa em Jetpack Compose.
- Navegação inferior redesenhada com indicador amarelo na tela atual.
- Paleta, cards, bordas, tipografia e hierarquia visual aproximadas do terminal militar do site.

## Dados
- Campanhas ao vivo via `/api/v1/campaigns`.
- Ordem Maior via `/api/v1/assignments` + snapshot do HELLDIVERS-BR.
- Despachos via `/api/v2/dispatches`.
- Suporte ao progresso de campanhas baseado em regiões.
- Requisições principais carregadas em paralelo e atualização automática a cada 60 segundos.

## Projeto
- `versionCode = 2`
- `versionName = 2.0.0`
- Coil Compose adicionado para imagens remotas dos terminais.
- Workflow atualizado para `actions/setup-java@v5`.
- Metadados de atualização passaram a apontar para o repositório correto do app.
