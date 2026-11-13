# Auditoria de navegação e proposta de foco

Estado em `cursor/launcher-vivo-7d09` (commit `9f4c4db`). Este documento só descreve o que o código faz hoje e o sistema de foco proposto. Nenhuma tela muda até o Enzo aprovar a etapa 3.

A leitura é do código, não de uma nova sessão na TCL. O sintoma na TV real (abas Para você, Filmes e Séries mudas, dock incluso; Ao vivo funciona) é explicado por esse código. A prova no controle fica para a etapa 4.

## O que a navegação faz hoje

O controle não é tratado num lugar só. Cada tela monta a própria cadeia com `Dpad.stack` (`ui/Dpad.kt`), que grava `nextFocusLeft/Right/Up/Down` por id. `View.tvFocus` (`ui/Motion.kt`) marca a view como focável, liga `focusableInTouchMode`, anima escala em 200 ms e pede `requestRectangleOnScreen`.

Teclas globais ficam em `TvActivity`:

| Tecla | Home | Outras telas |
| --- | --- | --- |
| Menu / TV input | Abre Entradas | No descanso, fecha. Em Entradas, fecha. |
| Voltar | Sobe uma faixa (`bandAfterBack`). Na faixa das abas, fica na home. | Fecha a activity (comportamento padrão), salvo o descanso, que também fecha. |
| OK / Enter / DPAD_CENTER | O que a view focada fizer em `onKeyUp`. Não há um caminho único. | Igual. |
| Home do sistema | `singleTask` + `clearTaskOnLaunch` traz a home de volta e `onNewIntent` reabre Explorar, zerando a memória de foco. | A home cobre a activity. |

Faixas da home, de cima para baixo, em `HomeActivity.wireFocus`:

1. Abas (Explorar, Filmes, Séries, Ao vivo) e a engrenagem.
2. Botão “Assistir” do hero, só se ele estiver visível e a aba não for Ao vivo.
3. Prateleira: pôsteres, ou, no Ao vivo, canais e depois “Grátis pra assistir”.
4. Dock (Netflix, YouTube, Globoplay, Prime Video, Disney+, Stremio, Todos).

O dock é um `HorizontalScrollView` desenhado por cima do conteúdo, com 96 dp de respiro no fundo. Esquerda e direita andam dentro da faixa. Nas pontas, o próximo id é o próprio item, então a tecla é consumida e o foco não dá a volta. Cima e baixo também apontam para o próprio item na primeira e na última faixa.

Memória: `ShelfMemory` guarda o índice da prateleira, dos canais e de “Grátis”. `lastFocusKey` guarda a tag da view (aba, hero, pôster, canal, dock) para o Voltar e para quando a activity recria. Não há memória da aba de Filmes separada da de Séries além do índice cru por `shelfId`.

Fora da home:

- **Detalhe.** A raiz pede foco enquanto o TMDB responde. Os botões “Assistir na X” entram numa única faixa. Voltar fecha.
- **Todos.** Grade em `ScrollView`. Cada app é focável e clicável. O primeiro pede foco.
- **Entradas.** Duas faixas (HDMI/antena, atalhos). A primeira entrada pede foco. OK abre a entrada; segurar OK renomeia. Menu fecha.
- **Fundos.** Fotos e, abaixo, as ações. OK na foto gira. “Ver descanso” abre o modo descanso.
- **Descanso.** Qualquer tecla que não seja volume fecha. Não há foco interno.

Abrir app: o dock e a grade chamam `InstalledApps.open(component)`, que tenta o componente gravado e depois `openPackage`. Canais e “Grátis” chamam `openPackage` direto. Os dois caminhos usam `getLeanbackLaunchIntentForPackage` e, se falhar, `getLaunchIntentForPackage`, com `FLAG_ACTIVITY_NEW_TASK`.

## Onde o foco se perde

### Causa nas abas Para você, Filmes e Séries, e no dock

Ao vivo funciona porque o foco cai num card de canal, que é focável e clicável desde o primeiro frame. `showLive` pede foco no primeiro canal quando o usuário ainda não apertou tecla. Esquerda e direita andam entre cards. OK dispara o clique. O bloco do catálogo está `GONE`, então o hero e a fileira de pôsteres nem entram na busca.

Nas outras três abas o pouso é outro.

