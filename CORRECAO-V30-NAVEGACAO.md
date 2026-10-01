# V30 — correção dos testes de navegação

O relatório recebido tem seis testes, quatro falhas. O teste de ida e volta à Início por todas as abas passou. A construção foi interrompida nos testes do emulador, antes da etapa Release.

O tutorial só aparece após os quatro segundos da abertura. A preparação anterior procurava ENTENDI antes de esperar a tela Início; agora espera a abertura, fecha o tutorial quando presente e verifica a janela pronta. Nos testes do painel, aguarda a abertura do painel e rola até Página Principal, que pode estar abaixo da área visível no emulador pequeno. O retorno das notificações aguarda a tela de Configurações. A rotação consulta a Activity atual pelo ActivityScenario, no thread da UI. O diagnóstico imprime todas as raízes de semântica, incluindo diálogos.

Os seis testes continuam ativos e preservam os toques físicos, o gesto de borda, o fechamento do painel por Voltar e o retorno a Início após rotação. O workflow, a chave permanente, o certificado, a versão 30 e os recursos do aplicativo estão preservados.

Validação local: construção limpa de :app:assembleDebug; 44 testes unitários aprovados; :app:compileDebugAndroidTestKotlin aprovado. Não foi possível executar os testes de interface neste ambiente, que não oferece /dev/kvm. A confirmação final será feita pelo emulador do GitHub Actions.

## Aplicar
1. Extraia este ZIP.
2. Copie o conteúdo da pasta Helldivers-BR-App para a raiz do seu clone do repositório, substituindo os arquivos. Não crie uma segunda pasta Helldivers-BR-App dentro da primeira.
3. Faça Commit e Push no GitHub Desktop.
4. Execute Actions > Build APK Helldivers BR > Run workflow.
5. Depois de todos os testes passarem, baixe o artifact Helldivers-BR-apk. Publique o APK no R2 e depois o JSON de atualização pelo procedimento já usado.

Não precisa excluir o repositório, fazer outro clone ou cadastrar novamente os Secrets. Este ZIP é o projeto completo V30; não contém chaves privadas nem APK assinado.
