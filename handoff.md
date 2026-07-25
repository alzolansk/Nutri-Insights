# Handoff — WidgetFatSecret

Atualizado em: 2026-07-25

> **Nota de sessão (2026-07-25, 9ª parte) — LEIA PRIMEIRO:** o projeto entrou
> numa fase nova. Existe um `planning.md` na raiz com o plano completo de
> evolução para "Nutri Insights" (app com navegação por abas, mantendo os
> widgets intocados). As Etapas 0, 1 e **2** (design system) do plano já foram
> implementadas. **A frente de trabalho ativa a partir de agora é `planning.md`,
> não as seções 1–19 abaixo** (que documentam o histórico de ajuste dos widgets
> e continuam válidas como referência de decisões passadas, mas não são mais o
> próximo passo). A próxima etapa é a **Etapa 3 — casca de navegação**, que é a
> primeira a tocar em UI real (`MainActivity`) — antes dela, rodar o checklist
> manual de fumaça dos widgets (`docs/widget-smoke-test.md`). Etapas 0–1 na
> seção 20; Etapa 2 na seção 21.
>
> Nota de sessão (2026-07-25): além da redução do widget para uma linha
> (sessão anterior), foi adicionado o **gráfico semanal de calorias** no widget
> quando ele é aumentado verticalmente. Detalhes na seção 13.
>
> Nota de sessão (2026-07-25, 2ª parte): dois bugs reportados pelo usuário
> foram corrigidos. Detalhes na seção 14.
>
> Nota de sessão (2026-07-25, 3ª parte): criado o **widget de peso**
> (`WeightWidget`), com endpoints de peso descobertos e verificados na API real.
> Detalhes na seção 15.

## 1. Objetivo geral do projeto

`WidgetFatSecret` é um aplicativo Android pessoal, local-first, que autentica uma conta FatSecret por OAuth 1.0a de três etapas, lê o diário alimentar privado e apresenta os totais nutricionais do dia em:

- um widget de tela inicial implementado com Jetpack Glance;
- uma tela principal implementada com Jetpack Compose.

O app mantém localmente as metas de calorias e macronutrientes, o último snapshot nutricional válido e os tokens OAuth. A sincronização ocorre ao conectar a conta, ao abrir o app, após mudanças de metas e periodicamente pelo WorkManager.

## 2. Objetivo específico desta sessão

Corrigir o widget que ocupava duas linhas de altura no launcher apesar de o conteúdo usar somente a parte superior, e permitir que ele fosse reduzido para uma única linha sem perder calorias, meta, progresso e macronutrientes no layout largo.

## 3. O que já foi concluído

- A causa foi identificada no metadata do AppWidget: `targetCellHeight="2"`, `minHeight="110dp"` e `minResizeHeight="110dp"` forçavam duas células verticais.
- O provider agora declara uma célula vertical e altura mínima/redimensionável de `40dp`.
- Os dois tamanhos responsivos de `NutritionWidget` passaram de `110dp` para `40dp` de altura.
- Os espaçamentos internos foram reduzidos apenas o necessário para a terceira barra de macros não ficar cortada no layout de uma linha.
- O projeto foi compilado, os testes unitários foram executados e o APK debug foi instalado no emulador conectado.
- O widget existente no emulador foi redimensionado manualmente de duas linhas para uma. A renderização final foi inspecionada com dados reais persistidos (`788 kcal`, proteína, carboidratos e gordura) e todos os elementos ficaram visíveis.

## 4. O que foi alterado

### Arquivos alterados nesta sessão

- `app/src/main/res/xml/nutrition_widget_info.xml`
  - `android:minHeight`: `110dp` → `40dp`.
  - `android:minResizeHeight`: `110dp` → `40dp`.
  - `android:targetCellHeight`: `2` → `1`.
  - Foram preservados `minWidth="150dp"`, `targetCellWidth="2"` e `resizeMode="horizontal|vertical"`.
- `app/src/main/java/com/example/widgetfatsecret/fatsecret/widget/NutritionWidget.kt`
  - `NutritionWidget.COMPACT`: `DpSize(150.dp, 110.dp)` → `DpSize(150.dp, 40.dp)`.
  - `NutritionWidget.MEDIUM`: `DpSize(250.dp, 110.dp)` → `DpSize(250.dp, 40.dp)`.
  - Padding vertical de `WidgetRoot`: `12.dp` → `8.dp`.
  - Espaços entre `MacroBar` em `MediumContent`: `7.dp` → `4.dp`.
  - Espaço entre a linha de texto e a barra dentro de `MacroBar`: `4.dp` → `3.dp`.
  - Comentários de documentação foram ajustados para mencionar o widget de uma célula.
- `handoff.md`
  - Criado nesta etapa para registrar o estado atual.

### Componentes relevantes que não foram modificados nesta sessão

- `NutritionWidgetReceiver.kt`: liga `NutritionWidget` ao sistema AppWidget/Glance.
- `WidgetColors.kt`: fornece pares de cores claro/escuro para o launcher.
- ~~`FatSecretApp.kt`: abre o app oficial FatSecret e possui fallbacks para Play Store/web.~~ (arquivo **removido** na seção 18; o clique abre este app)
- `FatSecretRepository.kt`, `NutritionCacheStore.kt`, `GoalsStore.kt` e `TokenStore.kt`: persistência e estado local.
- `FatSecretAuthClient.kt`, `FatSecretFoodClient.kt`, `OAuth1SigningInterceptor.kt` e `OAuth1Signer.kt`: autenticação e comunicação com a API.
- `SyncWorker.kt` e `SyncScheduler.kt`: sincronização periódica e imediata.
- `MainActivity.kt`, `FatSecretViewModel.kt` e `AppScreens.kt`: fluxo Compose e tratamento do callback OAuth.

## 5. Decisões técnicas e motivos

- Foi mantido `SizeMode.Responsive` com variantes por largura (`COMPACT` e `MEDIUM`). A solicitação era reduzir a altura, não redesenhar a hierarquia de informação nem remover macros.
- Foi usado `40dp` porque esse é o valor de metadata que representa uma célula no cálculo clássico de tamanho de AppWidget (`70dp × 1 - 30dp`) e permite ao launcher oferecer o redimensionamento para uma linha.
- `targetCellHeight` foi alterado explicitamente para `1`; mudar apenas os modificadores Glance não resolveria a grade reservada pelo launcher.
- O conteúdo largo continuou com calorias à esquerda e três macros empilhados à direita. A validação no Pixel Launcher mostrou que ele cabe em uma linha após diminuir padding e gaps.
- Não foram alteradas largura mínima, cores, tipografia, ações de toque, sincronização, persistência ou integração FatSecret, pois estavam fora do escopo.
- Para validar uma instalação já existente, o widget foi redimensionado pelo próprio Pixel Launcher. Atualizar o APK altera os limites permitidos, mas não reduz automaticamente uma instância que o usuário já deixou em duas linhas.

## 6. Problemas, limitações e bugs conhecidos

- Uma instância já adicionada pode permanecer com duas linhas após atualizar o APK. O usuário deve manter o widget pressionado e arrastar a alça inferior para cima, ou removê-lo e adicioná-lo novamente.
- A validação visual desta sessão foi feita somente no Pixel Launcher do emulador `emulator-5554` (1080 × 2400, densidade 420). Outros launchers podem aplicar dimensões, padding ou máscaras diferentes.
- Não foi validado nesta sessão o layout de uma linha com fonte do sistema ampliada, tema escuro, largura compacta mínima ou estados `NO_CREDENTIALS`, `DISCONNECTED` e `LOADING`.
- O texto da seção `7.1` do `README.md` ainda menciona aproximadamente `86dp` úteis e descreve premissas da altura anterior. O comportamento documentado precisa ser atualizado numa etapa futura, mas o README não foi alterado porque esta solicitação proíbe trabalho adicional além do handoff.
- A pasta `C:\Users\joaov\AndroidStudioProjects\WidgetFatSecret` não possui `.git` próprio. `git rev-parse --show-toplevel` resolve para `C:\Users\joaov`, que contém muitos arquivos não relacionados. Portanto, `git status`/`git diff` não oferecem um histórico confiável isolado deste projeto. Não inicialize ou reorganize Git sem autorização.
- A API FatSecret não fornece as metas de calorias/macros da conta; elas precisam continuar configuradas localmente em `GoalsStore`.
- OAuth 1.0 do FatSecret exige whitelist de IP e pode falhar em redes com IP variável ou antes da propagação da whitelist.