1. O catálogo chega fora do frame principal. `showSkeleton` esvazia a fileira e coloca seis cartazes com `isFocusable = false` (`PosterCard.showLoading`). O hero esconde o botão “Assistir” (`play.visibility = GONE`).
2. O `HorizontalScrollView` dessa fileira está `android:focusable="false"` no XML. O construtor da plataforma chama `setFocusable(true)` depois de ler o XML (`ScrollView` e `HorizontalScrollView`, `initScrollView`). O atributo do layout não vale. Com `FOCUS_AFTER_DESCENDANTS` e nenhum filho focável, o próprio scroller pode ficar com o foco. Nesse estado, esquerda e direita são consumidas por `arrowScroll` e OK não faz nada, porque o scroller não é clicável. O mesmo vale para o `ScrollView` da grade Todos e para as fileiras de Entradas, Fundos e do detalhe, se o foco cair no container.
3. Quando a lista chega, `renderCatalog` só chama `hero.play.requestFocus()` se nenhuma tecla foi pressionada (`focusTouched`). Qualquer `ACTION_DOWN`, inclusive a tentativa de navegar durante o skeleton, liga `focusTouched` antes da tecla ser tratada. Aí o código restaura `lastFocusKey` em vez de corrigir um foco que tenha caído no scroller.
4. No caminho rápido (cache de 6 h, que é o caso com chave válida), nenhuma tecla foi pressionada e o foco vai para o botão “Assistir”. Esse botão é uma faixa de um item só. `Dpad.stack` aponta esquerda e direita para o próprio botão. As duas teclas morrem ali.
5. Cima a partir das abas, com o catálogo pronto, também vai para esse botão (`downFromTabs = play.id`), não para o dock. O dock só é alcançado descendo do botão, atravessando a prateleira e descendo de novo. Preso no botão ou no scroller, o tile do dock nunca recebe OK. O lançamento em si é o mesmo que o Ao vivo já usa com sucesso. O dock não abre porque o clique não chega.
6. No orçamento de uma janela de 540 dp de altura (imagem Android TV 1080p, e vários painéis 4K que medem nessa faixa), o hero é o que sobra depois das abas, do título da prateleira, dos 186 dp do pôster e dos 96 dp do dock. Sobra perto de 130 dp. O bloco de texto do hero (título de 40 sp em até duas linhas, meta, chip e o botão) é mais alto que isso, e o `HeroBanner` corta com `clipToOutline`. O botão focado fica fora da área visível. A escala de foco da aba é 1, sem contorno, então subir para a aba também não desenha um alvo claro. Na tela, o controle parece morto.

Por isso dá para chegar em Ao vivo pela fileira de abas (esquerda e direita entre abas funcionam) e, lá, o controle volta a responder. Nas três abas de catálogo, o alvo real não é um cartaz nem um tile do dock.

Dois agravantes, no mesmo fluxo:

- `tvFocus` liga `focusableInTouchMode`. No Android, a primeira tecla de direção depois que a janela entra em touch mode só sai desse modo, e um OK numa view nesse estado pode focar em vez de clicar. Um controle que também manda evento de ponteiro deixa o OK mudo mesmo com o item certo focado.
- `refreshDock`, no `onResume`, reagenda `restoreFocus` para o frame seguinte. Se a chave salva ainda existe, o foco salta de volta para ela depois que o usuário já andou.

### Outros saltos

- Voltar em cima de um card “Grátis” não sobe uma faixa: o caso especial em `onBackPressed` manda o foco para a fileira de canais.
- `onNewIntent` (botão Home) zera `lastFocusKey` e `focusTouched` e reabre Explorar. Ao voltar de um app pelo Home, a posição some. Ao voltar por Voltar, a activity da home nem foi destruída e a posição permanece, desde que a view da tag ainda exista.
- Trocar de aba remove os pôsteres na hora (`removeAllViews`). Se o foco estava num pôster, a view sai da árvore e o foco fica nulo até alguém pedir de novo. Com foco nulo, o D-pad não anda.
- No detalhe, a raiz é focável e pede foco antes dos botões existirem. OK nessa raiz não faz nada.

## Responsabilidade das activities

