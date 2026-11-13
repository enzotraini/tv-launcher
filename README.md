# Para você

Launcher leve para a TCL 75" QD Mini LED com Google TV. A home é clara, em português do Brasil, no espírito de uma fileira “Para você”: prateleiras de filmes e séries e um dock de apps embaixo. A segunda tela, **Entradas e atalhos**, abre HDMI, TV ao vivo e configurações.

Nada neste repositório foi instalado na TV. Os scripts rodam no seu computador, na mesma rede Wi-Fi do aparelho. Este ambiente só compilou o APK.

A interface não segue o visual escuro de Apple TV. O cliente ADB e o debloat reversível foram adaptados de [tv-home](https://github.com/mihirgupta0900/tv-home) (MIT, Mihir Gupta). Veja [LICENSE](LICENSE).

## O que aparece na TV

- Fundo claro, título **Para você**, saudação e relógio.
- Prateleiras horizontais com cartazes desenhados no próprio app. Os títulos são um catálogo estático, sem rede e sem pôster de estúdio.
- Dock, nesta ordem: Netflix, YouTube, Globoplay, Prime Video, Disney+, Stremio. App que não estiver instalado continua no dock, apagado.
- **Todos** abre a grade dos outros apps instalados (Plex, Jellyfin e o que mais tiver atividade de launcher).
- Ao voltar para o launcher, a lista de pacotes é relida.
- **Entradas**, o botão Menu do controle, ou a tecla de fonte (`TV_INPUT`) abrem HDMI, TV ao vivo, o seletor da TCL se existir, Configurações e Wi-Fi.

## O que você precisa

- A TV ligada, no mesmo Wi-Fi do computador.
- macOS ou Linux.
- Python 3.
- JDK 17 ou 21.
- Android SDK com `platforms;android-34` e `build-tools;34.0.0`. O Android Studio já traz os dois. Anote a pasta do SDK (`$HOME/Library/Android/sdk` no Mac, em geral `$HOME/Android/Sdk` no Linux).

## 1. Opções de desenvolvedor e depuração USB

Na TV, com o controle:

1. **Configurações → Sistema → Sobre**.
2. Desça até **Compilação** (Build) e aperte OK sete vezes. Aparece a confirmação de que você já é um desenvolvedor.
3. Volte e abra **Configurações → Sistema → Opções de desenvolvedor**.
4. Ligue **Depuração USB**.
5. Se existir **Depuração sem fio** ou **Depuração de rede**, ligue também e anote o IP e a porta. A porta clássica é `5555`. Na depuração sem fio a porta pode ser outra.
6. Deixe a TV acordada. **Permanecer ativa** nas opções de desenvolvedor ajuda durante a instalação.

Quando o computador conectar pela primeira vez, a TV mostra **Permitir depuração USB?**. Aceite. Marque “sempre deste computador” se a opção existir.

## 2. Preparar o computador

Na raiz do repositório:

```bash
./setup.sh
```

Isso cria `tools/venv` com o cliente ADB em Python (`adb-shell`). Se `ANDROID_HOME` (ou a pasta padrão do Android Studio) existir, o script grava `local.properties`. Esse arquivo fica só na sua máquina.

No Ubuntu, se o `venv` reclamar que o `ensurepip` não existe, instale `python3-venv` e rode `./setup.sh` de novo.

Sem Android Studio, com as command-line tools já instaladas:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" \
  "platforms;android-34" "build-tools;34.0.0"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
```

No Mac, o `adb` do sistema às vezes é bloqueado na rede local. Os scripts preferem o cliente Python. Se precisar do binário assim mesmo: Ajustes do Sistema → Privacidade e Segurança → Rede Local, e permita o Terminal. Dá para forçar o modo com `ADB_CLIENT=python` ou `ADB_CLIENT=binary`.

## 3. Encontrar a TV

```bash
tools/find-tv.sh
```

O script varre a sua rede /24 nas portas 5555 (ADB) e 8008 (Cast) e grava o primeiro IP com ADB em `tools/.tv_host`. Esse arquivo não entra no git.

Se a varredura não achar, ou achar o aparelho errado:

```bash
tools/find-tv.sh 192.168.0.20
```

Porta diferente de 5555:

```bash
export TV_PORT=37123
export TV_HOST=192.168.0.20
```

Confira o aparelho e aceite o aviso na TV:

```bash
tools/venv/bin/python tools/tv.py --probe
```

A saída traz fabricante, modelo e a versão do Android. Se ficar esperando, olhe a TV: o pedido de autorização pode estar na tela. A chave RSA fica em `tools/adbkey/` e também não vai para o git.

Depuração sem fio com código de pareamento (seis dígitos) usa o protocolo do `adb` oficial, não o `adb-shell`. Nesse caso, uma vez:

```bash
adb pair 192.168.0.20:PORTA_DE_PAREAMENTO
export ADB_CLIENT=binary
export TV_PORT=PORTA_DE_CONEXAO
```

A porta de pareamento e a porta de conexão são as duas que a TV mostra. Não são a mesma.

## 4. Backup

Antes de desabilitar qualquer pacote:

```bash
tools/backup.sh
```

O resultado vai para `backup/<data-hora>/` (pasta ignorada pelo git): modelo da TV, lista de pacotes e qual atividade responde pelo Home hoje. Guarde essa pasta.

## 5. Debloat

A lista `examples/tcl-google-tv/debloat.txt` é um ponto de partida adaptado da TCL G07 do tv-home. Não é um inventário da sua 75". Abra o arquivo, leia os comentários e comente qualquer pacote que você queira manter. Chromecast (`com.google.android.apps.mediashell`) já vem comentado.

A lista **não** desliga o launcher do sistema nem o `com.google.android.tungsten.setupwraith`. Isso só acontece no passo de instalação, quando este app já pode assumir o Home. O script recusa esses pacotes se alguém os colocar na lista.

```bash
tools/disable.sh --dry-run examples/tcl-google-tv/debloat.txt
tools/disable.sh examples/tcl-google-tv/debloat.txt
```

Cada linha vira um único `pm disable-user --user 0 …`. Nada é desinstalado. Pacote que não existir na TV aparece como erro e o script segue. Não junte vários `pm` com `;` na mão: no Android a cadeia pode parar no primeiro comando sem dizer.

## 6. Compilar

```bash
tools/install-launcher.sh --build-only
```

O APK de debug fica em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

O mesmo comando, sem `--build-only`, compila e instala. O app não pede internet. Não há chave de API no código.

## 7. Instalar e definir como launcher padrão

Com a TV acordada e o probe do passo 3 funcionando:

```bash
tools/install-launcher.sh
```

O script, nesta ordem, cada um em uma chamada separada:

1. `pm install -r` do APK.
2. Apaga o APK temporário em `/data/local/tmp`.
3. Define `dev.enzotraini.tvlauncher/.HomeActivity` como Home.
4. Desabilita o launcher do Google TV (`com.google.android.apps.tv.launcherx`).
5. Desabilita `com.google.android.tungsten.setupwraith`, que toma o botão Home quando o launcher de fábrica sai do caminho.
6. Define o Home de novo.
7. Força a parada de Configurações e deste launcher, e manda a tecla Home.

Android TV antigo, sem launcherx:

```bash
STOCK_PACKAGE=com.google.android.tvlauncher \
STOCK_HOME=com.google.android.tvlauncher/.MainActivity \
  tools/install-launcher.sh
```

`STOCK_HOME` vale para o restore. Na instalação, `STOCK_PACKAGE` é o pacote que será desabilitado.

Se a TV ainda perguntar qual app abre ao apertar Home, escolha **Para você**.

Conferir:

```bash
tools/venv/bin/python tools/tv.py \
  'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
```

O resultado deve citar `dev.enzotraini.tvlauncher/.HomeActivity`.

## 8. Testar o Home de dentro de um app

1. Abra Netflix, YouTube ou Configurações.
2. Aperte **Home** no controle.
3. A home clara **Para você** tem de vir para a frente, sozinha.

Se abrir o Google TV antigo, o launcher de fábrica ainda está ativo. Rode `tools/install-launcher.sh` de novo e confira o nome do pacote:

```bash
tools/venv/bin/python tools/tv.py 'pm list packages' | grep -i launcher
```

Se abrir uma tela de configuração em vez da home, o `setupwraith` ainda está ativo. Rode a instalação de novo. Se o pacote tiver outro nome:

```bash
tools/venv/bin/python tools/tv.py 'pm list packages' | grep -i wraith
SETUPWRAITH_PACKAGE=nome.que.aparecer tools/install-launcher.sh
```

Se a home abrir atrás do app que estava na frente, rode `tools/install-launcher.sh` outra vez. O script força a parada de Configurações e do launcher antes de mandar Home. Foi o que destravou esse caso no tv-home.

No dock, **Todos** deve listar o que você instalou além dos seis apps fixos. Volte de um app para a home depois de instalar algo novo: a lista é relida. **Entradas** deve abrir mesmo sem HDMI detectado (no emulador ou num aparelho sem entrada visível aparece o aviso, e Configurações continua lá).

## 9. Restaurar

```bash
tools/restore.sh --dry-run examples/tcl-google-tv/debloat.txt
tools/restore.sh examples/tcl-google-tv/debloat.txt
```

O restore:

- reabilita cada pacote da mesma lista;
- reabilita `com.google.android.tungsten.setupwraith`;
- reabilita o launcher do sistema;
- devolve o Home para `com.google.android.apps.tv.launcherx/.home.HomeActivity`;
- manda a tecla Home.

O app Para você continua instalado, só deixa de ser o padrão. Remova-o depois que o Home antigo já estiver de volta:

```bash
tools/venv/bin/python tools/tv.py 'pm uninstall dev.enzotraini.tvlauncher'
```

Esse `pm uninstall` é o único uninstall do projeto, e é opcional. O debloat em si nunca desinstala.

## Dock

Edite [app/src/main/res/values/config.xml](app/src/main/res/values/config.xml). `dock_apps` e `dock_labels` andam juntos, da esquerda para a direita. Pacotes úteis:

| App | Pacote |
|---|---|
| Netflix | `com.netflix.ninja` |
| YouTube | `com.google.android.youtube.tv` |
| Globoplay | `com.globo.globotv` |
| Prime Video | `com.amazon.amazonvideo.livingroom` |
| Disney+ | `com.disney.disneyplus` |
| Stremio | `com.stremio.one` |
| Plex | `com.plexapp.android` |
| Jellyfin | `org.jellyfin.androidtv` |

Para confirmar no aparelho:

```bash
tools/venv/bin/python tools/tv.py 'pm list packages'
```

`hidden_apps` tira pacote da grade **Todos**. `live_tv_package` e `input_picker_package` são os atalhos de Entradas. `app_columns` é o número de colunas da grade.

Depois de editar:

```bash
tools/install-launcher.sh
```

## Recomendações

As prateleiras saem de `StaticRecommendations`, um catálogo fixo dentro do app. O gancho é a interface `RecommendationsSource` e o método `RecommendationCatalog.current()` em `app/src/main/java/dev/enzotraini/tvlauncher/model/Catalog.kt`.

Uma versão futura pode trocar essa fonte por TMDB:

1. Implemente `RecommendationsSource`.
2. Leia a chave de `recommendations.properties` (copie de `recommendations.properties.example`). O arquivo real está no `.gitignore`.
3. Devolva a fonte nova em `RecommendationCatalog.current()`, com o catálogo estático de reserva se a rede falhar.
4. Só então acrescente a permissão `INTERNET`. Ela não está no manifesto de propósito.

Não coloque a chave em `config.xml`, no Gradle versionado nem neste README.

## Avisos

- **macOS e o binário adb.** “No route to host” na rede local é o sistema barrando o `adb`. O padrão dos scripts é `adb-shell`.
- **setupwraith.** No Google TV ele tem prioridade de Home maior que um launcher de terceiros quando o launcher de fábrica está desligado. A instalação desabilita. O restore reabilita.
- **Um `pm` por chamada.** `pm disable a; pm disable b` pode silenciar o resto da linha. Os scripts não fazem isso.
- **Home atrás de outro app.** Force-stop de `com.android.tv.settings` e do launcher, depois a tecla Home. O script de instalação já faz isso.
- **App da Play Store não aparece.** A home relê os pacotes ao voltar. Se ainda faltar, o app pode não ter atividade `LEANBACK_LAUNCHER` nem `LAUNCHER`.
- **Lista de debloat.** É conservadora e herdada da G07. Na sua firmware, confira `pm list packages` e comente o que você usa. Desabilitar o pacote errado (atualização, entrada HDMI, Play Store) não é o objetivo desta lista.

## O que fica para depois

- Fonte real de recomendações (TMDB ou outra), com a chave fora do git.
- Ajuste da lista de debloat depois de ver os pacotes desta TCL.
- Prova visual na tela de 75". Aqui só houve compilação do APK de debug, sem aparelho.

## Mapa do repositório

| Caminho | Função |
|---|---|
| `app/` | Launcher Android. Sem rede e sem serviço em segundo plano. |
| `app/src/main/res/values/config.xml` | Dock, apps ocultos, TV ao vivo, colunas da grade. |
| `tools/tv.py` | ADB por Wi-Fi: shell, lote e envio do APK. |
| `tools/find-tv.sh` | Acha a TV na rede. |
| `tools/backup.sh` | Cópia dos pacotes e do Home atual. |
| `tools/disable.sh`, `tools/restore.sh` | Aplicam e desfazem a lista. |
| `tools/install-launcher.sh` | Compila, instala e assume o Home. |
| `examples/tcl-google-tv/debloat.txt` | Pacotes para desabilitar, nunca desinstalar. |

## Licença

MIT. Copyright (c) 2026 Enzo Traini. As ferramentas de ADB e o fluxo de debloat incluem o aviso do tv-home, copyright (c) 2026 Mihir Gupta. Os dois textos estão em [LICENSE](LICENSE).
