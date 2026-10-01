# V26 — contadores dinâmicos

Base: projeto V25 entregue nesta conversa. Mantidos applicationId br.com.helldiversbr.app, chave permanente, navegação direta para Início e os testes de toque da V25. versionCode 26, versionName 26.0.0.

## Onde aparece

- Percentuais de controle/defesa dos planetas: quatro casas decimais na frente em destaque da Home, nos cartões de Guerra, no dossiê e na ficha expandida do Mapa.
- Dossiê do planeta (Guerra ou Mapa): disparos, acertos, terminídeos eliminados, autômatos destruídos e iluminados eliminados, conforme a API disponibiliza cada campo.
- Configurações: Contadores dinâmicos, ligado inicialmente. Desligar mantém as quatro casas, mas exibe os valores confirmados sem projeção. A preferência fica salva.
- Sem indicação adicional ao lado dos números, conforme solicitado.

## Funcionamento e limites

O MainViewModel guarda um histórico de apresentação separado de HomeData. Duas leituras válidas, com intervalo de 15 a 90 segundos, permitem medir a diferença por segundo. Não são criados números aleatórios.

O relógio de apresentação atualiza os componentes visíveis uma vez por segundo e pausa quando a Activity deixa de estar visível. Nenhuma consulta adicional à API foi introduzida: o refresh existente continua a cada 60 segundos.

A projeção usa no máximo um intervalo observado, limitado a 60 segundos. A diferença percentual fica limitada a 0,0099 ponto percentual e não antecipa 0% ou 100%. Contadores cumulativos não extrapolam quedas ou reinícios; a próxima leitura real sempre passa a ser a base. Assim, os números podem ficar parados quando não houver avanço, quando o limite for atingido ou quando faltar uma segunda leitura comparável.

Leitura de cache, fonte diferente, campos indisponíveis, falha de conexão ou amostra antiga suspendem a projeção correspondente. Campos ausentes não aparecem como zero inventado. A API direta atualmente fornece somente playerCount nesse modelo de estatísticas; os novos contadores dependem dos campos da Community API.

HomeData, percentuais usados nas decisões de vitória, previsões, cache e notificações permanecem confirmados pela API. As projeções nunca são persistidas como telemetria nem enviadas ao WarAlertManager. O número de Helldivers ativos e os objetivos da Ordem Maior não foram extrapolados.

## Instalar e publicar

1. Instale o APK V26 por cima da V25: assinatura permanente preservada.
2. Aguarde duas leituras da rede (normalmente cerca de um minuto após a primeira leitura) e abra Guerra ou o dossiê de um planeta com atividade.
3. Compare o efeito ligado/desligado em Configurações. Teste também offline: o estado salvo deve continuar disponível e os números devem parar de extrapolar.
4. Atualize o GitHub com o ZIP completo, incluindo os testes novos e a pasta .github. Secrets de assinatura iguais.
5. No R2, envie helldivers-br-v26.apk para helldivers-br/apps/.
6. Somente depois de testar o APK, substitua o versao-app.json dessa mesma pasta pelo arquivo da V26.

## Testes

Foram acrescentados testes do cálculo e da integração: primeiro snapshot, avanço/recuo, limites percentuais, conclusão confirmada, idade da leitura, relógio recuando, extrapolação de contadores, fonte diferente, queda/reinício, intervalo longo, cache, dados parciais, preservação do snapshot e campos de estatísticas ausentes ou acima de 32 bits.

Os testes instrumentados de navegação da V25 continuam no workflow do GitHub.

## Validação realizada

- Debug e Release compilaram; APK Release gerado com sucesso.
- 31 testes unitários executados, zero falhas, zero erros, zero ignorados.
- Os testes instrumentados de navegação compilaram; não executados localmente por ausência de emulador. O workflow do GitHub continua executando esses testes.
- apksigner confirmou assinatura v2 e o mesmo certificado permanente da V24/V25.
- Manifesto confirmado: versionCode 26 e versionName 26.0.0.
- Workflow YAML e scripts Bash validados. Os 123 arquivos de assets/recursos da V25 foram comparados e preservados.
- A confirmação visual e de desempenho ainda deve ser feita no aparelho.
