# V29 — planeta e regiões independentes

Base: V28 Refinada, incluindo ícones, assinatura permanente, Início e DSS.

## Correções
- Progresso de libertação usa exclusivamente a saúde do planeta. Um planeta em 0% não herda mais o maior percentual de uma região.
- Defesas continuam usando a saúde do evento de defesa.
- A regra compartilhada corrige Guerra, dossiês, mapa, apresentação da Ordem e cálculo de ritmo/projeção do planeta. A conquista de um assentamento não significa a conquista do planeta.
- Dossiês passam a listar todas as regiões entregues pela API, incluindo bloqueadas, concluídas e aquelas sem disponibilidade informada. Não são criadas regiões ausentes da resposta.
- Bloqueadas ficam identificadas e não exibem jogadores ativos ou progresso presumido. O app não infere que bloquear equivale a conquistar.
- Aviso de dados salvos aparece uma vez, no final das estatísticas. Horários iguais mostram uma data; horários diferentes mostram o intervalo real. Valores continuam alinhados à direita, com dígitos de largura igual e alinhamento vertical central.
- A animação da V28 foi preservada: taxa observada entre duas leituras confirmadas recentes, tempo limitado, sem sorteios ou novas chamadas por quadro. Campos salvos não são animados; a API prevalece na próxima leitura.

## Publicação
1. Copie o conteúdo da pasta Helldivers-BR-App para a raiz do repositório, incluindo .github/workflows e arquivos novos.
2. Preserve os Secrets existentes. Execute Actions → Build APK Helldivers BR → Run workflow.
3. Baixe e teste o APK Release assinado no celular.
4. Após aprovação, envie helldivers-br-v29.apk para helldivers-br/apps/ no R2.
5. Depois do APK disponível, publique versao-app.json desta versão em helldivers-br/apps/versao-app.json no R2.

## Testes
Os testes de regressão cobrem planeta em 0% com assentamento em 95,29%, progresso planetário não zero, prioridade do evento de defesa e o aviso único com horários diferentes. A verificação visual no aparelho permanece necessária.

Validação local concluída: 43 testes unitários aprovados, APK Debug compilado e testes de navegação compilados. O emulador e a validação visual no celular não foram executados localmente. O APK Release de atualização deve ser gerado pelo GitHub com a assinatura permanente.