`HomeActivity` (cerca de 840 linhas) busca o TMDB, monta hero, pôsteres, canais, dock e clima, gera a paleta, escuta pacotes instalados e é o único dono do grafo de foco. O grafo é reescrito no skeleton, no resultado, no dock e na troca de aba, sempre com ids novos. Não há um tipo que represente “faixa focável”.

As outras telas repetem o mesmo trio: inflar, `tvFocus`, `Dpad.stack`, `requestFocus` no primeiro item. Funciona quando os filhos já nascem focáveis (Entradas, Fundos, Ao vivo). Quebra quando o container de scroll fica focável e os filhos ainda não estão, que é o caso do catálogo.

## Performance

Isto não é a causa do controle morto. Entra aqui para a etapa 3 não piorar o frame da tecla.

- A tecla em si só lê ids. O custo ruim é reconstruir a fileira inteira (`removeAllViews` e cartazes novos) a cada troca de aba e a cada `onResume` do catálogo.
- `App.tmdb()` cria um `TmdbRepository` novo em toda chamada. O trabalho de rede e o decode de imagem já estão num pool de três threads. A paleta também. Isso pode continuar fora da UI thread.
- O shimmer invalida a cada 32 ms enquanto o skeleton existe. Tem de parar quando o pôster real entra, como já faz `setRunning(false)`.
- `refreshDock` lista de novo todos os apps com launcher em todo `onResume`. O atalho por assinatura de pacotes já evita remontar à toa. Vale mantê-lo.
- O grafo de foco deve ser calculado uma vez por bind, nunca no callback de bitmap.

## Referências (só o modelo, sem copiar código)

