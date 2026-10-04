# V38 — movimento contínuo e informações do mapa

Base: GitHub d2a7271 (V37), conferida com o ZIP Helldivers-BR-App(5).zip (V36).
Inclui as imagens novas de Penta (vermelha) e Meridia (clara).

## Comportamento

- Rotas de invasão, pulsos, naves e planetas especiais continuam animando com a última leitura salva, durante a atualização e entre requisições.
- Relógio sincronizado aos quadros da tela, sem reiniciar quando a fonte de dados muda. A geografia estática mantém o cache existente.
- Movimento respeita a preferência “Movimento e pulsos” e a configuração de animações do Android. A rotina pausa quando o mapa sai do estado visível.
- Dados salvos mantêm a data/hora real; a animação não altera progressos nem converte cache em telemetria ao vivo.
- O “?” reúne leituras separadas de planetas, frentes e DSS, fontes por extenso, data e hora no fuso do aparelho, legenda e instruções.
- PREFERÊNCIAS no “?” abre os ajustes já existentes e salvos no aparelho.
- A versão exata do painel antigo não foi localizada: este painel foi reorganizado segundo as informações solicitadas.

## Instalação

Copie todo o conteúdo de Helldivers-BR-App/ para a raiz do repositório Kotlin,
substituindo os arquivos. Inclua os novos arquivos MapAnimationClock.kt,
MapReadingDetails.kt, MapReadingDialog.kt e o teste MapContinuityTest.kt.
A versão do build já está em 38 / 38.0.0. Gere o APK pelo workflow existente.
O JSON público de atualização não foi alterado; publique-o após conferir o APK.

## Verificação

Cinco testes JUnit compilados e executados com Kotlin 1.9.24 / Java 17:
continuidade por 18.000 quadros sem telemetria (5 minutos), troca de nomes de
fontes sem reinício do relógio, retomada/quadros anteriores sem retrocesso,
data/hora PT-BR por fuso e fontes/cache legíveis.
Também conferidos: referências/pixels dos assets, ligação do relógio no Canvas,
preservação da data de cache, sintaxe dos Kotlin alterados e git diff --check.

Não foi compilado APK neste ambiente (sem Android SDK/Gradle). O teste do relógio
não substitui a validação visual no aparelho nem a compilação completa do app.
Não houve envio ao GitHub ou publicação.
