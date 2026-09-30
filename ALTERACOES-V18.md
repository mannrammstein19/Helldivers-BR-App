# HELLDIVERS-BR App — V18

## Navegação
- O sexto botão da barra inferior deixou de ser **Menu** e passou a ser **Config.**.
- O menu lateral continua completo, mas agora abre por gesto iniciado na borda esquerda.
- A área sensível ao gesto é estreita (28 dp) e exige um arraste horizontal de pelo menos 56 dp, reduzindo conflitos com o Mapa Galáctico e rolagens.
- A faixa do gesto usa exclusão de gesto do sistema apenas nessa pequena área para evitar disputa com o gesto de voltar do Android.

## Primeira abertura
- Novo aviso nativo de primeira execução explicando: **deslize da borda esquerda para a direita para abrir o menu**.
- Ao tocar em **ENTENDI**, a confirmação fica gravada em `SharedPreferences`.
- O aviso não aparece novamente, salvo limpeza de dados/reinstalação (ou futura mudança explícita da chave do tutorial).

## Configurações
- Nova tela nativa **Configurações**.
- Área reservada para foto/perfil do desenvolvedor.
- Controle de tema movido também para uma área apropriada de Aparência.
- Atalho funcional para o Discord oficial via página do HELLDIVERS-BR e para o site completo.
- Espaços já preparados para Discord do desenvolvedor, WhatsApp, YouTube e canal parceiro; permanecem desativados até os links definitivos serem fornecidos.
- Nova seção de dedicatória/créditos e aviso de projeto comunitário não oficial.

## Hino da Super Terra
- O controle foi reduzido para uma faixa compacta de 92 dp.
- Tocar/pausar, volume e estado continuam disponíveis sem ocupar o antigo espaço vertical.
- Caminho opcional já preparado para a futura arte de fundo:
  `app/src/main/assets/backgrounds/super-earth-anthem.webp`
- Sem a imagem, o degradê interno mantém o componente funcional e visualmente íntegro.

## Versão
- `versionCode = 18`
- `versionName = 18.0.0`