## 7. Testes realizados e resultados

- Comando executado com o JBR do Android Studio:

  ```powershell
  $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
  .\gradlew.bat testDebugUnitTest assembleDebug
  ```

- Resultado final: `BUILD SUCCESSFUL`, 44 tarefas, com `testDebugUnitTest` e `assembleDebug` concluídos.
- APK gerado em `app/build/outputs/apk/debug/app-debug.apk` e instalado com sucesso usando `adb install -r`.
- `dumpsys appwidget` confirmou para `NutritionWidgetReceiver` os valores compilados equivalentes a `minHeight=40dp` e `minResizeHeight=40dp`.
- Validação visual antes: widget com aproximadamente 580 px de altura e grande área branca vazia.
- Validação visual depois: widget com aproximadamente 270 px de altura, uma linha do launcher, sem área vazia excessiva; calorias, meta, barra, restante e as três macros ficaram visíveis.
- Captura final da sessão: `C:\Users\joaov\AppData\Local\Temp\widget-final-loaded.png`. É um arquivo temporário e pode não existir em sessões futuras.

## 8. Dependências, configurações e credenciais necessárias

- Android Studio com JBR em `C:\Program Files\Android\Android Studio\jbr`.
- Android SDK configurado em `local.properties`; nesta máquina o SDK está em `C:\Users\joaov\AppData\Local\Android\Sdk`.
- `compileSdk 36`, `targetSdk 36`, `minSdk 34`, Java/Kotlin target 11.
- Principais bibliotecas: Jetpack Compose, Jetpack Glance 1.1.1, WorkManager, DataStore, Security Crypto, Retrofit, OkHttp e kotlinx.serialization. As versões ficam em `gradle/libs.versions.toml`.
- `local.properties` deve fornecer, sem colocar valores no código ou neste handoff:
  - `FATSECRET_CONSUMER_KEY`;
  - `FATSECRET_CONSUMER_SECRET`;
  - `FATSECRET_CALLBACK_URL` (padrão: `widgetfatsecret://oauth-callback`).
