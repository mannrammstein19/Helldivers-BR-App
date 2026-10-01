# V28 — contadores, DSS e Configurações

## Mudanças
- Cada contador guarda valor confirmado, fonte e horário próprios. Respostas parciais ou da API direta preservam o último campo válido; não atualizam artificialmente seu horário. Campos nunca recebidos mostram Indisponível.
- Os endpoints de planetas e campanhas são combinados por campo, escolhendo a leitura mais recente. Uma campanha sem estatísticas não apaga estatísticas completas do planeta.
- Mudanças de fonte em DSS ou despachos não reiniciam um contador cuja fonte real permaneceu a mesma.
- Movimento visual exige duas leituras recentes comparáveis. Disparos e acertos podem avançar; eliminações avançam apenas para a facção presente, considerando o atacante em defesas. Cache, ausência de dados, reinício de contador e leitura sem crescimento interrompem o movimento.
- Projeções não são gravadas no cache, não alimentam notificações e não geram novas chamadas à API. A próxima leitura confirmada sempre prevalece. A projeção tem limites de idade e duração; porcentagens variam no máximo 0,0099 ponto percentual.
- Dígitos com largura igual e valores alinhados à direita estabilizam contadores e porcentagens.
- DSS indisponível usa imagem 1:1, eliminando a área vertical excedente. DSS ativa no painel do mapa usa cabeçalho compacto com estação, planeta e setor, seguido das ações existentes. Financiamento, estados, descrições e recursos foram preservados.
- Configurações incorpora os nove PNGs enviados, nomes Helldivers BR / adonai_elohim / Luis Games / Pirata Perdido e cartões menores sem textos Abrir abaixo dos avatares. Links e ações existentes permanecem.
- A correção de Início da V27 e a assinatura permanente foram preservadas.

## Gerar e publicar
1. Extraia o ZIP. Copie o CONTEÚDO de Helldivers-BR-App para a raiz do seu repositório, incluindo .github/workflows e os arquivos novos. Não crie uma segunda pasta Helldivers-BR-App dentro do repositório.
2. Envie ao GitHub. Mantenha os Secrets de assinatura que já funcionaram; nenhuma chave nova é necessária.
3. Execute Actions → Build APK Helldivers BR → Run workflow. Baixe o Artifact Helldivers-BR-apk e extraia Helldivers-BR.apk.
4. Instale e teste no celular, inclusive Início, mudança de fontes, números e DSS.
5. Renomeie o APK aprovado para helldivers-br-v28.apk. Envie ao Cloudflare R2 em helldivers-br/apps/helldivers-br-v28.apk.
6. Somente depois que o APK estiver disponível, envie versao-app.json deste projeto para helldivers-br/apps/versao-app.json no R2. O JSON já indica V28. Editar no GitHub não publica automaticamente no R2.

## Validação
- Compilação Debug e compilação dos testes de navegação realizadas localmente.
- 41 testes unitários aprovados, incluindo preservação por campo, serialização, ordem de chegada dos endpoints, tempo próprio do contador e troca do indicador geral para fontes combinadas.
- Não foram executados localmente os testes no emulador nem a validação visual em aparelho. O workflow mantém seus testes de navegação no emulador.
- O APK local de validação é Debug. O APK para atualizar instalações existentes deve ser o Release assinado gerado no seu GitHub.
