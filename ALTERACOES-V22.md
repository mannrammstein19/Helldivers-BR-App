# HELLDIVERS-BR App — V22

## Paredão de telemetria

A V22 transforma a telemetria em uma cadeia de três níveis, sem trocar a API comunitária como fonte principal:

1. **Community API** — leitura normal do app.
2. **API direta do jogo** — consultada apenas quando a Community API falha.
3. **Cache persistente local** — última leitura válida gravada no aparelho, usada quando as duas fontes de rede não respondem.

### Segurança dos dados
- Nova `DirectGameApi` para Ordem Maior, despachos, guerra/campanhas, planetas e localização básica da DSS.
- Novo `TelemetryCache` persistente em `filesDir/telemetry-cache`.
- O cache guarda Home/Central de Guerra, DSS e Mapa Galáctico.
- Ao abrir o app sem rede, a última leitura salva pode aparecer imediatamente enquanto uma nova leitura é tentada.
- Dados recuperados do cache preservam o **horário verdadeiro da última leitura**; o app não cria um horário novo para dados antigos.
- O cabeçalho da Central de Guerra distingue `TELEMETRIA ONLINE`, `TELEMETRIA DIRETA` e `ÚLTIMA LEITURA SALVA`.
- Cache antigo não dispara alerta falso de invasão planetária.
- A API direta é fallback; ela não substitui a Community API como fonte normal.

## Navegação
- Corrigido o retorno para **Início** pela barra inferior depois de visitar Guerra, Ordem, Mapa ou Arsenal.
- A navegação preserva estado das abas sem bloquear o destino inicial.

## Ícone e nome no launcher
- Nome exibido no launcher reduzido para **HD2 BR**, evitando corte em launchers mais estreitos.
- `ic_launcher_foreground.png` reenquadrado dentro de uma área segura, preservando a arte escolhida pelo projeto e reduzindo o efeito de ícone excessivamente ampliado.

## Início
- `Tempo restante` e `Objetivos concluídos` ficaram mais compactos no topo da Ordem Maior.
- Menos espaçamento interno nos objetivos.
- Ícone da facção inimiga ampliado.
- `Ritmo observado` e `Conclusão estimada` agora ficam lado a lado, seguindo a referência visual da aba Ordem.
- Despachos ganharam cards mais compactos e menor distância entre texto, idioma e ação de leitura.

## Central de Guerra
- Objetivos expandidos da Ordem Maior mostram `Ritmo observado` e `Conclusão estimada` lado a lado.
- Filtros de facção foram compactados para uma única linha: Todas, Terminídeos, Autômatos e Iluminados.
- O painel da DSS ganhou mais altura na Central de Guerra para valorizar a arte vertical, mantendo a versão do Mapa mais compacta.

## DSS e Mapa
- DSS também participa do cache persistente e do fallback direto quando possível.
- Mapa Galáctico passa a usar Community API -> API direta -> cache local.
- Catálogo planetário salvo é reutilizado no fallback para preservar nomes e setores.

## Versão
- `versionCode = 22`
- `versionName = 22.0.0`