- O callback é convertido em `manifestPlaceholders` por `app/build.gradle.kts` e precisa corresponder ao callback usado no FatSecret.
- É necessária whitelist de IP no painel FatSecret para a API OAuth 1.0.
- O `adb` não estava no `PATH`; nesta máquina foi chamado por `C:\Users\joaov\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Invocar `gradlew.bat` com o Java padrão da máquina produziu `Error: -classpath requires class path specification`; definir `JAVA_HOME` para o JBR do Android Studio resolveu o problema.

## 9. O que ainda falta implementar

**Superado pela seção 20 / `planning.md`.** O widget nutricional já ganhou
responsividade de altura (seção 16) e o widget de peso já existe (seção 15),
então boa parte da lista original abaixo já está feita. A lista de pendências
ativa agora é a das Etapas 2–11 do `planning.md` (§8–9 desse arquivo) — ver
seção 20 para o estado exato de onde parar.

<details>
<summary>Lista original desta seção (histórico, majoritariamente resolvido)</summary>

Trabalhos futuros recomendados, somente com autorização:

- tornar o widget nutricional responsivo também à altura disponível — feito, seção 16;
- criar futuramente um segundo widget dedicado a peso — feito, seção 15;
- atualizar a seção `7.1` do `README.md` para refletir a altura de uma célula;
- validar o widget em outros launchers/dispositivos, tema escuro e escalas de fonte maiores;
- validar visualmente os estados sem dados/conexão e a variante `COMPACT` na altura mínima;
- considerar testes instrumentados ou snapshots para os tamanhos do widget, se a infraestrutura do projeto passar a suportá-los.

</details>

## 10. Próximos passos em ordem de prioridade

**A fonte da verdade agora é `planning.md` §8–9** (ordem de etapas + detalhe de
cada uma). Resumo de onde estamos e o que vem a seguir:

1. **Executar o checklist manual da Etapa 0** (`docs/widget-smoke-test.md`,
   itens 1–12) num dispositivo/emulador real — não foi possível nesta sessão
   por falta de ambiente. Bloqueia formalmente o avanço para qualquer etapa
   que toque em UI (Etapa 3+), embora a Etapa 2 (design system) possa
   prosseguir em paralelo por não mexer em runtime.
2. **Etapa 2 — Design system**: tokens de cor/tipografia do deck "Nutri
   Insights" + componentes base em `ui/design/` (`StatCard`, `MetricValue`,
   `SyncStatusChip`, etc.). Não toca em `WidgetColors.kt` nem em lógica —
   ver planning.md §9 Etapa 2 para a paleta exata e as regras.
3. **Etapa 3 — Casca de navegação**: `AppShell` com 5 abas + placeholder,
   atrás da flag `USE_LEGACY_UI`. **Ponto de maior risco para os widgets** —
   ler planning.md §1.2 e §6 antes de tocar em `MainActivity.kt`.
4. Etapas 4–9 (telas) e 10–11 (estados / remoção do legado) na ordem descrita
   em planning.md §8.

Ao final de CADA etapa: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
+ o checklist de `docs/widget-smoke-test.md` (o ciclo completo, itens 9–12, é
obrigatório nas Etapas 3, 5 e 11).

## 11. Instrução exata para o próximo agente começar

> Leia `planning.md` inteiro primeiro (é o plano vigente), depois a seção 20
> deste `handoff.md` para o estado exato desta sessão. As Etapas 0 e 1 do
> plano já estão implementadas: existe uma tag `baseline-widgets` no commit
> inicial, `docs/widget-smoke-test.md` com o checklist, e uma camada de
> histórico nova em `fatsecret/data/history/` + `fatsecret/domain/history/`
> (sem UI, sem chamador de `HistoryRepository.refresh()` ainda — isso é
> esperado). Rode `git status`/`git log` para confirmar que nada mudou desde
> então. **Antes de escrever qualquer código de UI**, confirme que o checklist
> manual de `docs/widget-smoke-test.md` foi executado num dispositivo/emulador
> — se a linha "Etapa 0" na tabela de registro do doc ainda disser "pendente",
> rode-o primeiro. Depois disso, siga para a Etapa 2 (design system) do
> `planning.md` §9, que não tem essa dependência. Não toque em
> `MainActivity.kt`, nos dois widgets, nos DataStores existentes ou em
> `AppContainer.syncAndRefresh()` sem reler planning.md §1.2 e §10 (contratos e
> riscos) primeiro.

## 12. O que não deve ser alterado ou refeito sem necessidade

- Não reverta `minHeight`, `minResizeHeight`, `targetCellHeight`, `COMPACT` ou `MEDIUM` para `110dp`/duas células; isso recria o problema original.
- Não remova macros nem redesenhe o widget apenas para ganhar altura: o layout atual já foi validado em uma linha no Pixel Launcher.
- Ao adicionar suporte a alturas maiores, não sobrecarregue nem prejudique a variante mínima de uma linha. O conteúdo extra deve aparecer progressivamente apenas quando houver espaço.
- Não misture o futuro widget de peso ao `NutritionWidget` sem uma decisão explícita; ele foi solicitado como um novo widget, com provider e escopo próprios a serem definidos.
- Não altere o fluxo OAuth, endpoints, assinatura OAuth 1.0, callback, whitelist, armazenamento de tokens ou credenciais para resolver questões visuais do widget.
- Não coloque credenciais ou tokens em arquivos versionáveis, logs, documentação ou mensagens. Não exponha o conteúdo de `local.properties`.
- Não substitua `WidgetColors` por cores fixas: os `ColorProvider(day, night)` permitem que o launcher escolha corretamente o tema claro/escuro.
- O bloco `<queries>` do `AndroidManifest.xml` foi **removido** (seção 18) junto com `FatSecretApp.kt`. Só volte a adicioná-lo se algum código voltar a usar `resolveActivity`/`getLaunchIntentForPackage`/`queryIntentActivities`; `startActivity` direto (como o `ACTION_VIEW` do OAuth em `MainActivity`) **não** é afetado pela filtragem de visibilidade de pacotes.
- Não mude o comportamento de toque: o corpo dos dois widgets abre **este app** (`MainActivity`) em todos os estados (reversão da seção 17). Reabrir o app do FatSecret exigiria decisão explícita do usuário.
- Não inicialize um novo repositório Git, mova o projeto ou tente limpar o repositório localizado em `C:\Users\joaov` sem autorização explícita.
- Não repita a implementação já concluída nesta sessão. Comece pela verificação do estado e pela nova necessidade apresentada pelo usuário.

## 13. Sessão 2026-07-25 — gráfico semanal de calorias no widget alto

### Objetivo da sessão

O usuário pediu, como uso do espaço vertical extra do widget, um **gráfico de
barras das calorias da semana com uma linha de meta**. Confirmado com o usuário
antes de implementar (o handoff exigia definir o conteúdo, não inventá-lo).

### O que foi feito

- O widget agora tem quatro variantes responsivas: `COMPACT`/`MEDIUM` (uma linha,
  inalteradas) e as novas `TALL_NARROW` (150×200dp) e `TALL` (250×200dp). Ao
  aumentar o widget verticalmente, o launcher promove para as variantes altas.
- Nas variantes altas, o **cabeçalho de uma linha continua idêntico** no topo
  (calorias + macros no largo, só calorias no estreito) e o espaço novo abaixo
  recebe um gráfico de barras: uma barra por dia (mais antigo à esquerda, hoje à
  direita), rótulos de dia da semana (`D S T Q Q S S`, hoje em negrito) e a meta
  de calorias desenhada como uma linha fina cruzando o gráfico. Barras acima da
  meta ficam vermelhas (`WidgetColors.over`), abaixo ficam verdes (`accent`),
  reaproveitando a linguagem do resto do widget.
- Como o Glance não tem peso fracionário de layout, o gráfico é medido em dp a
  partir da altura nominal do bucket (`SizeMode.Responsive` pré-gera todos os
  tamanhos). Sem histórico, mostra "Sem histórico semanal".

### Camada de dados (necessária: o widget renderiza do snapshot persistido)

- `NutritionSnapshot` ganhou `weeklyCalories: List<Double>` (7 dias, do mais
  antigo ao mais recente terminando hoje; dia sem registro = 0.0). Persistido em
  `NutritionCacheStore` como string CSV na chave `weekly_calories`.
- `saveSuccess(..., weekly: List<Double>? = null)`: quando `weekly` é nulo (a
  busca semanal falhou) o histórico anterior é preservado — uma falha nunca zera
  o gráfico.
- `FatSecretRepository.sync()` agora, após somar o dia, chama
  `fetchWeeklyCalories()` (best-effort, em `runCatching` — não derruba o sync).
  Ela usa o endpoint **já existente** `foodClient.getMonth` (uma chamada, ou duas
  quando a janela de 7 dias cruza o limite do mês) e sobrescreve o slot de hoje
  com o total recém-somado. **Não** foram tocados OAuth, endpoints, assinatura,
  tokens nem credenciais.

### Arquivos alterados

- `NutritionCacheStore.kt`: campo `weeklyCalories`, leitura/escrita CSV, novo
  parâmetro em `saveSuccess`.
- `FatSecretRepository.kt`: `fetchWeeklyCalories()` e chamada dentro de `sync()`.
- `NutritionWidget.kt`: buckets `TALL`/`TALL_NARROW`, `TallContent`,
  `WeeklyChart`, `weekdayLetter`, imports de `java.time`.
- `nutrition_widget_info.xml`: **não** alterado (já permitia resize vertical;
  `targetCellHeight="1"` e os limites de 40dp da variante mínima preservados).

### Testes e validação

- `testDebugUnitTest assembleDebug` com o JBR do Android Studio: `BUILD
  SUCCESSFUL`, 44 tarefas. APK instalado no `emulator-5554`.
- Confirmado por `run-as` que `sync()` persiste `weekly_calories` com 7 valores e
  `status=SUCCESS`, sem crash.
- Validação visual no Pixel Launcher (tema escuro): variante mínima de uma linha
  intacta; `TALL` e `TALL_NARROW` renderizam com o cabeçalho fixo no topo; estado
  vazio "Sem histórico semanal"; e o gráfico populado (barras verdes/vermelhas,
  linha de meta, rótulos de dia, hoje em negrito) usando dados de teste semeados
  temporariamente no DataStore e depois **restaurados** byte a byte.

### Pendências/limitações

- Como a conta conectada no emulador não tinha calorias registradas, a validação
  do gráfico populado usou dados sintéticos; o gráfico real aparecerá após um sync
  com histórico de calorias registrado no FatSecret.
- Não validado: tema claro, fontes ampliadas, outros launchers, e a variante alta
  quando `getMonth` cobre parcialmente a janela na virada de mês (o código já
  trata isso com uma segunda chamada, mas não foi exercitado com dados reais).
- README seção 7.1 continua desatualizado (fora do escopo desta sessão).

## 14. Sessão 2026-07-25 (2ª parte) — dois bugs reportados pelo usuário

O usuário reportou, em uso real: (1) tocar no widget não abre o FatSecret nem
faz nada, e (2) o gráfico semanal sempre mostra "Sem histórico semanal" mesmo
com histórico real de calorias no FatSecret.

### Bug 2 — gráfico semanal sempre vazio (causa raiz confirmada e corrigida)

`FatSecretJson.parseMonth` lia a resposta de `food_entries.get_month.v2` no
caminho `month.days.day` (objeto "days" intermediário). A API real do
FatSecret devolve `month.day` diretamente — sem o wrapper "days". Como
consequência, `parseMonth` sempre retornava lista vazia com respostas reais,
e isso nunca foi pego pelos testes porque os fixtures em
`FatSecretJsonTest.kt` reproduziam a mesma forma errada (auto-consistentes
com o bug). Isso não era um bug introduzido nesta sessão — já afetava
silenciosamente `recentDailyCalorieAverage`/`daysRecordedThisMonth`
(features pré-existentes que dependem do mesmo parser), só que sem UI visível
que expusesse o problema até o gráfico semanal ser adicionado.

Corrigido em [FatSecretJson.kt](app/src/main/java/com/example/widgetfatsecret/fatsecret/data/FatSecretJson.kt)
(`parseMonth` agora lê `month["day"]` direto) e nos fixtures de
[FatSecretJsonTest.kt](app/src/test/java/com/example/widgetfatsecret/FatSecretJsonTest.kt)
(ajustados para a forma real da API, incluindo `from_date_int`/`to_date_int`
como a API realmente envia). `testDebugUnitTest` passa.

**CONFIRMADO com dados reais da API.** Um interceptor de logging temporário
(`DiagLoggingInterceptor`, já removido) capturou as respostas reais no
emulador: `get_month.v2` devolve `{"month":{"day":[...],"from_date_int":...,
"to_date_int":...}}` com ~19 dias de calorias reais (230, 1079, 1594, ...).
Com o fix, o parser lê corretamente e o widget alto renderiza o gráfico
semanal populado (barras verde/vermelha conforme a meta, linha de meta,
rótulos de dia, hoje em negrito). Verificado por screenshot no emulador.

Observação sobre "hoje zerado": `get.v2` para o dia atual devolve
`{"food_entries": null}` quando não há refeição registrada no dia — é o
comportamento correto (o dia começou há poucas horas). Por isso o bloco
"0 / 1.000 kcal" fica zerado enquanto o gráfico semanal (dias anteriores)
aparece normalmente. Não é bug.

### Bug 1 — toque no widget não faz nada (mitigado, causa raiz não confirmada)

Reproduzido no emulador: o toque no corpo do widget abriu corretamente o
fallback (Play Store, já que nem o FatSecret nem a Play Store nativa estão
instalados no emulador), então o mecanismo de clique (`actionStartActivity`
+ `FatSecretApp.openIntent`) funciona no código como estava. O usuário
confirmou que o FatSecret **está** instalado no aparelho real, então o bug
não pôde ser reproduzido aqui.

Hipótese mais provável: `PackageManager.getLaunchIntentForPackage` era
confiado cegamente (sem checar `resolveActivity`) — diferente dos outros
candidatos (Play Store, site), que já validavam com `resolveActivity` antes
de retornar. Se o launch intent do FatSecret vier não-nulo mas não realmente
iniciável (activity desabilitada, quirk de fabricante, etc.), o toque falhava
silenciosamente sem cair em nenhum fallback.

Corrigido defensivamente em
[FatSecretApp.kt](app/src/main/java/com/example/widgetfatsecret/fatsecret/widget/FatSecretApp.kt):
`openIntent` agora valida `resolveActivity` também no branch do launch
intent direto, e todas as chamadas ao PackageManager estão envolvidas em
`runCatching` para nunca deixar uma exceção interromper a cadeia de
fallback — na pior hipótese, cai no site do FatSecret via browser.

**Causa raiz não confirmada** — este é o cenário mais plausível encontrado
por inspeção de código, não uma reprodução direta do bug relatado. Se após
esta correção o toque continuar sem efeito no aparelho real, o próximo passo
é capturar `adb logcat` no momento do toque (procurar
`ActivityNotFoundException`, `SecurityException` ou erros do
`ActionTrampoline` do Glance) para confirmar a causa real.

### Aparelho real: "sincronização falhou" + "autorização inválida ou expirada"

Reportado só no aparelho real (não reproduz no emulador). O emulador, com o
MESMO APK/consumer key/código OAuth, conecta e sincroniza normalmente. A única
diferença relevante é a rede: **a API OAuth 1.0 do FatSecret exige whitelist de
IP** (documentado nas seções sobre limitações da API / linhas ~80 e ~109). O
emulador roteia pela IP da máquina de desenvolvimento (que está na whitelist);
o aparelho real, em WiFi/dados móveis, usa uma IP pública diferente, não
incluída na whitelist → a API rejeita as chamadas e o handshake OAuth falha
(mapeado para AUTH_INVALID/erro genérico).

**Não é bug de código.** Ação do usuário: adicionar a IP pública atual do
aparelho no painel de desenvolvedor do FatSecret, ou usar o app numa rede cuja
IP já esteja na whitelist. Se precisar de diagnóstico definitivo no futuro,
reintroduzir temporariamente um interceptor de logging (sem logar oauth_*) e
inspecionar o `code`/`message` do erro real da API.

### ANR observado no emulador (a investigar, não bloqueante)

Durante o teste apareceu uma vez o diálogo "WidgetFatSecret isn't responding"
logo após tocar em Sincronizar num emulador cold-start. As chamadas de rede
completaram (http=200) segundos depois e o app se recuperou. `sync()` roda em
`viewModelScope.launch` com a rede em `withContext(Dispatchers.IO)`, então em
teoria não bloqueia a main thread; provável causa foi lentidão transitória do
emulador (dex verification + cold start + reinstalação em sequência). Vale
confirmar num emulador quente / aparelho real se o ANR reaparece; se sim,
revisar se `NutritionWidget.updateAll` (chamado logo após `repo.sync()` no
mesmo escopo) está fazendo trabalho pesado no dispatcher errado.

### Arquivos alterados

- `app/src/main/java/com/example/widgetfatsecret/fatsecret/data/FatSecretJson.kt`
- `app/src/test/java/com/example/widgetfatsecret/FatSecretJsonTest.kt`
- `app/src/main/java/com/example/widgetfatsecret/fatsecret/widget/FatSecretApp.kt`
- (temporário, criado e removido nesta sessão: `DiagLoggingInterceptor.kt` +
  uma linha em `AppContainer.kt` — usado só para capturar as respostas reais da
  API; nenhum vestígio permanece no código)

### Testes/validação

- `testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL (build final, sem o
  interceptor de diagnóstico).
