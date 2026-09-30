# V23 — navegação, previsões da Ordem e DSS no mapa

Base: Helldivers-BR-App(3).zip, enviado em 30/09/2026.

## Alterações

- Início: a faixa de 28 dp que reconhece o gesto do menu cobria a borda esquerda da barra inferior. Agora ela fica dentro do conteúdo do Scaffold e respeita a altura real da barra inferior, inclusive em paisagem e com navegação do Android. A lógica de retorno à rota inicio foi preservada.
- Ordem: nomes das métricas Ritmo observado e Conclusão estimada passam de 7 para 10 sp; resultados passam de 11 para 15 sp. A organização lado a lado permanece.
- Guerra: as mesmas métricas passam de 7/10 para 10/15 sp. Objetivos da Ordem aparecem abertos por padrão e podem ser recolhidos. O botão de abrir/recolher passa de 11 para 13 sp.
- Mapa/DSS: painel abre expandido, possui rolagem e arte em proporção 9:16. O modelo da estação fica maior e centralizado acima das informações de localização. O cartão da DSS na Guerra conserva sua apresentação anterior.
- Notificações: texto esclarece que o Uplink controla todos os alertas e que as escolhas individuais ficam salvas quando os alertas estão pausados.
- Versão: versionCode 23; versionName 23.0.0.

## Como as notificações funcionam nesta base

1. O interruptor superior (Uplink) precisa estar ligado.
2. Selecione os eventos individuais desejados. O interruptor de grupo é um atalho para marcar/desmarcar todos os eventos daquele grupo; não é uma autorização separada. Quando só parte do grupo está marcada, o interruptor de grupo aparece desligado, mas os eventos individuais marcados continuam habilitados.
3. A permissão de notificações do Android e o canal do aplicativo no sistema precisam permitir a entrega.
4. O app detecta mudanças entre leituras. A primeira leitura cria uma referência e não envia eventos antigos como se tivessem acabado de ocorrer.
5. Há verificação em segundo plano programada a cada 15 minutos, com rede. O Android pode adiar a execução. Com o app aberto, leituras atualizadas também processam alertas.
6. As seleções são filtros internos. No Android, os eventos usam o mesmo canal: Alertas da Guerra Galáctica.
7. Alertas regionais dependem de dados regionais da API. Região sob ataque é inferida pela saúde da região humana abaixo do máximo; defesa regional também depende da campanha de defesa do planeta. Não existe garantia de que a API informe todos os eventos.
8. Os chamadores não processam telemetria recuperada integralmente do cache; fontes parciais antigas possuem proteções próprias.

## Cor do mapa

A lógica existente já agrega, por setor, tanto a facção dona dos planetas quanto a facção atacante informada em planet.event.faction. Com Territórios ligado, um planeta humano sob invasão acrescenta a cor invasora ao preenchimento do setor. Mais de uma facção inimiga produz um gradiente. Isso sinaliza presença/invasão no setor; não significa que a posse do planeta mudou. Regiões internas do planeta têm sua própria apresentação baseada no dono da região.

## Instalação e validação

Extraia este ZIP completo. Use a pasta Helldivers-BR-App como projeto no Android Studio ou substitua o conteúdo do projeto existente (sem apagar o .git do seu repositório). Não inclui histórico Git, APK ou pasta de build.

Compile com o processo habitual ou GitHub Actions. O workflow existente executa os testes unitários antes de compilar o APK. Para atualizar uma instalação existente, use a mesma assinatura.

O versao-app.json incluído foi preparado para a V23. Publique o manifesto de atualização somente depois que o APK correspondente estiver disponível; este trabalho não publicou APK, manifesto ou release.

Validação nesta sessão: revisão das alterações, delimitadores Kotlin, diferenças contra o ZIP recebido, whitespace dos arquivos alterados e integridade do ZIP final. Este ambiente tem Java, mas não Gradle, SDK Android ou emulador; não houve compilação, teste instrumentado nem confirmação da entrega de notificações.

Após compilar, confira:
- Voltar ao Início de todas as abas, tocando no ícone e no texto; repetir em paisagem.
- Abrir o menu por gesto na área acima da barra inferior.
- Legibilidade das previsões na Ordem e na Guerra, incluindo tamanho de fonte maior no Android.
- DSS no mapa: arte vertical completa e rolagem até as ações táticas.
- Uplink desligado com escolhas salvas; depois ligado com permissão e eventos novos reais.
