# HELLDIVERS-BR — Android V39.0.0

Projeto Kotlin + Jetpack Compose. A V39 reproduz os GIFs originais de Penta e Meridia e traz o painel compacto do “?” conforme as prints. Consulte ALTERACOES-V39.md. Os tópicos antigos abaixo documentam a base herdada.

## V17: navegação nativa e telemetria

- Busca com imagem e abertura da ficha dentro do app. Arsenal usa as mesmas fichas nativas, mantendo favoritos.
- 110 registros: 91 com descrições/tabelas importadas do site e 19 registros de missão com informações básicas. Incluídos 105 ícones locais; cinco imagens ausentes na fonte são identificadas como indisponíveis.
- Despachos: três resumos iniciais, leitura completa por cartão e expansão da lista.
- Briefings da Ordem Maior e despachos traduzidos para PT-BR com cache, opção de original e nova tentativa se necessário.
- Previsão corrigida: pequenas variações da expiração não reiniciam a medição nem recolhem o painel.
- Hino no menu com tocar/pausar, repetição e volume salvo. Pausa ao sair do aplicativo; não começa sozinho.
- Tema Meridian com o mesmo arquivo de fundo do site, incluído no APK e aplicado também atrás do Arsenal.
- Central de Guerra sem o ícone adicional no contador nem o subtítulo amarelo que quebrava linha.

Consulte **ALTERACOES-V17.md** para detalhes de validação e limites. Os tópicos V8 abaixo documentam recursos herdados, preservados nesta versão.

## Antes de compilar: conferir as imagens
Abra **CONFIRIR-IMAGENS.html** no Chrome, com conexão. Confira primeiro as três artes da Ordem Maior (andamento, vitória, derrota) e a medalha. Verde significa que carregou; confira também visualmente se é a arte esperada. O HTML e o app usam os mesmos caminhos de `app/src/main/assets/site-assets.json`.

Se mudar um caminho nesse JSON, gere novamente a conferência com `python tools/generate_asset_preview.py`. Se trocar o arquivo no site mantendo o caminho, abra o link original e confirme a nova arte. Um cache antigo pode exigir limpar o cache do navegador/app para a comparação.

O teste no Chrome verifica a imagem e o acesso pelo navegador. O carregamento via Android e a seleção de estados ainda precisam ser testados no APK. Aqui, as solicitações às quatro imagens principais receberam HTTP 403; não foi possível aprovar seu conteúdo visual.

## Novidades V8
- 55 contornos originais, territórios, ícones de facção dentro dos planetas, defesa/libertação, invasões, DSS, locais especiais e marcações editoriais de Omicron.
- Artes da Ordem Maior selecionadas por estado e recompensas com SVGs originais, sem caveira genérica.
- Regiões nos dossiês do mapa e da Guerra, com tipo identificado pelo hash da região.
- Remoção do cabeçalho “Menu de navegação”, mantendo menu lateral e botão inferior.

## Recursos da V7 mantidos
- Arsenal nativo: 110 registros reais do site, códigos, aquisição, busca, categorias e favoritos.
- Mapa nativo: planetas da API, zoom, arraste, seleção, rotas, pesquisa e filtros.
- Facções nativas com conteúdo do site e acesso aos dossiês completos.
- Base da V6 preservada: Home, Guerra, Ordem Maior, temas, menu compacto e ícones originais.

Veja CHANGES-V8.md para escopo completo, recursos ainda exclusivos do site e limites de validação.

## Gerar o APK no GitHub
1. Faça backup da versão atual do repositório.
2. Copie o conteúdo da pasta Helldivers-BR-App deste ZIP para a raiz do repositório do aplicativo, mantendo a pasta .github/workflows incluída no ZIP.
3. Confirme o envio dos arquivos alterados e novos, incluindo app/src/main/assets e tools.
4. Abra Actions → Build APK Helldivers BR → Run workflow.
5. Se a compilação terminar com sucesso, baixe Helldivers-BR-apk em Artifacts. Dentro estará Helldivers-BR.apk.

Valide o APK no celular antes de atualizar o JSON público. O workflow produz APK Release com a assinatura permanente já cadastrada nos Secrets.

## O que depende de internet
Telemetria ao vivo, primeira tradução, hino e imagens ainda hospedadas no site dependem de internet. Catálogo, fichas nativas importadas, 105 ícones, fundo Meridian, traduções já armazenadas e favoritos funcionam sem rede. A última telemetria válida e os metadados dos contadores são salvos no aparelho. Projeções visuais permanecem somente em memória.

## Atualizar o catálogo a partir do site
Use Python 3, sem bibliotecas adicionais:

```sh
python tools/import_site_catalog.py /caminho/estratagemas.html
```

O comando gera app/src/main/assets/stratagems.json (catálogo básico). Na V17, manter também stratagem-details.json com exatamente a mesma ordem dos registros e o campo localIcon apontando para os ícones incluídos. O importador básico sozinho não atualiza as fichas detalhadas nem preserva esses caminhos locais. As alterações entram no próximo APK; não há sincronização automática do catálogo em instalações existentes.

## Verificação no aparelho
- Buscar “canhao” e conferir resultados com acentos; filtrar e favoritar, reiniciar e confirmar favorito.
- Abrir uma ficha específica; verificar nomes/códigos/imagens SVG e registros de missão sem link.
- Abrir o mapa; arrastar, ampliar com dois dedos, centralizar, filtrar e selecionar pela lista.
- Interromper a conexão após uma leitura; verificar aviso e conservação dos dados do mapa.
- Abrir Facções pela Home e menu; mudar entre Padrão e Meridian.
- Conferir Guerra e Ordem Maior da V6, rotação, fonte ampliada e botão Voltar.

A validação desta entrega está registrada em ALTERACOES-V17.md. A aparência, os gestos e a reprodução de áudio precisam ser conferidos no aparelho.