- Gráfico semanal confirmado populado no emulador com dados REAIS da API
  (screenshot). Bug 2 corrigido de ponta a ponta.
- Toque no corpo do widget testado via `adb shell input tap`: abriu o
  fallback corretamente (confirma que o mecanismo de clique básico não está
  quebrado no código atual).

## 15. Sessão 2026-07-25 (3ª parte) — widget de peso (`WeightWidget`)

Novo widget de tela inicial dedicado a peso e tendências, separado do
`NutritionWidget` conforme a restrição já registrada na seção de limites.

### Endpoints de peso — descobertos e VERIFICADOS na API real

Não havia nenhuma fonte de dados de peso no projeto. Antes de escrever
qualquer parser, uma sonda temporária (`WeightProbe`, já removida) testou
candidatos contra a conta conectada. Resultado real:

| Método | Resultado |
|---|---|
| `profile.get` | ✅ `{"profile":{"goal_weight_kg":"83.0000","last_weight_kg":"104.9000","last_weight_date_int":"20656","weight_measure":"Kg","height_cm":"171.00"}}` |
| `weights.get_month.v2` | ✅ `{"month":{"day":[{"date_int":"20640","weight_kg":"107.7000"},...],"from_date_int":...,"to_date_int":...}}` |
| `profile.get.v2` | ❌ erro 10 "Unknown method" |
| `weight.get_month.v2` | ❌ erro 10 "Unknown method" |

**Não "modernize" esses nomes** — as variantes `.v2` do profile e o singular
`weight.` não existem. O envelope de `weights.get_month.v2` é idêntico ao de
`food_entries.get_month.v2` (`month.day`), então a travessia foi extraída para
`FatSecretJson.monthDays()` e é compartilhada pelos dois — sem duplicação.

### Reúso (nada de fonte de dados ou regra de negócio duplicada)

- `FatSecretWeightClient` usa o **mesmo** `FatSecretService`/Retrofit/OkHttp,
  herdando assinatura OAuth 1.0a, timeouts e tratamento de erro.
- `syncWeight()` vive dentro do `FatSecretRepository` existente (não há um
  segundo repositório) e reaproveita `tokenStore`, checagem de credenciais e
  `mapError`.
- O sync de peso é chamado de dentro de `sync()`, então **pega carona** em todo
  o agendamento que já existia (abrir o app, salvar meta, worker de 30 min) —
  é isso que faz uma pesagem registrada no FatSecret aparecer sozinha na tela
  inicial, sem agendamento novo. É best-effort: falha de peso nunca derruba o
  sync nutricional, e nunca apaga o histórico já cacheado.
- `FatSecretViewModel.updateWidgets()` centraliza o refresh dos dois widgets.
- Aritmética isolada em `WeightCalculator` (domínio puro), fora do widget.

### Estados-limite cobertos (exigidos e testados)

- **Sem registros** → "Sem pesagens".
- **Sem meta** → "Sem meta definida" (meta 0/ausente vira `null`, nunca uma
  meta de zero quilos).
- **Uma única pesagem** → "Primeira pesagem"; delta/total/média/tendência ficam
  `null` em vez de exibir "0,0 kg" enganoso.
- Pesagem só no perfil (fora da janela buscada) é incorporada, então o usuário
  ainda vê um peso atual.

### Tom (restrição de design deliberada)

O widget relata direção, nunca veredito. **Ganhar peso NÃO usa o vermelho de
"excedeu"** do widget nutricional — a pessoa pode estar em bulking ou
recuperação. Verde/accent só indica movimento em direção à meta *do próprio
usuário*; o resto fica neutro. Rótulos são descritivos ("Perdendo", "Ganhando",
"Mantendo") e a direção usa setas de texto (↓↑→), não ícones — assim nunca
falham em carregar no processo do launcher.

### Armadilha do Glance descoberta (importante)

