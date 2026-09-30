# V24 — assinatura permanente e atualizações no R2

A base é a V23 já entregue. Esta versão preserva suas correções de interface e muda a assinatura de distribuição para Release permanente. versionCode 24 e versionName 24.0.0.

## Configuração uma única vez

1. Extraia o ZIP de projeto e envie seu conteúdo para o repositório Helldivers-BR-App. Inclua o arquivo .github/workflows/build-apk.yml atualizado; não basta enviar a pasta app.
2. Extraia separadamente o pacote privado de assinatura. NÃO coloque seus arquivos no repositório nem no R2 público.
3. No GitHub, abra Settings > Secrets and variables > Actions > New repository secret.
4. Crie HD_SIGNING_KEYSTORE_BASE64, copiando o conteúdo completo de HD_SIGNING_KEYSTORE_BASE64.txt no campo Secret.
5. Crie HD_SIGNING_PASSWORD, copiando o conteúdo de HD_SIGNING_PASSWORD.txt no campo Secret.
6. Em Actions > Build APK Helldivers BR > Run workflow, compile a branch atualizada. O workflow testa a lógica, compila Release e verifica o certificado. Sem os Secrets corretos, ele falha explicitamente e não gera outra assinatura aleatória.

A chave utiliza o alias hd2-br. A senha da chave é a mesma do arquivo de armazenamento. O fingerprint SHA-256 público esperado está fixado no workflow. Não regenere a chave para atualizações futuras. Guarde o pacote privado em um local seguro.

## Primeira instalação dessa base

A chave Debug usada nas execuções antigas do GitHub não foi preservada pelo workflow fornecido. Este pacote não recupera a assinatura da V22 ou V23. Se houver uma cópia externa da chave antiga, ela pode ser reutilizada mediante ajuste da configuração.

Como não temos a chave antiga, a V24 deve ser instalada após remover a versão Debug anterior. A remoção apaga preferências e cache local. Baixe e guarde primeiro o APK novo. Esta troca não foi executada no seu aparelho por este trabalho.

As próximas versões Release assinadas com ESTA chave podem atualizar a V24 por cima, mantendo os dados, desde que mantenham applicationId e um versionCode crescente.

## Publicar a V24 no R2

1. Baixe o Artifact Helldivers-BR-apk, extraia Helldivers-BR.apk e renomeie para helldivers-br-v24.apk.
2. Envie para o mesmo prefixo do R2 usado na V23: helldivers-br/apps/helldivers-br-v24.apk.
3. Confirme que a URL abaixo baixa o APK:
   https://pub-f324221f4e5e42b08ecfa5062afd5960.r2.dev/helldivers-br/apps/helldivers-br-v24.apk
4. Só depois envie o versao-app.json incluído para helldivers-br/apps/versao-app.json.
5. A V24 consulta:
   https://pub-f324221f4e5e42b08ecfa5062afd5960.r2.dev/helldivers-br/apps/versao-app.json
6. Instale a V24 como descrito acima. Com o JSON publicado em 24, ela deve informar que está na versão mais recente.

A V22/V23 antiga continua consultando o GitHub. Não recebe uma mudança de endereço automaticamente. Após os usuários migrarem para a V24, o repositório pode ser privado sem afetar a consulta de atualização da V24. O APK e o JSON do R2 permanecem públicos. O endereço r2.dev é para teste; podemos trocar para um domínio de distribuição depois.

## Próximos updates

1. Preserve os dois Secrets e a chave privada.
2. Atualize app/build.gradle.kts: versionCode 25 e versionName 25.0.0, por exemplo.
3. Envie o código, execute o mesmo workflow e baixe o APK Release.
4. Publique no R2 como helldivers-br-v25.apk.
5. Atualize o JSON com versionCode 25, versionName 25.0.0 e apkUrl apontando para esse arquivo. Envie o JSON somente após o APK estar acessível.
6. No app, Verificar atualização > Baixar > instalar por cima. Não remova o app nos próximos updates.

## Validação feita

- Geração e abertura de keystore PKCS12 com RSA 3072 e validade de 10000 dias.
- Base64 íntegro e abaixo do limite de tamanho dos GitHub Secrets.
- Preparação de chave do workflow testada com Secrets preenchidos; ausência de Secret interrompe o processo.
- Sintaxe YAML e Bash dos passos do workflow verificada.
- APK não compilado nesta sessão: não há Gradle/SDK Android. A compilação Release e a verificação da assinatura do APK serão feitas pelo workflow no GitHub.
- Nenhum Secret foi cadastrado remotamente, nenhum arquivo enviado ao R2 e nenhum APK publicado por este trabalho.
