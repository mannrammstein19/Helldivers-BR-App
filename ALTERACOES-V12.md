# V12 — mapa dedicado e ficha flutuante

Base: V11. Alterações limitadas a GalaxyScreen.kt, GalaxyCanvas.kt e versão do app.

- A aba Mapa ocupa toda a área disponível acima da navegação inferior, respeitando o espaço das barras do sistema.
- Cabeçalho compacto e controles sobrepostos. A tela não rola mais para uma lista abaixo da galáxia.
- Buscar abre a lista em um painel; selecionar um resultado localiza o planeta no mapa.
- Tocar num planeta abre uma ficha flutuante recolhível: efetivo com ícone de capacete, progresso, ritmo líquido, previsão, invasão quando disponível, regiões operacionais e acesso ao dossiê existente.
- Ficha com altura limitada e rolagem própria para telas menores. Voltar fecha a ficha antes de sair da aba.
- Todos os planetas desligado por padrão: mantém inimigos, campanhas/defesas, vizinhos de rotas e ataques, Super Terra, DSS e planetas especiais. Esta é uma regra própria, não uma reprodução confirmada do filtro do DiversHub.
- Todos os planetas pode ser ligado no canto inferior ou nos filtros. O atalho inferior também limpa a busca/facção para mostrar todos.
- Preservados filtros de facção, frentes ativas, rotas, invasões, territórios e setores. Busca e facção explícita permitem consultar planetas humanos fora do recorte contextual; Frentes ativas continua restringindo a campanhas/defesas.
- Atualizações da API não recentralizam a câmera. Selecionar um novo planeta ajusta o enquadramento abaixo da ficha.
- Previsões indisponíveis quando a fonte de campanhas falha; ausência de ritmo positivo não inventa prazo de vitória.
- Letras continuam desenhadas em pixels de tela com verificação de colisões.

## Limites desta entrega

A perspectiva continua vista de cima. Não há implementação 2.5D nesta versão; esta etapa entrega a navegação imersiva e a ficha flutuante, com os recursos visuais próprios do HELLDIVERS-BR.

Verificação local: análise de sintaxe Kotlin e integridade do ZIP. Não foi possível compilar ou executar Android neste ambiente (sem Gradle/SDK). A compilação e a validação visual final precisam ocorrer no GitHub Actions e no aparelho. Não é APK já compilado.

## Instalação do projeto

Substitua os arquivos do projeto pelo conteúdo da pasta Helldivers-BR-App no ZIP, incluindo .github, mantendo as pastas adicionais que só existam no seu repositório. Execute o workflow de APK como nas versões anteriores.

## Conferência no aparelho

1. Abrir Mapa: a galáxia deve preencher a aba, com menu inferior acessível.
2. Alternar Todos e Frentes ativas; verificar facções, rotas e invasões.
3. Buscar um planeta, selecionar, recolher/expandir/fechar a ficha e abrir o dossiê.
4. Fazer pinça/arrastar e aguardar uma atualização: o enquadramento deve ser preservado.
5. Conferir uma libertação, uma defesa e um planeta com regiões disponíveis.