Com `SizeMode.Responsive`, o Glance **compõe o RemoteViews contra a altura do
BUCKET**, não a altura real do widget. Duas consequências, ambas encontradas na
prática nesta sessão:

1. Alturas fixas derivadas por subtração de `LocalSize` estavam erradas — as
   três linhas de stat foram espremidas até a última colidir com a linha da
   meta. Corrigido usando alturas naturais.
2. Um bucket menor que o conteúdo faz o excedente ser **recortado no bake**;
   nenhuma altura real na tela traz de volta. O gráfico simplesmente não
   aparecia. Por isso os buckets agora têm alturas dimensionadas ao conteúdo:
   `TALL_NARROW`/`MEDIUM_TALL` = 170dp (herói+stats+meta ≈163dp) e
   `LARGE` = 235dp (o mesmo + gráfico de 56dp ≈227dp).

O gráfico usa uma coluna por pesagem (máx. 14) em vez de 30 slots por dia:
30 slots geravam ~90 views aninhadas para meia dúzia de pontos, o que é muito
para RemoteViews. Escala vertical é min..max (não zero-based, senão a linha
ficaria plana) e os dois rótulos de borda tornam a escala comprimida explícita.

### Arquivos criados/alterados

Criados: `domain/WeightDomain.kt`, `data/WeightCacheStore.kt`,
`data/remote/FatSecretWeightClient.kt`, `widget/WeightWidget.kt`,
`widget/WeightWidgetReceiver.kt`, `res/xml/weight_widget_info.xml`,
`test/WeightCalculatorTest.kt`.
Alterados: `FatSecretJson.kt`, `FatSecretRepository.kt`, `AppContainer.kt`,
`FatSecretViewModel.kt`, `SyncWorker.kt`, `AndroidManifest.xml`, `strings.xml`.

### Validação

- `testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL (14 testes novos).
- Cache de peso confirmado no dispositivo com dados REAIS: 8 pesagens
  (108,4→104,9 kg), meta 83,0, `status=SUCCESS`. A entrada `20632` veio do mês
  anterior, o que **exercitou a lógica de virada de mês com dados reais**.
- Variantes SMALL/WIDE e MEDIUM_TALL validadas por screenshot; todos os valores
  conferidos à mão (média −1,0 kg/sem sobre 24 dias, faltam 21,9 kg, progresso
  ~14%).
- O gráfico foi validado por um teste de isolamento (build temporária só com
  herói+gráfico): os 8 pontos renderizaram descendo de 108,4 a 104,9 com os
  rótulos de escala. **A variante LARGE completa (stats+meta+gráfico juntos)
  não foi vista numa única captura** porque a grade do launcher do emulador
  estava cheia e não permitiu um widget de ≥235dp. Vale confirmar num aparelho
  com espaço.

### Pendências

- Confirmar a variante LARGE completa num aparelho real com espaço na grade.
- Não validados: tema claro, fontes ampliadas, outros launchers, unidade `Lb`
  (o código converte, mas a conta de teste usa `Kg`).
- O fuso do emulador foi alterado por mim de `GMT` para `America/Sao_Paulo`
  durante a investigação do bug de data; continua assim.

## 16. Sessão 2026-07-25 (4ª parte) — revisão da responsividade

### CORREÇÃO de um erro registrado na seção 15

A seção 15 afirma que o Glance "compõe contra a altura do bucket e recorta o
excedente no bake". **Isso está errado** e a evidência desta sessão contradiz:
o layout é medido pelo launcher no **tamanho REAL** do widget; o bucket
(`LocalSize`) apenas escolhe qual variante é composta. O gráfico sumia porque o
conteúdo (~219dp) estourava o widget real (~212dp) e o último filho era
espremido a zero — não por recorte de bake.

A consequência prática continua a mesma, mas por outro motivo: **o conteúdo de
cada faixa deve ser orçado contra a altura do BUCKET**, que por definição é
≤ a real. Orçar contra qualquer coisa maior é o que produziu, três vezes nesta
sessão, linhas se sobrepondo.

### Causa raiz do desperdício de espaço relatado

A escada de buckets era esparsa demais (40dp e 200dp, sem nada entre). Um
widget de 2 linhas (~212dp reais) caía no bucket de 40dp, então o layout de UMA
linha era esticado por 212dp — daí as grandes áreas vazias e a tipografia
pequena. Não era um problema de densidade de tela.

### Como a responsividade foi implementada

Novo arquivo `widget/WidgetScale.kt`, compartilhado pelos dois widgets:

- `WidgetSizes.ALL` — escada de 8 buckets (larguras 150/260/340dp × alturas
  80/120/180/250dp, só nas combinações que launchers realmente produzem).
- `HeightBand` (COMPACT/REGULAR/COMFORTABLE/SPACIOUS) e `WidthBand`
  (NARROW/WIDE/EXTRA_WIDE), resolvidos por `scaleFor(LocalSize.current)`.
- `WidgetScale` devolve tipografia, paddings, gaps, espessura de barra, altura
  de gráfico e nº de linhas de stats por faixa.

Não há multiplicador de escala: cada faixa tem valores próprios e, mais
importante, **composição própria** — faixas maiores mostram mais elementos, não
os mesmos elementos maiores. Como um aparelho grande (S23 Ultra) dá mais dp por
célula, ele cai numa faixa mais alta sem que o código conheça o dispositivo.

### Breakpoints

| Faixa | Altura do bucket | Conteúdo |
|---|---|---|
| COMPACT | <100dp | herói + legenda (+ stats ao lado, se largo) |
| REGULAR | 100–169dp | + 2 linhas de stats |
| COMFORTABLE | 170–239dp | + 3ª linha + barra de meta; gráfico no widget nutricional |
| SPACIOUS | ≥240dp | + gráfico; no widget de peso a lista cai para 1 linha |

Largura: NARROW <220dp, WIDE 220–319dp, EXTRA_WIDE ≥320dp (só o herói ganha
passo extra: +4sp / +6sp, para não achatar a hierarquia).

Herói: 24→32→34→42sp (mais o passo de largura). Antes era fixo em 24/30sp.

### Onde o comportamento muda

- 1 linha → 2 linhas: herói de 24 para ~36sp, entram as linhas de stats, o
  conteúdo passa a preencher a altura via spacers com peso.
- 2 linhas → 3 linhas: entra o gráfico; no widget de peso a lista de stats cede
  linhas para o gráfico não colidir com a meta.
- Largura ≥320dp: herói ganha mais 6sp e as linhas param de quebrar.

### Testado

- Pixel (emulator-5554, 1080×2400 @420dpi): 1, 2 e 3 linhas, ambos os widgets,
  tema escuro; tema claro confirmado (superfície branca, contraste preservado —
  `WidgetColors` não foi alterado).
- Geometria grande simulada via `wm size 1440x3088` + `wm density 500`
  (≈460dp de largura, comparável ao S23 Ultra): tipografia cresce, gráfico
  aparece, sem sobreposição. Depois revertido com `wm size/density reset`.
- `testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.

### NÃO testado (importante)

- Nenhum aparelho físico. Samsung/Motorola/Xiaomi **não** foram testados —
  apenas o Pixel Launcher do emulador e uma geometria grande simulada. Grades
  de OneUI/MIUI podem produzir dp por célula diferentes e cair em outra faixa.
- Fontes ampliadas (font scale >1.0) continuam não validadas, e são o risco
  mais provável de sobreposição, já que os orçamentos de altura assumem escala
  1.0.
- `weight_widget_info.xml` passou a nascer com `targetCellWidth/Height = 3×2`
  (o widget tem conteúdo para isso); o mínimo continua 40dp, então ainda dá
  para encolher para uma linha.

## 17. Sessão 2026-07-25 (5ª parte) — sincronização determinística + reversão do clique

### Causa raiz do atraso/inconsistência

