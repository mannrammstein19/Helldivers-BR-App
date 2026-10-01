# V27 — Configurações, menu por gesto e contadores por facção

Base: V26 entregue nesta conversa. Mesma chave permanente e applicationId. versionCode 27, versionName 27.0.0.

## Configurações

- Aviso de notificações no topo, acima do cartão do desenvolvedor. Vermelho quando o interruptor geral está ligado; neutro quando desligado. Toque abre a tela já existente de personalização.
- O aviso distingue bloqueio geral pelo Android e nenhum tipo selecionado, evitando informar envio ativo nessas condições.
- Cartão de destaque DarylDixon_19 com foto circular, descrição e ação para copiar o contato do Discord.
- Contatos existentes agrupados em Discord, YouTube e WhatsApp, com imagens circulares e nomes abaixo. Nenhum contato ou link novo foi inventado. A referência branca foi usada como estrutura; o tema escuro/amarelo do projeto permanece.
- Compartilhamento, site, contadores dinâmicos, tema, verificação manual/automática de atualização e créditos continuam disponíveis abaixo.

## Menu

Removido o cabeçalho Menu de Navegação que tinha sido acrescentado na V25. Menu continua abrindo pelo gesto da borda esquerda e fechando pelo gesto, Fechar ou Voltar. Barra inferior e retorno direto ao Início preservados. Conteúdo respeita a barra de status do Android.

## Contadores

- Exibição dinâmica a cada 250 ms enquanto a tela está visível e existe ritmo diferente de zero, sem aumentar consultas à API.
- No planeta de Terminídeos só suas eliminações são projetadas; no planeta de Autômatos só autômatos; no planeta de Iluminados só iluminados. Disparos e acertos seguem seus próprios ritmos.
- Em defesa da Super Terra, considera a facção atacante do evento.
- Os valores confirmados de todas as facções permanecem visíveis. As demais eliminações não recebem projeção, mas podem mudar se a própria API confirmar um novo valor.
- Cache, notificações, objetivos, previsões e regras de vitória continuam utilizando os dados confirmados. Limites e proteção da V26 mantidos; sem texto adicional de estimativa na tela.

## Instalar e publicar

Instale o APK V27 sobre a V26, sem desinstalar nem trocar as chaves. Confira Configurações com notificações desligadas e ligadas, toque no aviso para abrir a personalização e teste Guerra → Início pelo botão e pelo menu lateral.

Para publicar, envie helldivers-br-v27.apk ao R2 em helldivers-br/apps/. Depois de testar o APK, substitua o versao-app.json dessa mesma pasta pelo arquivo incluído. O ZIP completo pode substituir a base do GitHub, incluindo .github e os testes; os Secrets permanecem iguais.

## Validação realizada

- APK Release compilado com sucesso; versionCode 27, versionName 27.0.0.
- 34 testes unitários executados, zero falhas e zero erros. Incluem seleção de facção, atacante em defesa e troca de inimigo sem projeção anterior.
- Testes instrumentados de navegação atualizados e compilados; execução no emulador fica no GitHub. Não houve execução local em aparelho/emulador.
- Assinatura v2 e certificado permanente confirmados com apksigner.
- 123 arquivos de assets/recursos da V26 preservados; workflow YAML/Bash validado.
- Conferência final da aparência, toque e desempenho deve ser feita no aparelho.
