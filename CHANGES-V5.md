# HELLDIVERS-BR App — V5.0.0

## Salto visual / shell do aplicativo
- Nova barra superior compartilhada com **MENU DE NAVEGAÇÃO**, linha e indicador na cor do tema.
- Barra inferior remodelada com **6 itens**: Início, Guerra, Ordem, Mapa, Arsenal e Menu.
- A barra usa os mesmos arquivos de ícone publicados pelo HELLDIVERS-BR (`/icons/inicio.png`, `guerra.png`, `ordem.png`, `mapa.png`, `arsenal.png`, `menu.png`) e mantém fallback nativo caso algum asset remoto falhe.
- Estado ativo aparece destacado e acompanha a rota atual; Menu também recebe estado ativo enquanto o drawer está aberto.

## Menu lateral nativo
- Drawer lateral em Jetpack Compose, sem WebView.
- Atalhos para Início, Central de Guerra, Ordem Maior, Mapa e Arsenal.
- Atalhos para Warbonds, Facções e site completo.
- Seção de tema integrada.

## Tema Meridia
- Alternância entre **Padrão Helldivers** e **Meridia**.
- Preferência salva localmente no Android e restaurada ao abrir o app.
- Meridia troca fundo, superfícies, bordas e cor principal para a identidade roxa e usa o visual do Void/Meridia como backdrop.
- Cores das facções permanecem semânticas: Terminídeos laranja, Autômatos vermelho, Iluminados roxo e Super Terra azul.

## Ordem Maior
- Nova rota **Ordem** no app.
- Tela dedicada com banner, tempo restante, objetivos concluídos, medalhas, briefing e objetivos separados.
- Objetivos de eliminação identificam a facção-alvo e herdam logo/cor correta.
- Cards de objetivo com progresso, contagem e status.

## Arsenal
- Arsenal deixa de ser apenas tela provisória.
- Hero nativo inspirado no terminal mobile do site.
- Seções Ofensiva, Suprimento e Defensiva.
- Categorias expansíveis com contagens e acesso ao catálogo completo do site durante a migração das fichas individuais para Compose.

## Preservado da V4
- API, telemetria, atualização automática e sistema de update do APK.
- Central de Guerra com pesquisa, filtros por operação e facção, imagens de planeta, dossiê tático, dono/facção correta e telemetria.
- Home com carrossel visual, Ordem Maior, frente em destaque e estatísticas.
- Bordas arredondadas e linguagem visual já aprovada.