**1. Todo o sync rodava em `viewModelScope` (causa principal).**
`syncNow()` era `viewModelScope.launch { repo.sync(); updateWidgets() }`. A
sequência é buscar → persistir → atualizar widgets, e `repo.sync()` faz até 4
chamadas HTTP (diário, mês, peso do mês, perfil), cada uma com timeout de 30s.
Se a Activity terminasse nesse meio-tempo (voltar, girar, sair do app), o job
era cancelado. Como `saveSuccess()` acontece ANTES de `updateWidgets()`, o
desfecho comum era: **dados gravados no disco, widgets nunca avisados**. Na
abertura seguinte um novo sync rodava e só então o widget mostrava o valor —
exatamente o "às vezes preciso sincronizar mais de uma vez".

**2. Nenhuma guarda de reentrância (causa da não-determinância).**
O botão não tinha `enabled=false` e `init {}` também chama `syncNow()`. Dois
syncs simultâneos executavam `setLoading()` → fetch → `saveSuccess()` e suas
escritas no DataStore intercalavam: **quem terminasse por último vencia**, então
um sync lento podia sobrescrever o resultado de um mais novo. O worker
periódico de 30 min podia colidir da mesma forma.

**3. Latência do próprio Glance (contribuinte, não bug nosso).**
`updateAll()` é assíncrono: o Glance compõe dentro de um
`androidx.glance.session.SessionWorker` do WorkManager. Nos logs desta sessão
esses workers levaram 46–60s de vida útil. Somado ao item 1, fazia a
atualização parecer aleatória.

### Descartados (investigados e sem culpa)

- **Cache HTTP**: `AppContainer` monta o `OkHttpClient` **sem** `.cache(...)`,
  então o OkHttp não tem como servir resposta cacheada. Além disso cada
  requisição carrega `oauth_nonce`/`oauth_timestamp` novos, então nem URL
  repetida existe.
- **DataStore stale**: `edit {}` suspende até o commit, e `provideGlance` lê
  `.first()` da MESMA instância no MESMO processo (os receivers não declaram
  `android:process`). A ordem persistir→atualizar já estava correta dentro de
  `sync()`.
- **WorkManager duplicando**: `ensurePeriodic` usa `KEEP`. O problema não era
  duplicação de agendamento, e sim sobreposição com o sync manual.

### Correção

- `AppContainer` ganhou `appScope` (SupervisorJob, sobrevive a qualquer tela) e
  `syncAndRefresh()`: **o único lugar** onde a sequência buscar → persistir →
  atualizar ambos os widgets existe, com *single-flight* (chamadas concorrentes
  compartilham a mesma execução, com troca de handle por identidade no
  `finally` para nunca limpar uma execução mais nova).
- `FatSecretRepository.sync()` passou a ser serializado por um `Mutex`
  (`syncLocked()` faz o trabalho). É o backstop caso alguém chame o repositório
  direto.
- `FatSecretViewModel` expõe `isSyncing` e apenas **observa** a execução
  compartilhada; cancelar a observação não cancela mais o sync.
- `SyncWorker` usa o mesmo `syncAndRefresh()`, então periódico e manual não
  competem.
- Botão "Sincronizar" desabilita e vira "Sincronizando…".

### Reversão do clique (pedido do usuário)

O corpo dos DOIS widgets agora abre **este app** (`MainActivity`) em todos os
estados. Antes, no estado com dados, abria o app do FatSecret com fallback para
Play Store/site. Os `clickable` redundantes na linha de meta / subtítulo foram
removidos (faziam o mesmo destino do corpo).

**Consequência**: `widget/FatSecretApp.kt` ficou **sem uso**, e o bloco
`<queries>` do manifest existia para ele. **Ambos foram removidos** na sessão
seguinte (ver seção 18) — a limpeza foi pedida explicitamente.

### Arquivos alterados

`AppContainer.kt`, `FatSecretRepository.kt`, `FatSecretViewModel.kt`,
`SyncWorker.kt`, `AppScreens.kt`, `MainActivity.kt`, `NutritionWidget.kt`,
`WeightWidget.kt`.

### Validação

- `testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL.
- App abriu, sincronizou (657 kcal / 6 registros) e o widget mostrou **657
  imediatamente** na mesma verificação.
- Clique no widget confirmado abrindo `MainActivity` via `dumpsys window`
  (`mCurrentFocus=...MainActivity`).
- **Não testado**: o cenário específico de cancelamento (tocar Sincronizar e
  sair do app no meio) não foi reproduzido de forma controlada — a correção é
  estrutural e verificada por leitura, não por repro do bug original.

## 18. Sessão 2026-07-25 (6ª parte) — remoção do código morto do clique no FatSecret

### Decisão explícita

A seção 17 deixou dois candidatos a limpeza e a seção de restrições proibia
mexer em `<queries>` sem decisão explícita. **Esta sessão foi essa decisão**: o
usuário pediu a remoção dos dois.

### O que foi removido

- `app/src/main/java/com/example/widgetfatsecret/fatsecret/widget/FatSecretApp.kt`
  (arquivo inteiro). Confirmado sem nenhuma referência em `app/src/main` e
  `app/src/test` — os únicos hits de `FatSecretApp` fora dele eram o comentário
  do manifest, a documentação e artefatos em `app/build/`.
- O bloco `<queries>` de `app/src/main/AndroidManifest.xml` (visibilidade de
  `com.fatsecret.android` + intents `market` e `https`) e o comentário que o
  explicava.

### Por que remover `<queries>` é seguro

A filtragem de visibilidade de pacotes do Android 11+ afeta apenas consultas —
`getLaunchIntentForPackage`, `resolveActivity`, `queryIntentActivities`. Depois
da remoção de `FatSecretApp.kt`, **nenhum código do app chama qualquer uma
delas**. O único `startActivity(Intent(ACTION_VIEW, ...))` restante é o do fluxo
OAuth em `MainActivity.kt` (abre a URL de autorização no navegador), e
`startActivity` direto **não** é afetado pela filtragem — o sistema resolve o
intent fora do processo do app.

### Validação

`JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' ./gradlew.bat testDebugUnitTest assembleDebug`
→ **BUILD SUCCESSFUL** (44 tasks, 12 executadas). Sem testes em execução no
device nesta sessão — a mudança é remoção de código morto, sem alteração de
comportamento.

### Documentação atualizada

- `README.md`: a seção "Toque abre o FatSecret" virou "Toque abre este app" e
  não menciona mais `FatSecretApp.kt` nem a obrigatoriedade do `<queries>`.
- `handoff.md`: seções 12 (restrições), 1 (componentes) e 17 (consequência)
  atualizadas para refletir a remoção.

---

## 19. Sessão 2026-07-25 (7ª parte) — divergência nos indicadores de peso

### Sintoma relatado

No app do FatSecret: peso inicial 128,5 kg, atual 104,4 kg, "Perdeu até agora:
24,1 kg". No widget: **Total −4,0 kg** e barra de meta quase vazia.

### Causa 1 — âncora errada (bug real)

`fetchWeightWindow` busca **30 dias** (`WEIGHT_WINDOW_DAYS`). `WeightCalculator`
usava `merged.first()` — a pesagem mais antiga **da janela** — como âncora de
`totalDelta` **e** de `goalProgress`. Com a janela começando em 108,4 kg:

- total = 104,4 − 108,4 = **−4,0 kg**
- progresso = (108,4−104,4)/(108,4−83) = **15,7 %** ← a barrinha vazia

A média semanal (−1,0 kg/sem) sempre esteve certa: ela **deve** ser da janela.

### Causa 2 — a API não expõe o peso inicial

`profile.get` devolve apenas `goal_weight_kg`, `last_weight_kg`,
`last_weight_date_int`, `weight_measure`, `height_cm` (ver seção 15). **Não há
campo de peso inicial.** A caminhada mês a mês por `weights.get_month.v2` foi
implementada e executada no device: a pesagem mais antiga que a API devolve é
**123,0 kg em 2026-03-04**, e aumentar a tolerância de 6 para 12 meses vazios
deu o mesmo resultado — ou seja, não é heurística parando cedo, os 128,5 kg
simplesmente **não existem no diário**. É um campo de perfil que a plataforma
guarda mas não publica.

### Solução

