# V31 — regiões confirmadas e fundos compactos

Base: projeto completo V30 Corrigida, com abertura, navegação e assinatura permanente preservadas.

## Estados e evidência

A Community API simplificada não informa o controle regional. A V31 complementa as regiões com planetRegions da resposta bruta do jogo, consultada pelo passthrough comunitário; se falhar, tenta o backend direto. Owner 1 = Humanos, 2 = Terminídeos, 3 = Autômatos, 4 = Iluminados.

- Disponível para operações: isAvailable = true.
- Limpo / Recuperado: owner = 1 e isAvailable = false.
- Bloqueado para operações: owner inimigo conhecido e isAvailable = false.
- Aguardando confirmação: resposta insuficiente. Não considera HP zerado uma prova de conquista.

Evidência observada em 01/10/2026, guerra 801: Martale (199), Songguo Cun (0), owner 1, isAvailable false, health/maxHealth 100000/100000; Xin Fuzhou (1), owner 3, isAvailable false; Aurora Bay (114), Eaglemount (1), owner 3, isAvailable true. Esses casos estão nos testes. A associação usa planetIndex + regionIndex, nunca a ordem dos arrays. Metadados da Community API são preservados.

FONTES: https://api.helldivers2.dev/raw/api/WarSeason/801/Status ; https://api.helldivers2.dev/api/v1/planets/199 ; https://github.com/helldivers-2/api/blob/master/src/Helldivers-2-Models/ArrowHead/Status/PlanetRegionStatus.cs ; https://github.com/helldivers-2/api/blob/master/src/Helldivers-2-Models/V1/Planets/Region.cs . O app descobre a guerra atual automaticamente; 801 não está fixado no código.

availabilityFactor tem finalidade desconhecida na documentação pública. A V31 não inventa um percentual de desbloqueio nem afirma que todo bloqueio é falta de meta. Mostra o bloqueio confirmado para operações. O controle do assentamento nunca substitui a libertação do planeta.

## Cache e carga

Uma resposta contém todas as regiões. Intervalo mínimo de um minuto, inclusive após erro; WarID comunitário guardado por seis horas. A consulta ocorre em paralelo com as demais leituras, não por card. Continua usando o ciclo normal de atualização do app. A data salva é a data da confirmação regional; uma resposta incompleta conserva o estado anterior com aviso discreto ao final. Uma nova resposta válida substitui o cache, inclusive se o inimigo retomar a região. Os alertas ignoram regiões salvas/incompletas e só notificam recuperação quando havia controle inimigo conhecido antes.

## Visual

Três imagens locais WebP em app/src/main/res/drawable-nodpi:
- region_operacao.webp: operação, acento amarelo.
- region_bloqueado.webp: bloqueio, acento vermelho.
- region_recuperado.webp: recuperação, acento azul.

Cards compactos e expansíveis naturalmente para texto grande. Texto, estado e ícone são nativos, sem letras dentro da imagem. Cards recuperados e bloqueados não repetem barras de progresso sem utilidade. O componente compartilhado aparece no dossiê aberto por Guerra e por Mapa. Os resumos de ambos também distinguem recuperadas, bloqueadas e disponíveis.

As três imagens foram geradas com a ferramenta integrada de geração de imagens e otimizadas para WebP; somam aproximadamente 72 KB. Os prompts estão em PROMPTS-REGIOES-V31.md.

## Validação e instalação

Construção limpa de assembleDebug, testes unitários e compilação dos testes Android. Os testes visuais em emulador não foram executados localmente (sem /dev/kvm); o workflow do GitHub continua exigindo os seis testes de navegação antes do APK Release assinado.

Copie o conteúdo da pasta Helldivers-BR-App do ZIP para a raiz do clone, substitua os arquivos, faça Commit e Push e execute Build APK Helldivers BR. Não apague o clone e não altere os Secrets. A V31 tem versionCode 31 e versionName 31.0.0. Após o workflow passar, publique o APK assinado em helldivers-br/apps/helldivers-br-v31.apk no R2. Só depois publique versao-app.json no mesmo local usado nas atualizações anteriores. O JSON incluído já aponta para a URL prevista do APK V31; ele não faz upload sozinho.
