# Catálogo de alvos da Ordem Maior — V16

Pesquisa conferida em 30/09/2026. Atualizado em 08/10/2026: **14 IDs no aplicativo.**
Este é um catálogo comunitário parcial, não uma lista oficial completa de inimigos.

## Origem verificável

- Catálogo `enemies` do projeto hd2api.py: https://github.com/CrosswaveOmega/hd2api.py/blob/0ff67eade7c2d4f4db45ea225f41b865dd515b26/src/hd2api/constants/constants.py
- Uso desse catálogo ao interpretar `enemyID` nas tarefas: https://github.com/CrosswaveOmega/hd2api.py/blob/0ff67eade7c2d4f4db45ea225f41b865dd515b26/src/hd2api/models/Task2.py
- Documentação do campo: https://github.com/helldivers-2/json/blob/c7425990a1ef5891005b0ecb387fbea5def471b7/assignments/tasks/task/valueTypes.json (`4 = unit_id`, `3 = goal`, `1 = race`).
- Voteless também aparece no registro bruto coletado da ordem 3655441578, tarefa de erradicação, valueType 4 = 4211847317: https://www.helldiversstats.com/major-orders/3655441578
- Atropeladores e Tanques Autômatos já constavam em `order-targets.js` do site enviado (comentário referente à ordem 1715805482 e ao despacho 3941).

## IDs incorporados

| ID da unidade | Nome exibido no app | Nome na fonte | Facção |
| --- | --- | --- | --- |
| 20706814 | Batedores Andantes | Scout Strider | Autômatos |
| 2664856027 | Tanques Autômatos | Shredder Tank | Autômatos |
| 471929602 | Hulks | Hulk | Autômatos |
| 4276710272 | Devastadores | Devastator | Autômatos |
| 878778730 | Soldados Autômatos | Trooper | Autômatos |
| 3330362068 | Caçadores | Hunter | Terminídeos |
| 2058088313 | Guerreiros | Warrior | Terminídeos |
| 2387277009 | Espreitadores | Stalker | Terminídeos |
| 2651633799 | Atropeladores | Charger | Terminídeos |
| 2514244534 | Titãs de Bile | Bile Titan | Terminídeos |
| 1379865898 | Cuspidores de Bile | Bile Spewer | Terminídeos |
| 4211847317 | Sem-voto | Voteless | Iluminados |

Os nomes em português são rótulos editoriais do app; não foram certificados como a localização oficial do jogo. Os nomes originais estão acima para permitir revisão.

### Escopo das famílias e variantes

Mantido o rótulo anterior **Tanques Autômatos**: a fonte comunitária chama 2664856027 de Shredder Tank, enquanto o site fornecido usava Tanques Autômatos. Não restringimos a ordem a uma variante com base apenas nessa diferença de nome. O mesmo cuidado vale para Hulk: esta tabela não prova quais variantes o jogo contabiliza em cada ordem. O aplicativo apenas apresenta o alvo e o progresso recebidos; não calcula mortes por variante.

## Como funciona

- Início, Guerra e Ordem consultam `OrderTargets.kt`.
- O ID é lido procurando `4` em `valueTypes`, usando o mesmo índice em `values`.
- A facção, quando informada, deve corresponder ao alvo. Facção ausente ou zero permite resolver o ID conhecido.
- ID ausente/zero mantém o nome da facção. Um ID específico desconhecido ou incompatível exibe “alvo específico não identificado”.
- Quantidade vem de `valueTypes: 3`, em `Long`: 25 milhões e 25 bilhões permanecem valores distintos. Não há multiplicação nem meta fixa.
- A identificação também funciona na aba Ordem quando a tarefa traz um ID conhecido sem informar a facção.
- Os IDs ficam disponíveis localmente no aplicativo, sem uma chamada de rede adicional.
- Novos IDs ainda exigem atualização do catálogo e do APK. Não foi implementado carregamento remoto deste catálogo.

## Limites e manutenção

O catálogo não cobre todos os inimigos e variantes do jogo. Nomes de inimigos, IDs de equipamentos e hashes encontrados sem relação comprovada com tarefas não foram adicionados.
Para ampliar: registrar a fonte ou uma tarefa real com ID + briefing inequívoco, atualizar `OrderTargets.kt`, esta lista/CSV e os testes. Não deduzir IDs pela posição fixa no JSON.

A referência consultada tem licença MIT; atribuição e licença seguem no APK em `assets/licenses/hd2api.txt`. A lógica Kotlin foi implementada no projeto.

## Atualização da Ordem 4152388944 (08/10/2026)

| ID da unidade | Nome exibido | Facção | Meta observada (somente evidência) |
| --- | --- | --- | --- |
| 717622970 | Bile Spewers | Terminídeos | 30.000.000 |
| 444529084 | Bile Spitters | Terminídeos | 40.000.000 |

Fonte dos IDs: `dados/major-order.json` do site, commit 4b33d78, Ordem 4152388944, campos valueTypes 4 e 1. Fonte dos nomes: print do Helldivers Companion enviado pelo desenvolvedor em 08/10/2026, com os mesmos dois objetivos e o controle de Senge 23. O vínculo nome/ID é uma correlação entre essa leitura e o print; não foi obtido de um catálogo oficial do jogo. Os nomes originais foram mantidos para evitar traduzir duas espécies como se fossem a mesma.

O catálogo anterior contém 1379865898 como Bile Spewer. Esse ID continua preservado: não há evidência para substituir um pelo outro ou afirmar que todas as variantes são equivalentes. O app só resolve os IDs conhecidos e usa o progresso fornecido. Metas e posição das tarefas não são usadas na identificação em tempo de execução.
