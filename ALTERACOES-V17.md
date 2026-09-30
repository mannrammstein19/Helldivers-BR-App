# V17 — fichas nativas, tradução, previsão, música e tema

Base V16. Preservados o catálogo dos 12 alvos de Ordens Maiores e os ajustes anteriores do mapa. Versão 17.0.0 / versionCode 17.

## 1. Arsenal dentro do app

A busca do menu mostra a imagem de cada resultado e abre uma tela Compose nativa. O botão ABRIR FICHA do Arsenal abre a mesma tela. Inclui imagem ampliável, código com as setas do site, recarga, nível, aquisição, favoritos, descrições e tabelas expansíveis. O botão Voltar retorna ao contexto anterior.

São 110 registros: 91 com conteúdo detalhado importado das páginas do repositório HELLDIVERS-BR e 19 registros de missão cujo catálogo trazia link `#`, sem ficha vinculada. Estes últimos continuam disponíveis com os dados básicos e indicação das informações ausentes. Não foram inventados códigos ou estatísticas.

Incluídos 105 ícones em `assets/stratagem-icons`, para uso sem rede. Corrigido o caminho de Enviar Dados para o SVG Upload_Data existente. Cinco imagens não foram encontradas nos caminhos da fonte: Canhão de fusão 40-K, Broca Tectônica, Relé de Comunicações Portátil, Câmera Tática e Entrega SSSD. Quando o carregamento falha, aparece “Sem imagem”. Imagens técnicas das tabelas e algumas artes principais ainda usam os endereços do site. Vídeos das páginas não foram migrados nesta etapa.

Dados em `stratagems.json` e `stratagem-details.json`; a ordem dos registros nos dois arquivos deve permanecer correspondente. A importação básica antiga não atualiza as fichas detalhadas. Mudanças nesse conteúdo entram num novo APK.

## 2. Despachos e português

Início e Guerra mostram três despachos resumidos. Cada cartão permite ler o texto completo; VER MAIS expande o restante das dez comunicações carregadas.

Briefings da Ordem Maior (Início, Guerra e Ordem) e despachos recebem tradução automática EN → PT-BR pelo MyMemory, o mesmo provedor de `ptbr.js` do site. Apenas esses textos públicos são enviados; contadores e metas não são traduzidos nem recalculados. Os títulos conhecidos continuam com a tradução local.

Traduções persistem no aparelho (até 200 textos), com solicitações serializadas e blocos abaixo do limite de bytes por consulta. A interface permite ver o original. Em falhas ou falta de cota, mantém o texto original e oferece nova tentativa. A disponibilidade da primeira tradução depende da conexão e do serviço.

## 3. Central de Guerra

Removidos o ícone adicional acima da quantidade de Helldivers e o subtítulo amarelo “COMANDO E CONTROLE // SUPREMA AUTORIDADE”, que quebrava linha. Mantidas as artes de fundo dos indicadores.

## 4. Ritmo e previsão

Causa identificada: a API reconstrói o horário de expiração com pequenas diferenças entre respostas. Duas leituras da mesma ordem retornaram `19:30:03.9961414Z` e `19:30:09.2962369Z`. A chave antiga incorporava esse horário, perdendo a amostra anterior a cada atualização.

`OrderRateTracker` usa ID da ordem e definição do objetivo; a expiração não participa da chave. Mantém uma janela recente de até cinco minutos, exige pelo menos 30 segundos entre amostras e reinicia se houver mudança de objetivo, queda do contador ou intervalo longo sem leitura. Ausência de progresso não é tratada como zero inventado; uma leitura válida sem avanço resulta em ritmo zero e mensagem apropriada.

A consulta automática continua a cada 60 segundos. O painel aberto também não se recolhe mais por pequenas alterações da expiração. Snapshots sem telemetria ao vivo não produzem novas estimativas. Histórico permanece em memória: após encerrar o processo, são necessárias novas leituras.

## 5. Hino e Meridian

No menu: tocar/pausar o hino e controle de volume de 0 a 100%, com preferência salva. Usa `audio/hino-super-terra.mp3`, como o site, em repetição. Não toca automaticamente, pausa ao sair do app e respeita o foco de áudio do Android. Sem serviço em segundo plano.

O tema Meridian usa o arquivo original `imagens/fundos/especiais/meridian.png`, incluído no APK. Corrigidos o caminho anterior e a sobreposição que praticamente escondia a imagem. Arsenal deixa o fundo Meridian aparecer quando esse tema está ativo.

## Validação

- Executados `:app:testDebugUnitTest` e `:app:assembleDebug` com Gradle 8.7, Kotlin 1.9.24, JDK 17 e SDK Android 34: conclusão com código 0 e APK gerado.
- 16 testes passaram: 5 de previsão, 6 do catálogo de alvos e 5 de regras de artes/recompensas/regiões/mapa. Nenhuma falha ou erro nos relatórios JUnit.
- Conferido no APK: applicationId `br.com.helldiversbr.app`, versionCode 17, versionName 17.0.0, minSdk 26 e targetSdk 34.
- Conferida correspondência dos 110 registros com as fichas e existência dos 105 ícones locais e do PNG Meridian.
- Consulta de tradução do briefing atual respondeu HTTP 200, sem cota esgotada e com texto em português.
- Aparência, gestos, reprodução de áudio e comportamento no aparelho físico não foram verificados aqui.

## Aplicar

Copiar o conteúdo da pasta `Helldivers-BR-App` deste ZIP para a raiz do repositório Android, incluindo `.github` e todos os arquivos de `app/src/main/assets`. Executar Actions → Build APK Helldivers BR → Run workflow e instalar o APK gerado pelo fluxo habitual.

Este ZIP entrega o projeto-fonte completo. Site e Capacitor não foram alterados. O workflow de assinatura debug e o manifesto remoto `versao-app.json` foram preservados; a assinatura permanente não foi configurada nesta entrega.