Três camadas, nesta ordem de precedência:

1. `startOverrideKg` — peso inicial digitado pelo usuário (`GoalsStore`,
   chave `start_weight_kg`). **Vence sempre.**
2. `baseline` — pesagem mais antiga descoberta pela caminhada, persistida em
   `WeightCacheStore` (`baseline_date`/`baseline_kg`).
3. `windowFirst` — a mais antiga da janela de 30 dias (fallback).

`WeightStats.first` passou a ser a âncora efetiva; `WeightStats.windowFirst` foi
adicionado e é o que ancora **média semanal e tendência** — elas continuam
medindo o ritmo recente e não podem ser diluídas por um ano de histórico.

A caminhada (`walkBackForFirstWeighing`) roda **depois** de a janela já estar
persistida, dentro de `runCatching`, e só quando não há baseline guardado (ou
quando a janela contém algo mais antigo que o guardado). Limites:
`MAX_MONTHS_BACK = 60`, `MAX_EMPTY_MONTHS = 12`. Custo real medido: ~15
requisições, **uma única vez**.

### Causa 3 — corrida entre dois `updateWidgets()`

Ao adicionar o campo, o botão Salvar passou a disparar **duas** corrotinas
(`saveGoals` e `saveStartWeight`), cada uma com seu `updateWidgets()`. As duas
sessões do Glance corriam e a que vencia podia ter começado antes da segunda
gravação: o widget mantinha o valor antigo até o próximo sync. **Reproduzido e
confirmado no emulador** (persistido 130,0; widget preso em −24,1 por 3 min;
só mudou ao tocar Sincronizar).

Correção: uma única `saveGoals(goals, startWeightKg)` — grava as duas coisas em
sequência e chama `updateWidgets()` **uma vez**. Depois disso, salvar atualiza o
widget sem sync extra (sessão 18:54:22 → SUCCESS 18:55:07, ~45 s, que é a
latência normal do `SessionWorker`).

> **Regra geral:** nunca dispare dois `updateAll()` concorrentes para o mesmo
> widget a partir de escritas independentes. Agrupe as escritas e atualize uma vez.

### Arquivos alterados

`WeightDomain.kt`, `WeightCacheStore.kt`, `GoalsStore.kt`,
`FatSecretRepository.kt`, `FatSecretViewModel.kt`, `AppScreens.kt`,
`MainActivity.kt`, `WeightCalculatorTest.kt`.

### Validação

