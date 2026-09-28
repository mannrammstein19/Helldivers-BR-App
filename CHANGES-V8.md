# V8.0.0 — Mapa e Ordem Maior

Base: V7 instalada pelo Daryl. Fonte visual: mapa-classico.js, planet-regions.js, guerra.js, overview.js e order-rewards.js do ZIP Helldivers-BR(7).zip.

## Correções da Ordem Maior
- O banner genérico foi substituído pelos três caminhos exatos definidos no site, selecionados pelo estado da ordem.
- Ativa/aguardando: imagens/fundos/major-order/major-order-ativa.png.
- Vitória: imagens/fundos/major-order/major-order-vitoria.png.
- Derrota: imagens/fundos/major-order/major-order-derrota.png.
- Recompensas usam icons/recompensas/{tipo}.svg, incluindo medalhas.svg, com decodificação SVG global.
- Identificação de recompensa segue a ordem de regras do site, sem chamar todo item especial de medalha.
- Expiração sem resultado confirmado passa a “aguardando”; um snapshot terminal da mesma ordem não volta a ativo por uma leitura atrasada.
- O mecanismo completo de confirmação por despachos do JS ainda não foi portado; vitória/derrota dependem do estado do snapshot.

## Mapa nativo
- 55 contornos SVG copiados sem alteração geométrica para sectors.json.
- Fundo original, setores, territórios por controle/invasão, rotas, setas de invasão e filtros.
- Ícone do dono dentro da bolinha, anel de defesa azul e libertação amarelo; invasão e atacante separados.
- Capital central, imagem personalizada da Super Terra, Penta/Meridia, destroços, névoa decorativa de Cyberstan e marcadores editoriais de Omicron.
- Marcador da DSS quando a API informa seu planeta.
- Consulta de posições/ataques reais; planetas desativados ou sem coordenadas válidas não se acumulam sobre a capital.
- Imagens carregadas uma vez por tela/cache compartilhado; geometrias preparadas fora da rotina de desenho. Mantidos zoom/arraste nativos.
- Dossiê completo com condições e regiões acessível pelo planeta selecionado.
- Bordas arredondadas preservadas.

## Regiões
- O tipo visual vem do catálogo de 367 hashes do site; não é deduzido da facção do planeta.
- Assentamento, vila, cidade, megacidade e megafábrica recebem os ícones correspondentes.
- Controle, disponibilidade, saúde e progresso próprios da região; região fechada não é automaticamente uma conquista.
- O dossiê apresenta todas as regiões retornadas, inclusive as indisponíveis, com status explícito. O painel web original filtra as regiões disponíveis em operação.

## Navegação e escopo
- Retirado o cabeçalho superior “Menu de navegação”. Drawer e botão Menu inferior continuam disponíveis.
- Arsenal mantido para revisão visual posterior, conforme a prioridade indicada.

## Conferência antes da compilação
- CONFIRIR-IMAGENS.html é gerado do mesmo cadastro site-assets.json que o app usa. Abre no Chrome e mostra carregamento/dimensões/link original por recurso.
- O sinal verde significa imagem decodificada pelo navegador; a adequação da arte ao estado exige inspeção visual.
- Solicitações locais às três artes e à medalha receberam HTTP 403. Não foi possível inspecionar suas imagens aqui; isso não comprova arquivo ausente.
- Não são usadas artes inventadas para ocultar falhas de carregamento.

## Validação e limites
- Análise sintática de Kotlin/Gradle e verificação de JSON/XML.
- Conferência dos caminhos contra o site; comparação dos 55 contornos e dos 367 hashes com a fonte original.
- Testes unitários adicionados para estados de arte, recompensas, regiões, capital/coordenadas e prioridade defesa/libertação. O workflow roda testDebugUnitTest antes de assembleDebug.
- Testes unitários, compilação Android e comparação visual em aparelho NÃO executados neste ambiente, sem SDK/Gradle.
- Não há declaração de igualdade visual absoluta: layout dos controles, tipografia/escala, animações e posicionamento do dossiê são nativos e ainda exigem comparação com o site. O mapa web completo segue acessível.