[MomTv](https://github.com/DhilipBinny/SmartTv-MomTv) (MIT, Copyright 2026 Dhilip Kumar) trata cada card como o alvo de foco. A tecla de confirmação abre o app no próprio card. A escala fica perto de 1,06 com um contorno que aparece no foco. A primeira fileira pede foco no primeiro item. As fileiras são listas horizontais dentro de uma lista vertical: o framework anda pela posição e a fileira só rola para acompanhar. A ponta não dá a volta.

[Yukuza](https://github.com/urunkarpm/yukuza-launcher) marca a fileira como grupo de travessia. Quando o foco entra na fileira, cai num item de verdade, não no container. Abrir app prefere a categoria leanback e cai para a categoria launcher. O anel de foco entra com fade. O README traz um selo MIT, mas o repositório clonado não tem arquivo `LICENSE`. Nada de lá será copiado. Os dois projetos servem só para confirmar o modelo: item focável e clicável, container que não toma foco, confirmação explícita, memória do item ao entrar na fileira.

O layout claro já aprovado permanece. Cor, tipo, dock e a estrutura Para você / pôsteres / dock não entram nesta proposta.

## Arquitetura proposta

Um diretor de foco pequeno, sem biblioteca nova, sem Compose e sem Leanback. A home continua dona dos dados. O diretor só recebe as faixas já montadas e devolve o próximo alvo.

### 1. Container de fileira que não pode ter foco

Subclasse de `HorizontalScrollView` (e outra de `ScrollView` para a grade Todos) que, depois do construtor, força `isFocusable = false` e `isFocusableInTouchMode = false`, e não consome seta (`executeKeyEvent` retorna falso). O rolamento continua por `requestRectangleOnScreen` quando o filho ganha foco, que é o que `tvFocus` já pede. Vale para a fileira de pôsteres, canais, “Grátis”, dock, Entradas, Fundos, ações do detalhe e a grade Todos.

### 2. Ligação espacial, calculada depois do layout

Função pura, testável sem emulador. Entrada: faixas em ordem, e para cada item o id e o retângulo visível na tela. Saída: o próximo id em cada direção.

- Esquerda e direita: vizinho na mesma faixa. Na ponta, a tecla fica (não aponta para o próprio id de um jeito que também prenda cima e baixo, e não dá a volta).
- Cima e baixo: a faixa vizinha. O item escolhido é o que tem o centro horizontal mais perto do item atual. É isso que torna a descida previsível quando a fileira de baixo tem outro comprimento.
- Nunca um `ScrollView` ou uma view que não seja focável e clicável.
- O dock é faixa explícita, não “o que estiver geometricamente embaixo”. Ele flutua por cima do conteúdo. Geometria pura cairia nele por acidente ou nunca o alcançaria.
- O hero é um único alvo: o banner, não o botão interno. O texto “Assistir” continua desenhado. Esquerda e direita no hero trocam o título em destaque (hoje essas teclas morrem). Cima vai para a aba atual. Baixo vai para o pôster lembrado.
- Recalcular uma vez quando a faixa termina de ser montada e numa passagem de layout. Se o foco atual é nulo ou está num container, pedir foco no item lembrado daquela faixa. Se o foco já está num item válido, não roubar.

`Dpad.stack` passa a usar essa função. O hábito de apontar cima e baixo para si mesmo na primeira e na última faixa sai.

### 3. Memória por seção

`ShelfMemory` já guarda índice por id de prateleira. Completar com a chave estável (id do TMDB, pacote do dock, id do canal, aba). Ao entrar na faixa, o foco cai nessa chave se a view ainda existe. Ao fechar um app com Voltar, `onResume` restaura a mesma chave. O botão Home pode continuar abrindo Explorar, mas sem apagar a memória das outras abas: ao voltar para Filmes, o pôster anterior reaparece focado.

Não restaurar por cima de um foco válido. Não restaurar para uma view que o `removeAllViews` já tirou.

### 4. Regras de tecla, num lugar só

Em `TvActivity`, antes do despacho normal:

- **OK, Enter e DPAD_CENTER.** Se a view focada é clicável e está visível, `performClick` uma vez e a tecla é consumida. O card, o tile, a aba e o botão do detalhe não dependem do `onKeyUp` padrão de `TextView`.
- **Voltar na home.** Sobe uma faixa: dock → prateleira → hero → abas. Nas abas, fica. O atalho que hoje manda “Grátis” direto para os canais entra na mesma regra (sobe uma faixa).
- **Voltar nas outras telas.** Fecha a tela e devolve a home com o foco que ela tinha.
- **Menu.** Na home, abre Entradas. Não vira atalho de app.
- **Home do sistema.** Traz a home para a frente sem destruir a memória das seções. Abrir um app continua com `getLeanbackLaunchIntentForPackage` e, se não houver, `getLaunchIntentForPackage`, com `FLAG_ACTIVITY_NEW_TASK`. O componente solto deixa de ser a primeira tentativa: no Ao vivo, o caminho que já funciona é o do pacote.

`focusableInTouchMode` sai de `tvFocus`. No controle, focável e clicável bastam. A primeira tecla deixa de ser comida para sair de touch mode.

### 5. O que não muda

Visual, tokens, cópia, TMDB, cache, Ao vivo, Entradas, descanso, debloat e `restore.sh`. Nenhuma dependência nova. `HomeActivity` não é reescrita: o diretor é uma classe ao lado, chamada nos pontos em que hoje existe `wireFocus`.

## Etapa 3, quando for aprovada

Incrementos, nesta ordem, cada um compilando sozinho:

1. Containers que não tomam foco, trocando as tags de scroll.
2. Função pura do grafo e testes de unidade: ponta da fileira, fileira de tamanho diferente, faixa vazia, hero único, dock como última faixa, restauração de índice.
3. Hero como um alvo só, com esquerda e direita trocando o título.
4. OK centralizado e Voltar subindo uma faixa, inclusive a partir de “Grátis”.
5. Memória por seção no retorno do app e na troca de aba.
6. Tirar `focusableInTouchMode`.

## Etapa 4, o que dá para provar e o que fica na TCL

Provável em teste, sem TV:

- A função pura: limites, troca de faixa, item lembrado, fileira vazia, nenhum id apontando para um container.
- `testDebugUnitTest` e `assembleDebug`, para regressão do que já existe.
- Um teste instrumentado no emulador, com fixture de TMDB só no `androidTest`, confirmando que seta move o foco (aba → hero → pôster → vizinho → dock) e que OK abre o detalhe ou a grade Todos. Isso ainda não foi escrito. Não entra neste commit.

Só na TCL G08, com a chave de verdade:

- O anel de foco visível no hero e no dock, à distância de uma sala.
- OK abrindo Netflix, YouTube e Globoplay pelo pacote instalado.
- Voltar de um app caindo no mesmo pôster.
- Nenhuma tecla perdida enquanto a prateleira ainda está no skeleton.

Até essa sessão, o que está provado é a leitura do código. O sintoma do Enzo combina com ela. Não está provado de novo num `screencap` desta etapa.