`./gradlew testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**;
`WeightCalculatorTest` com **20 testes, 0 falhas** (5 novos cobrindo baseline,
baseline dentro da janela, baseline com uma única pesagem, override manual e
override não-positivo).

No emulador, com 128,5 kg informado: **Total −24,1 kg**, barra ~53 %,
"faltam 21,4 kg" — idêntico ao app do FatSecret.

### Pendências conhecidas

- O peso inicial é local ao aparelho (como as metas de kcal/macros) e **não** é
  limpo no disconnect, pois vive em `nutrition_goals`. Se o usuário conectar
  outra conta, precisa reajustar.
- ~~`README.md` seção 4 não documentava a limitação do peso inicial~~ —
  documentado nesta sessão (subseção "O **peso inicial** tem a mesma limitação").

## 20. Sessão 2026-07-25 (8ª parte) — início da evolução para "Nutri Insights" (planning.md, Etapas 0–1)

### Contexto

Existia já um `planning.md` na raiz descrevendo uma evolução grande do app —
de "app de widgets + uma tela" para "Nutri Insights", um app com navegação por
5 abas (Hoje/Tendências/Padrões/Consistência/Peso) + Metas e conta, mantendo os
dois widgets **totalmente intocados**. O plano tem 12 etapas (§8–9 do
`planning.md`); esta sessão implementou as duas primeiras, que são
deliberadamente "preparação de terreno": nenhuma delas tem UI nova nem altera
o comportamento visível do app ou dos widgets.

### O que foi feito

**Etapa 0 — Rede de segurança:**
- O commit inicial e o `.gitignore` já existiam e já estavam corretos
  (`local.properties`, `build/`, `.idea/*` ignorados) — nada a corrigir aí.
- Criado [`docs/widget-smoke-test.md`](docs/widget-smoke-test.md): o checklist
  de fumaça dos 12 itens que já vivia dentro do `planning.md` §11, extraído
  para um documento próprio com uma tabela de registro de execuções.
- Criada a tag anotada `baseline-widgets` no commit inicial (`5f744e5`).
- **Pendência real:** o checklist MANUAL (itens 1–12) não foi executado — este
  ambiente de sessão não tem dispositivo/emulador Android conectado. Só o
  checklist automatizado (`testDebugUnitTest` + `assembleDebug`) rodou.

**Etapa 1 — Camada de histórico (sem UI):**
Objetivo: persistir uma série diária (calorias + macros por dia) para
alimentar as futuras telas Tendências/Padrões/Consistência, sem tocar em nada
que os widgets já leem.

- [`fatsecret/data/history/NutritionHistoryStore.kt`](app/src/main/java/com/example/widgetfatsecret/fatsecret/data/history/NutritionHistoryStore.kt) —
  DataStore novo (`nutrition_history`, arquivo próprio, independente de
  `nutrition_cache`), CSV compacto `dateInt:cal:protein:carbs:fat` no mesmo
  padrão de `WeightCacheStore`. `merge()` faz upsert por `dateInt` e nunca
  apaga dias já persistidos que não vieram na leva atual; bounded a 400 dias.
- [`fatsecret/data/history/HistoryRepository.kt`](app/src/main/java/com/example/widgetfatsecret/fatsecret/data/history/HistoryRepository.kt) —
  lado de leitura (`trend`/`pattern`/`consistency`, Flows derivados de
  `daysFlow`) e o único lado de escrita (`refresh()`, busca via
  `foodClient.getMonth` — 1 request por mês, ver risco R6 do planning.md).
  **`refresh()` não tem nenhum chamador ainda** — é intencional, nenhuma tela
  ou worker deve acioná-lo antes de existir uma tela que precise dele.
- Três calculadores puros (sem Android, 100% testáveis em JVM) em
  [`fatsecret/domain/history/`](app/src/main/java/com/example/widgetfatsecret/fatsecret/domain/history/):
  - `TrendCalculator` — média num período, média do período anterior,
    variação, e a lista dia-a-dia com `null` explícito nos dias sem registro
    (nunca zero — regra do deck, planning.md §0).
  - `PatternCalculator` — média de calorias agrupada por dia da semana.
  - `ConsistencyCalculator` — tem DUAS assinaturas: uma janela rolante
    (`windowDays` terminando em `today`) e uma janela explícita
    (`windowStart`/`windowEnd`). **A janela explícita existe porque a rolante
    nunca produz o estado `FUTURE`** (o range sempre termina em `today`, então
    nenhum dia é `> today`) — só faz sentido para o futuro calendário mensal
    (Etapa 8), que vai mostrar o mês inteiro incluindo dias que ainda não
    aconteceram. Isso foi descoberto por um teste que falhava (ver "Testes"
    abaixo) — vale a pena ler o comentário no arquivo antes de usar essa
    classe na Etapa 8.
- `AppContainer` ganhou `historyStore` + `historyRepository`. **Nada mais em
  `AppContainer` mudou** — `syncAndRefresh()`, a ordem
  fetch→persist→updateAll dos dois widgets, e o único ponto de sync
  continuam exatamente como estavam.

### Por que isso é "preparar o terreno"

Nenhum arquivo em `fatsecret/widget`, `fatsecret/work`, `fatsecret/oauth`, ou
os stores/DataStores existentes (`NutritionCacheStore`, `WeightCacheStore`,
`GoalsStore`, `TokenStore`) foi tocado. `AppContainer.syncAndRefresh()`
continua sendo o único ponto de sync dos widgets. A única mudança de
comportamento em runtime é: mais duas chaves de `AppContainer` existem (mas
nada as chama ainda). Etapas 4/6/7/8 (telas que consomem histórico) poderão
simplesmente ler `container.historyRepository` sem precisar desenhar a
camada de dados do zero.

### Testes e validação

- 15 testes novos: `TrendCalculatorTest` (4), `PatternCalculatorTest` (3),
  `ConsistencyCalculatorTest` (4 — um deles pegou o bug de design do
  `ConsistencyCalculator` descrito acima, forçando a segunda assinatura).
- `./gradlew :app:testDebugUnitTest` → **68 testes, 0 falhas** (53 antigos +
  15 novos).
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**.
- `grep -rn "import com.example.widgetfatsecret.ui" app/src/main/java/.../fatsecret/`
  → vazio, confirma a regra de ouro do planning.md §5 (nada em `fatsecret/`
  importa `ui/`).
- **Não validado nesta sessão** (sem dispositivo): nenhuma verificação visual
  dos widgets. Como nenhum arquivo de widget foi tocado, o risco é baixo, mas
  o checklist manual de `docs/widget-smoke-test.md` continua formalmente
  pendente — ver seção 9/10/11 acima.

### Arquivos criados/alterados

Criados: `docs/widget-smoke-test.md`,
`fatsecret/data/history/NutritionHistoryStore.kt`,
`fatsecret/data/history/HistoryRepository.kt`,
`fatsecret/domain/history/TrendCalculator.kt`,
`fatsecret/domain/history/PatternCalculator.kt`,
`fatsecret/domain/history/ConsistencyCalculator.kt`,
`test/TrendCalculatorTest.kt`, `test/PatternCalculatorTest.kt`,
`test/ConsistencyCalculatorTest.kt`.
Alterados: `AppContainer.kt` (só adições — `historyStore`, `historyRepository`),
`planning.md` (status das Etapas 0 e 1 marcado, ver topo do arquivo),
`handoff.md` (esta seção + seções 1/9/10/11 apontadas para o novo fluxo).

Tag criada: `baseline-widgets` (no commit `5f744e5`).

### Pendências desta sessão

- Checklist manual da Etapa 0 (dispositivo/emulador) — ver seção 10.
- `HistoryRepository.refresh()` não é chamado por ninguém ainda — correto por
  ora, mas quem implementar a Etapa 4/6/7/8 precisa decidir de onde disparar
  o refresh (nunca do `init{}` de uma tela — ver risco R6/R5 do planning.md).
- Nada foi commitado nesta sessão — as mudanças estão no working tree,
  aguardando revisão/commit explícito do usuário.

---

## 21. Sessão 2026-07-25 (9ª parte) — Etapa 2 do planning.md: design system

### Contexto

Continuação direta da seção 20. As Etapas 0 (rede de segurança) e 1 (camada de
histórico, sem UI) já estavam feitas. Esta sessão implementou a **Etapa 2 —
design system**: tokens de cor, tipografia e componentes base do deck "Nutri
Insights", **sem lógica de tela e sem tocar nos widgets**. É a última etapa antes
da UI real (Etapa 3 mexe em `MainActivity`).

### O que foi feito

**Tema reescrito (`ui/theme/`):**
- `Color.kt` — tokens do protótipo (`Nutri Insights.dc.html`), escuro e claro:
  `bg #0A0F1A`, `surf #131B2B`, `surf2 #1B2539`, 3 níveis de texto
  (`#E9EFF8/#93A3BD/#64748F`), 5 acentos (mint `#84E0A8`, cyan `#5FC8E8`, amber
  `#E5B15C`, coral `#F0806F`, violet `#9E9BF0`) + `page`. Linhas (`line`/`line2`)
  não vêm com hex no deck — derivadas da superfície para contorno sutil/visível.
- `Theme.kt` — `NutriColors` (data class `@Immutable`) carrega a paleta estendida
  que o `ColorScheme` do Material não comporta, exposta por
  `MaterialTheme.nutriColors` via `staticCompositionLocalOf`. A camada Material3
  (primary=mint, error=coral, surface/background/…) é derivada dos tokens.
  **Dynamic color desligado** — o Material You (wallpaper) atropelaria a paleta do
  deck. O nome `WidgetFatSecretTheme` foi preservado (MainActivity já o chama).
- `Type.kt` — Manrope (UI) + IBM Plex Mono (números/metadados) pedidos pelo deck,
  hoje via **fallback do sistema** (`SansSerif`/`Monospace`) porque `res/font/`
  ainda não empacota os `.ttf` (caminho previsto em planning §3). `Typography`
  completo + `MonoText.metricLarge/metricMedium/meta` (fora do Material porque
  número mono não mapeia nos papéis 1:1). Trocar as duas famílias no futuro não
  mexe em nenhum call-site.

**Componentes novos (`ui/design/`):** `Tokens.kt` (raios 22/13/12dp,
espaçamentos), `StatCard`, `MetricValue` (mono, `tabular-nums`, `null` → "—"
nunca "0"), `MetaChip`, `SyncStatusChip` (+ enum `SyncStatus` — o mapeamento a
partir do `NutritionSnapshot` fica na camada de ViewModel, **não** no design
system, para não acoplar `ui/design` a modelos de dados), `GoalRing` (anel via
Canvas com número central), `BarChart` (+ `BarDatum`; dia ausente vira contorno
tracejado, **fora de qualquer média** — regra do deck; linha de meta tracejada),
`EmptyState` (linguagem descritiva, sem cobrança), `SkeletonBlock` (pulsação;
deve usar a altura do conteúdo final para não deslocar layout), e
`NutriPrimaryButton`/`NutriSecondaryButton` (tokens de botão do deck: mint
preenchido / outline coral). Cada arquivo traz previews `@Preview` em claro **e**
escuro.

### Decisões e sutilezas

- **Todo estado carrega rótulo textual, nunca só cor** (regra do slide 4):
  `SyncStatusChip` e `MetaChip` sempre têm texto; a cor é reforço.
- **`SyncStatus` é enum próprio do design system**, não importa
  `NutritionSnapshot` — mantém a regra de ouro (`ui/design` → nada de
  `fatsecret`). Quem for ligar na Etapa 3+ mapeia snapshot→status no ViewModel.
- `Alignment.LastBaseline` não existe para `Row` (só `Top/Center/Bottom`) — foi
  o único erro de compilação, trocado por `Alignment.Bottom` em `MetricValue`.

### Testes e validação

`:app:assembleDebug` **verde** (compila a UI antiga + o novo design system);
`:app:testDebugUnitTest` **68 testes verdes** (inalterados — o design system é
validado por preview Compose, não por teste JVM; é o que a Etapa 2 do planning
pede). Regra de ouro reverificada: **0 imports de `ui.*` em `fatsecret/`**.
`widget/WidgetColors.kt` **não foi tocado** (risco R7).

### Arquivos criados/alterados

Criados: `ui/design/Tokens.kt`, `StatCard.kt`, `MetricValue.kt`, `MetaChip.kt`,
`SyncStatusChip.kt`, `GoalRing.kt`, `BarChart.kt`, `EmptyState.kt`,
`SkeletonBlock.kt`, `NutriButtons.kt`.
Reescritos: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`.
Atualizados: `planning.md` (Etapa 2 marcada ✅ + "Status real"), `handoff.md`
(esta seção + nota de topo).

### Pendências desta sessão

- **Conferência visual dos previews** (claro/escuro, contraste ≥ 4,5:1) depende
  de Android Studio/dispositivo — não há render de preview neste ambiente. Os hex
  seguem o protótipo, mas a validação visual final fica para quando houver IDE.
- **Checklist manual de fumaça dos widgets** (`docs/widget-smoke-test.md`) segue
  pendente da Etapa 0 e **deve rodar antes da Etapa 3**, que é a primeira a tocar
  em `MainActivity`.
- Nada foi commitado — mudanças no working tree, aguardando revisão do usuário.
- **Próximo passo:** Etapa 3 (casca de navegação: `AppShell` + `NavHost` de 6
  rotas + `navigation-compose`), mantendo `MainActivity`/manifesto/`onNewIntent`
  intocados e a UI antiga atrás da flag `USE_LEGACY_UI`.
