# V39 — GIFs originais e painel compacto de telemetria

Base: V38 entregue nesta conversa.

## Alterações

- “?” agora abre um painel flutuante no canto inferior esquerdo, acima dos botões.
  Ocupa 78% da largura do mapa, limitado a 420 dp, e até 600 dp de altura.
  O conteúdo rola em telas menores. O mapa permanece visível ao redor.
- Aparência baseada nas duas prints: fundo #0E181F, borda arredondada, títulos
  amarelos, datas em verde claro, fontes em azul e avisos em dourado.
- Seções: Ordem Maior, DSS, Planetas e regiões, Planetas e Despachos.
  Os horários exibidos são os registros reais disponíveis, no fuso do aparelho.
  Quando não existe registro específico, o painel informa a ausência.
- “Tentar atualizar” usa a atualização existente, sem consultas extras automáticas.
  “Preferências” abre as opções já existentes. “×”, o “?” e Voltar fecham o painel.
- Os GIFs originais de Downloads.zip estão embutidos no APK e preservados byte a
  byte: Meridia — 151 quadros / 10,07 s; Penta — 300 quadros / 9 s.
- Reprodução dos quadros originais em loop; sem rotação/pulsação artificial dos
  buracos negros. A rotação dos destroços e as outras animações foram preservadas.
- GIFs acompanham zoom/arraste e continuam entre atualizações e com dados salvos.
  A preferência de movimento e a configuração do Android continuam respeitadas.
  O fundo preto é mesclado ao mapa pela composição existente.

## Instalação

Copie todo o conteúdo de Helldivers-BR-App/ para a raiz do projeto Kotlin,
substituindo os arquivos e incluindo os novos. A versão já está em 39 / 39.0.0.
Gere o APK pelo workflow existente. O JSON público de atualização não foi alterado.

## Validação

- Seis testes JUnit passaram: relógio contínuo (18.000 quadros/5 minutos), fontes,
  retomada, horários/fuso, identificação de cache e temporização/loop dos GIFs.
- Classe que renderiza GIFs compilada contra a API Android; sintaxe dos arquivos
  alterados validada pelo parser Kotlin. Referências e integridade dos GIFs
  verificadas, além de git diff --check e integridade do ZIP.
- Não foi compilado APK completo nem executada a interface num aparelho neste
  ambiente. Esses testes não validam o desempenho visual da decodificação no celular.
- Não houve envio ao GitHub ou publicação.
