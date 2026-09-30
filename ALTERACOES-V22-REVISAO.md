# HELLDIVERS-BR App — V22 Revisada

Esta revisão mantém `versionCode = 22` e `versionName = 22.0.0`, mas corrige pontos que ainda não estavam de acordo com o objetivo original da V22.

## Navegação
- O retorno para **Início** não restaura mais uma entrada antiga da Navigation Compose.
- Ao sair de outra aba e tocar em **Início**, o destino raiz é reconstruído usando o mesmo `MainViewModel` e uma revalidação é disparada sem apagar o último snapshot válido.

## Barra inferior
- Barra um pouco mais alta no modo retrato.
- Ícones das abas maiores.
- Nome das abas maior e com mais espaço vertical.
- Modo paisagem continua compacto.

## Ordem Maior
- Na Home, o emblema da facção foi ampliado sem ampliar o texto do objetivo.
- Na aba Ordem, o emblema da facção também foi ampliado e o texto principal voltou a uma hierarquia menor.
- Na Central de Guerra, cada objetivo expandido agora tem demarcação própria com borda/cor da facção e cabeçalho de facção inimiga.

## Central de Guerra / filtros
- Busca, tipo de operação e facção inimiga foram reunidos em um único painel visual.
- Filtros de facção deixaram de ficar espremidos em uma única linha e agora usam grade 2x2.
- Ícones e nomes das facções ficaram maiores e alinhados.
- Foi adicionado comando **LIMPAR** quando algum filtro estiver ativo.

## DSS
- O card da DSS na Central de Guerra ficou mais alto de verdade, usando proporção 4:3 no modo alto.
- A arte/modelo vertical da estação passa a ocupar mais altura do card, sem aumentar a largura da seção.

## Paredão de dados
- API direta tenta `/WarInfo` primeiro e mantém `/Info` apenas como compatibilidade.
- Parser bruto agora aceita diferenças de capitalização (`Id/id`, `Message/message`, `Published/published`, etc.).
- Respostas HTTP vazias ou sem conteúdo útil deixam de ser consideradas telemetria válida para campanhas/despachos.
- Se Community e Direct não entregarem conteúdo válido, o app mantém o último cache confiável.
- A Central de Guerra pode exibir **TELEMETRIA MISTA** quando módulos diferentes estiverem vindo de fontes diferentes.
- Leituras de campanhas completamente vazias não apagam o baseline de alertas de invasão.
