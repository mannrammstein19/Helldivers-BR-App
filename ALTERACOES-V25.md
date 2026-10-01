# V25 — revisão da navegação

Base: V24 com o workflow de verificação de certificado corrigido. Mesmos applicationId, chave permanente, senha, alias e fingerprint. versionCode 25 e versionName 25.0.0.

## Mudanças

- As telas principais são selecionadas diretamente por estado salvo. Início e Página Principal do menu lateral executam a mesma ação. O retorno deixa de depender de popBackStack ou da restauração de uma entrada inicial do NavHost.
- Cada toque em Início recria a tela no topo usando os dados existentes; não depende de uma chamada de rede. A atualização periódica dos dados permanece.
- O conteúdo fica fisicamente dentro dos limites do Scaffold, acima da barra inferior e abaixo do cabeçalho. As telas não precisam desenhar por baixo dos botões.
- Os itens inferiores ocupam toda a altura disponível e usam comportamento de aba selecionável, incluindo acessibilidade.
- Foi removida a faixa invisível de 28 dp sobreposta à tela. O gesto é observado pelo próprio conteúdo, somente quando começa nos 32 dp da borda esquerda e se move predominantemente para a direita por 40 dp. Taps, rolagem vertical e multitouch não acionam a abertura.
- Menu de navegação tem botão visível no cabeçalho. O menu aberto também pode ser fechado por gesto para a esquerda, pelo botão Fechar ou pelo Voltar do Android.
- Voltar do Android: fecha primeiro o menu; detalhes de estratagema retornam ao Arsenal; Notificações retorna a Configurações; demais telas principais retornam ao Início. No Início, permanece o comportamento normal de sair do aplicativo.
- A rota corrente é salva na recriação da Activity. O conteúdo, cache, notificações, DSS, mapa, configurações e temas foram mantidos.

## Testes de navegação incluídos

NavigationTouchTest usa toques reais em coordenadas de tela, para detectar também sobreposição de áreas clicáveis. Ele cobre:

1. Retorno ao Início pelo ícone e pela parte inferior do botão, repetidamente, a partir de Guerra, Ordem, Mapa, Arsenal e Configurações.
2. Gesto da borda esquerda abrindo o menu e Página Principal retornando ao Início.
3. Botão visível de menu, fechamento pelo Voltar e retorno pelo item do menu.
4. Retorno ao Início após rotação para paisagem.
5. Rolagem vertical na borda sem abrir o menu.
6. Voltar de Notificações para Configurações e depois retornar ao Início.

O workflow de build agora executa estes testes em emulador API 29 antes de compilar o Release. O relatório é guardado como Artifact Relatorio-navegacao. Essa etapa torna a compilação mais demorada, pois inicializa um emulador Android. Os Secrets existentes são reutilizados.

## Instalar e publicar

- A V25 Release foi preparada com a mesma chave da V24. Instale por cima da V24; não há nova troca de assinatura.
- Envie o projeto completo ao GitHub, incluindo .github/workflows/build-apk.yml e app/src/androidTest. Os arquivos privados de assinatura continuam fora do projeto.
- Para publicar no R2, use o nome helldivers-br-v25.apk no mesmo prefixo helldivers-br/apps/.
- Teste o download do APK antes de substituir o versao-app.json nesse prefixo.
- O JSON incluído anuncia 25.0.0 e aponta para:
  https://pub-f324221f4e5e42b08ecfa5062afd5960.r2.dev/helldivers-br/apps/helldivers-br-v25.apk
- Não precisa alterar os dois GitHub Secrets para compilar esta versão ou as seguintes.

## Validação local

- Compilação Kotlin das versões Debug e Release concluída.
- 16 testes unitários executados: zero falhas e zero erros.
- Os seis testes de navegação instrumentados compilaram. Não foram executados neste ambiente, que não dispõe de emulador Android; a execução fica no workflow do GitHub e a confirmação final no aparelho.
- YAML e scripts Bash do workflow validados.
- Os 123 arquivos originais de assets e recursos Android foram comparados: nenhum ausente ou alterado.
- APK Release gerado com sucesso; apksigner confirmou a assinatura v2 e o certificado permanente da V24. Manifesto confirmado: br.com.helldiversbr.app, versionCode 25, versionName 25.0.0.
