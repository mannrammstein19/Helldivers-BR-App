# V10 — navegação, telemetria e Arsenal

Base: V9. Alterações pontuais, com catálogo e recursos anteriores preservados.

## Início
Mantém Ordem Maior e Seu próximo destino. Retira a frente em destaque e Situação da Guerra. Exibe até cinco despachos recentes.

## Guerra
Identidade estável dos cartões por planeta, inclusive quando a ordenação muda. A mesma lista permanece composta entre sucesso e erro com dados anteriores.
Fontes que falham preservam os últimos dados válidos em memória, com aviso. Resposta válida vazia continua sendo respeitada. O cache dura a sessão, não é armazenamento permanente offline.
Logos nas opções de facção, filtros visíveis em linhas, botões de operação mais compactos.

## Ordem
Imagem sem escurecimento sobre a maior parte da capa; degradê apenas na base.
Ritmo e previsão abre por toque. Calcula variação após duas leituras válidas separadas por pelo menos 30 segundos; atualização automática a cada 60 segundos. Sem avanço positivo, não inventa uma data de conclusão. Telemetria antiga pausa a previsão.

## Mapa e regiões
Colisão mede cada texto com a fonte efetiva, incluindo população, percentuais, invasão, regiões e DSS. Seleção tem prioridade sem ignorar colisões. Textos desenhados após os marcadores.
Frentes abaixo do mapa reutilizam os cartões completos da Guerra e abrem dossiê.
Somente regiões explicitamente disponíveis para operação são listadas e contadas.

## Arsenal
Permissões e categorias expansíveis, cores e fundo do site, contagem por categoria.
Favoritos e busca preservados; busca/favoritos abrem automaticamente categorias com resultados.
Setas, requisições e medalhas usam os caminhos originais do site. Custos gratuitos não recebem símbolo de moeda.
Códigos ausentes continuam identificados; não foram inventados códigos nem estatísticas. Recarga, nível, aquisição e ficha completa disponíveis.

## Validação e limites
Análise sintática dos arquivos Kotlin/KTS, validação JSON, caminhos novos comparados com o site enviado, catálogo de 110 registros preservado, ZIP verificado.
Não houve compilação Android, execução dos testes Gradle nem inspeção visual no aparelho neste ambiente.
Validar no GitHub Actions e no celular: permanecer rolado na Guerra durante atualizações; abrir previsão após duas leituras; zoom com todas as facções; expandir categorias e testar favoritos.
