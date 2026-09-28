# HELLDIVERS-BR App V4

## Central de Guerra
- Corrige a distinção entre **controle do planeta** e **facção inimiga da frente**.
- Em libertação, o card usa o dono atual do planeta (Terminídeos, Autômatos ou Iluminados) para logo, nome e cor.
- Em defesa, mantém a Super Terra como controle atual e mostra separadamente a facção atacante.
- Barras de libertação seguem a facção inimiga: Terminídeos = laranja, Autômatos = vermelho, Iluminados = roxo.
- Barra da invasão em defesas usa a cor da facção atacante.
- Dossiê tático agora mostra Controle Atual, Facção Inimiga/Atacante, setor, bioma e telemetria.

## Busca e filtros
- Pesquisa por nome do planeta, setor ou nome da facção.
- Filtro por tipo: Todas / Libertação / Defesa.
- Filtro por facção inimiga: Terminídeos / Autômatos / Iluminados.
- Todos os filtros funcionam em conjunto.

## Ordem Maior
- Lê o campo `valueType 1` das tarefas para identificar a raça/facção alvo.
- Objetivos de eliminação deixam de aparecer todos como “Eliminar forças inimigas” quando a API informa a facção.
- Cada objetivo recebe cor e logotipo da facção correspondente.

## Home
- Frente em Destaque agora mostra logotipo e nome da facção inimiga e herda sua cor.

## Versão
- versionCode: 4
- versionName: 4.0.0
