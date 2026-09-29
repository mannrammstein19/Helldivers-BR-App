# V13 — refinamento visual do mapa e dossiê

Base: V12 aprovada no aparelho.

- Filtros de facção com logos e cores próprias; seleção realça o chip. O botão Filtros também mostra o logo da facção escolhida.
- Fechar e recolher/expandir a ficha têm símbolos brancos, fundo escuro e área de toque de 48 dp, com rótulos de acessibilidade.
- Ficha flutuante com a mesma imagem de paisagem usada no dossiê, resolvida pelo mapeamento existente de planeta e catálogo, e degradê para legibilidade. Recolher oculta a imagem e os detalhes.
- Dossiê: menos padding e separação entre fatos, com alturas de linha explícitas. Mantidas imagem, condições e informações existentes.
- Regiões: cartões compactos; nome/tipo/status juntos, progresso e efetivo preservados. Continua exibindo apenas regiões disponíveis para operação.
- Capa do menu: imagens/fundos/site/wallpaper_principal_4_helldivers.png no domínio https://helldivers-br.pages.dev. Esse arquivo precisa estar publicado no site. O ícone instalado do aplicativo não foi alterado.

## Capacete e efetivo

O capacete da ficha usa a imagem icons/recompensas/capacete.svg do site, via chave reward_capacete. É um ícone visual. O número vem de statistics.playerCount do planeta (com dados de campanha quando disponíveis), abreviado pelo formatador existente. Não representa quantidade de itens/capacetes; representa Helldivers naquele planeta na última leitura da API.

## Validação

Verificados sintaxe Kotlin, diferenças em relação à V12 e integridade do ZIP. Sem SDK/Gradle disponível para compilar ou testar visualmente no Android neste ambiente. Executar o workflow do GitHub Actions e conferir no aparelho: filtros, ficha aberta/recolhida, dossiê com regiões e capa do menu.

## Aplicação

Substituir os arquivos do projeto pelo conteúdo de Helldivers-BR-App no ZIP, incluindo .github, preservando pastas adicionais do seu repositório. Versão 13.0.0, versionCode 13.
