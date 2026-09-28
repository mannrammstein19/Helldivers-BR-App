# HELLDIVERS-BR — V7.0.0

Base: ZIP da V6 enviado pelo Daryl. Referência: Helldivers-BR(7).zip, de 28/09/2026.

## Arsenal
- Substitui categorias com contagens fixas por 110 registros extraídos do HTML do site.
- Categorias reais: 12 Orbitais, 7 Águia, 33 Armas de Apoio, 13 Mochilas, 8 Veículos, 10 Sentinelas, 8 Plataformas e 19 Estratagemas de Missão.
- Pesquisa sem distinção de acentos, filtro por categoria e favoritos persistidos no aparelho.
- Códigos de ativação, recarga, custo, nível e fonte disponíveis offline.
- Caminhos e informações preservados conforme o site recebido, sem correção editorial das estatísticas.
- Imagens SVG com suporte Coil SVG. Imagens continuam remotas, sem necessidade de enviar as pastas pesadas.
- Fichas individuais abrem no navegador pelo link específico. Os 19 registros sem link no HTML não ganham links inventados.
- O catálogo é uma cópia gerada no momento desta versão, não uma sincronização automática com o site. O importador em tools permite regenerá-lo a partir do HTML atualizado.

## Mapa nativo
- Substitui a tela provisória por Canvas em Compose, com coordenadas reais da API.
- Zoom com dois dedos e botões, arraste, centralização, seleção de planeta e lista acessível por toque.
- Filtros por nome/setor, facção e frentes ativas; rotas de suprimento opcionais.
- Dossiê compacto com setor, dono, jogadores, progresso da campanha e bioma quando disponível.
- Mantém última leitura em memória em falhas e mostra horário/status; consulta a cada 60 s enquanto a tela está STARTED.
- Mapa online: não há persistência dos planetas após encerrar o processo.
- Ainda não replica contornos de setores, territórios, invasões, imagens de facção nos planetas nem efeitos/chefões/buracos negros especiais do mapa web. Acesso ao mapa completo do site mantido.

## Facções e navegação
- Tela Compose com as quatro facções, textos e caminhos de imagem do site recebido.
- Acesso pela Home e pelo menu; dossiês completos continuam no site.
- Busca do menu ignora acentos; busca de “catálogo” passa a revelar a opção correspondente.
- Mantidas Home, Guerra, Ordem Maior, compactação da V6, ícones e tema Meridia.
- Atualizações manuais repetidas da Home não disparam consultas concorrentes.

## Verificação e limites
- 110 registros comparados com um segundo parser HTML: nomes, recargas, custos, níveis, fontes e links correspondem ao original.
- Sintaxe de todos os arquivos Kotlin verificada com tree-sitter-kotlin; isso não é compilação nem validação de tipos/dependências.
- JSON e XML analisados; diferenças revisadas com git diff --check.
- Não foi possível compilar ou executar o APK neste ambiente: sem SDK Android/Gradle; tentativa de obtenção do Gradle não concluída.
- É necessário executar o GitHub Actions e testar no aparelho, em especial gestos do mapa, carregamento da API, SVGs e tamanhos de fonte.
- Nenhuma alteração foi enviada ao repositório remoto ou publicada.
