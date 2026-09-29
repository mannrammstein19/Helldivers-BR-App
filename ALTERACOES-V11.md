# V11 — ajustes visuais mobile

Base: V10 corrigida.
- Início reorganizado conforme referência mobile: abertura com destaque amarelo, carrossel mais alto com indicadores clicáveis, resumo da Ordem sem capa grande, objetivos/ritmo/previsão, link para aba Ordem e cartão Discord. Despachos preservados.
- Arsenal compacto: remove categoria repetida dentro de cada equipamento, botão Ver aquisição e botão amarelo de largura total. Origem sempre visível; link Detalhes discreto. Favoritos, setas e custos originais preservados.
- Mapa: atalho Frentes ativas e painel Filtros com todas as camadas/facções; controles de zoom sobre o mapa.
- Nomes, soldados e percentuais desenhados em pixels de tela após restaurar o canvas, fora do zoom. Colisões medidas em pixels e maior distância entre linhas. Prioridade das frentes e do planeta selecionado preservada.
- Cabeçalho dos planetas mais compacto: menos preenchimento e sem espaço adicional de fonte no nome/setor. Indicadores inferiores inalterados.

Validação: análise sintática Kotlin/KTS, inspeção das chamadas alteradas e integridade do ZIP. Não compilado com Android/Gradle neste ambiente. Sem teste visual nativo ou medição de FPS. Não se afirma equivalência de 99% sem comparação no aparelho.
