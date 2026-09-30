# HELLDIVERS-BR App — V21

## DSS alinhada ao portal HELLDIVERS-BR

Esta versão porta para o Android nativo a lógica de DSS já usada em `guerra.js` no portal, sem transformar a tela em HTML/WebView.

### Dados e cache da DSS
- Continua lendo `/api/v2/space-stations` pela API comunitária.
- A leitura da DSS agora é centralizada em `DssRepository` e compartilhada entre Central de Guerra e Mapa.
- TTL de 2 minutos, igual ao intervalo específico do portal.
- Distingue conexão indisponível, nenhuma estação retornada e localização não informada.
- Preserva a última leitura boa por até 24 h enquanto o processo do app estiver vivo.
- O cache persistente em disco **não** faz parte desta versão; será integrado à futura parede geral de telemetria.

### Estados das ações táticas
- `status == 2` é tratado como **ATIVA**, conforme a regra atual do portal.
- Financiamento abaixo de 100%: **PREPARANDO**.
- Financiamento concluído: **ATIVANDO**.
- Prazo futuro sem ação ativa: **RECARREGANDO**.
- Sem condição ativa: **DESATIVADA**.
- Ações ativas pulsam em verde; preparação/ativação usam amarelo; recarga/desativação usam vermelho.

### Identidade visual reutilizada do site
O Android usa os mesmos assets publicados pelo HELLDIVERS-BR, através do domínio oficial do projeto:
- `imagens/guerra/dss/dss-indisponivel.webp`
- `imagens/guerra/dss/DSS_Summary_Model.png`
- `imagens/guerra/dss/EAGLE STORM.png`
- `imagens/guerra/dss/ORBITAL BLOCKADE.png`
- `imagens/guerra/dss/HEAVY ORDNANCE DISTRIBUTION.png`

As imagens não foram redesenhadas nem substituídas por aproximações.

### Central de Guerra
- Novo painel nativo da DSS depois da Ordem Maior.
- Quando disponível, mostra planeta, setor, arte da estação, eleição e ações táticas.
- Quando indisponível, usa a arte e as mensagens equivalentes às do portal.
- Planetas de campanha que hospedam a DSS recebem o selo `DSS // ESTAÇÃO DEMOCRACIA`.

### Mapa Galáctico
- A cápsula DSS da V20 permanece.
- O painel inferior agora usa a mesma lógica da Central de Guerra.
- Inclui contribuição, progresso e descrição estratégica quando a API fornece esses dados.
- O planeta hospedeiro continua entrando no contexto visual do mapa.

## Versão
- `versionCode = 21`
- `versionName = 21.0.0`
