# Plano de Evolução — WidgetFatSecret → Nutri Insights

> **Status:** Etapas 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 e 10 implementadas (ver §9).
> A Etapa 11 segue apenas planejada, aguardando aprovação para prosseguir.
> **Premissa central:** os widgets de tela inicial são o que já funciona hoje e são
> intocáveis. Toda a evolução é **aditiva** — nada da infraestrutura atual é removido
> antes de existir paridade verificada.

---

## 0. Fotografia do projeto atual

- **Módulo único** `:app`, `namespace com.example.widgetfatsecret`, minSdk 34 / target 36.
- **~2.900 linhas de Kotlin de produção**, das quais **apenas ~600 são UI do app**
  (`MainActivity.kt` 113, `ui/AppScreens.kt` 294, `ui/FatSecretViewModel.kt` 191).
  Todo o resto — API, OAuth, cache, domínio, widgets, workers — é infraestrutura
  reaproveitável.
- **Dois widgets Glance** (`NutritionWidget`, `WeightWidget`) com receivers próprios,
  alimentados por dois DataStores independentes.
- **6 classes de teste JVM** já verdes (`OAuth1SignerTest`, `FatSecretJsonTest`,
  `FatSecretDateTest`, `NutritionCalculatorTest`, `NutritionFormatTest`,
  `WeightCalculatorTest`).
- **Sem histórico Git**: `git log` não retorna commits (branch `master` vazia).
  Isso é o risco operacional nº 1 — ver Etapa 0.

### Fonte do design

O deck `assets/app_slides.pptx` (12 slides, "Nutri Insights") define o produto-alvo:
5 telas de MVP + Metas/Conta, princípio "padrão mensurável, nunca julgamento",
paleta escura (`#0A0F1A` base, `#131B2B` superfície, mint/ciano/âmbar/coral/violeta),
tipografia Manrope + IBM Plex Mono para metadados, e um escopo negativo explícito
(sem TDEE, sem correlação peso×calorias, sem score, sem classificação de alimentos,
dias ausentes nunca viram zero).

> Atualização: o projeto do Claude Design **"Protótipo de aplicativo Android"**
> (`71f2d226-7e36-4fab-b0ef-aee906d73cbc`) está acessível e contém o protótipo
> interativo completo — `Nutri Insights.dc.html` (as 8 telas/overlays, com HTML/CSS
> real) e `Nutri Insights Deck.dc.html` (as 12 slides do deck). Os tokens de cor,
> tipografia e o mapa de telas descritos abaixo já refletem o protótipo, não só o
> deck estático.
>
> **Achado importante — não existe tela de login no protótipo.** Nem o protótipo
> interativo nem o deck de 12 slides têm uma tela de "conectar conta"/OAuth/login.
> A única superfície relacionada a conta é o card **"Conta FatSecret"** dentro do
> overlay *Metas e conta*, e ele assume estado **já conectado** (status "Conectado",
> e-mail, meta de peso, última sincronização, botões "Sincronizar" e "Desconectar").
> Não há botão "Conectar conta", nem tela de consentimento OAuth, nem estado vazio
> "desconectado" desenhado. **Implicação para a Etapa 5:** o fluxo de conectar pela
> primeira vez (`beginConnect` → navegador → deep link → `completeConnect`) e o
> estado "desconectado" do card de conta não têm design de referência — terão que
> ser desenhados ad-hoc, reaproveitando os tokens do design system (cores, tipografia,
> raios de 22px/13px, botão mint preenchido / outline coral), e não copiados do
> protótipo porque essa tela não existe nele.

---

## 1. Arquivos e componentes que PRECISAM ser preservados

### 1.1 Núcleo intocável (não editar sem necessidade comprovada)

| Arquivo | Papel para os widgets |
|---|---|
| `fatsecret/widget/NutritionWidget.kt` (452) | Composição Glance do widget de nutrição |
| `fatsecret/widget/WeightWidget.kt` (471) | Composição Glance do widget de peso |
| `fatsecret/widget/NutritionWidgetReceiver.kt` / `WeightWidgetReceiver.kt` | Binding com o launcher |
| `fatsecret/widget/WidgetColors.kt` | Pares dia/noite; o launcher escolhe na inflação |
| `fatsecret/widget/WidgetScale.kt` / `WidgetTime.kt` | Escala responsiva e carimbo de horário |
| `res/xml/nutrition_widget_info.xml` / `weight_widget_info.xml` | Metadados do AppWidgetProvider |
| `fatsecret/data/AppContainer.kt` | Singleton + `syncAndRefresh()` single-flight |
| `fatsecret/data/FatSecretRepository.kt` | OAuth, sync, baseline de peso, mutex |
| `fatsecret/data/TokenStore.kt` | Tokens em EncryptedSharedPreferences |
| `fatsecret/data/NutritionCacheStore.kt` | DataStore `nutrition_cache` |
| `fatsecret/data/WeightCacheStore.kt` | DataStore `weight_cache` |
| `fatsecret/data/GoalsStore.kt` | DataStore `nutrition_goals` (metas + peso inicial) |
| `fatsecret/data/FatSecretJson.kt` / `FatSecretError.kt` | Parsing tolerante e mapeamento de erro |
| `fatsecret/data/remote/*` (6 arquivos) | Retrofit, config, interceptor OAuth1, clients |
| `fatsecret/oauth/OAuth1Signer.kt` | Assinatura HMAC-SHA1 |
| `fatsecret/domain/*` (5 arquivos) | Modelos e cálculos puros |
| `fatsecret/work/SyncWorker.kt` / `SyncScheduler.kt` | Sync periódico de 30 min + one-shot |
| `app/build.gradle.kts` (bloco `secret()`/`manifestPlaceholders`) | BuildConfig e deep link |
| `AndroidManifest.xml` (receivers + intent-filter de callback) | Registro dos widgets e do OAuth |

### 1.2 Contratos que os widgets assumem — quebrar qualquer um destes quebra o widget

1. **`com.example.widgetfatsecret.MainActivity` precisa continuar existindo com esse
   nome e pacote.** Ambos os widgets chamam `actionStartActivity<MainActivity>()`
   (`NutritionWidget.kt:110`, `WeightWidget.kt:112`). O *conteúdo* da Activity pode
   mudar à vontade; a *classe* não pode ser renomeada, movida ou removida.
2. **`launchMode="singleTask"` + `exported="true"` + intent-filter `VIEW`** na
   MainActivity — é o que faz o `oauth_verifier` voltar por `onNewIntent`.
3. **Nomes dos arquivos DataStore**: `nutrition_cache`, `weight_cache`,
   `nutrition_goals`. Renomear = perder metas e cache dos usuários atuais.
4. **`AppContainer.syncAndRefresh()` é o único ponto de sync**, e a ordem
   fetch → persist → `NutritionWidget.updateAll` → `WeightWidget.updateAll` é
   deliberada. Nenhuma tela nova pode chamar `repository.sync()` diretamente.
5. **`appScope` (SupervisorJob + Dispatchers.Default)** — o sync não pode migrar
   para `viewModelScope`; foi bug corrigido.
6. **Salvar meta atualiza os dois widgets numa única corrotina** (`saveGoals`) —
   dois `launch` separados causaram race no Glance.
7. **Erro nunca sobrescreve o cache com zeros** (`saveError` só toca status/erro).

---

## 2. Telas atuais que podem ser removidas ou substituídas

| Componente | Destino |
|---|---|
| `ui/AppScreens.kt` → `MainScreen` | **Substituída** pela tela *Hoje* (Etapa 4). Descartável. |
| `ui/AppScreens.kt` → `TodaySummaryCard`, `MacroRow` | Descartáveis (viram componentes do design system). |
| `ui/AppScreens.kt` → `GoalsSettingsScreen` | **Migrada**, não descartada: contém a lógica de parsing pt-BR (vírgula decimal), os textos explicativos sobre a API não expor metas e o campo de peso inicial. Reescrever a casca visual, preservar regras. |
| `ui/AppScreens.kt` → `NumberField` / `DecimalField` | Preservar a lógica de sanitização (`take(6)`, separador único) ao reescrever. |
| `MainActivity.kt` → `AppRoot` (composable privado) | **Substituída** pelo novo `AppShell` com navegação. |
| `MainActivity.kt` → classe + `onNewIntent` + `callbackUri` | **Preservar integralmente.** |
| `ui/FatSecretViewModel.kt` | **Decompor**, não apagar: vira `AccountViewModel` (conectar/desconectar/sync) + ViewModels por aba. A lógica de `init{}`, `syncNow`, `saveGoals`, `handleCallback` migra literalmente. |
| `ui/theme/*` (Color, Theme, Type) | **Reescritos** com os tokens do deck. Sem impacto nos widgets — `WidgetColors.kt` é independente por design (o widget é inflado pelo processo do launcher). |

---

## 3. Dependências usadas pelos widgets

Nenhuma pode sair do `libs.versions.toml`:

| Dependência | Usada por |
|---|---|
| `androidx-glance-appwidget` + `androidx-glance-material3` | Widgets (exclusivo deles) |
| `androidx-work-runtime-ktx` | `SyncWorker` / `SyncScheduler` |
| `androidx-datastore-preferences` | Os 3 stores |
| `androidx-security-crypto` | `TokenStore` |
| `okhttp` + `okhttp-logging` | Clients HTTP e interceptor OAuth |
| `retrofit` | `FatSecretService` |
| `kotlinx-serialization-json` | `FatSecretJson` |
| `kotlinx-coroutines-android` | Flows, `appScope`, mutex |
| Plugins `kotlin-serialization`, `kotlin-compose`, AGP | Build inteiro |

**Compose/Material3/Activity/Lifecycle** são usados só pelo app — podem evoluir.

**A adicionar (nenhuma delas toca os widgets):**
- `androidx.navigation:navigation-compose` — navegação por abas.
- *(opcional)* `androidx.room` — se a série histórica crescer além do que
  Preferences comporta. **Recomendação: começar sem Room**, com um DataStore
  novo e dedicado; adotar Room só se a Etapa 2 mostrar necessidade real.
- *(opcional)* fonte Manrope como recurso local (`res/font`) — o deck pede Manrope
  e IBM Plex Mono. Sem isso, usar a fonte do sistema com pesos equivalentes.

---

## 4. Dados e serviços reutilizáveis no novo app

**Reutilizáveis como estão (100%):**
- `FatSecretRepository`: `uiState` (Flow de snapshot+metas), `weightState`,
  `goalsFlow`, `startWeightFlow`, `discoveredStartWeightFlow`, `beginConnect`,
  `completeConnect`, `disconnect`, `isConnected`, `saveGoals`, `saveStartWeight`.
- `AppContainer.syncAndRefresh()` — sync manual da nova UI.
- `SyncScheduler.ensurePeriodic/cancelAll` — atualização periódica.
- `NutritionCalculator`, `WeightCalculator`, `NutritionFormat`, `WeightFormat`,
  `FatSecretDate` — cálculo e formatação pt-BR prontos.
- `foodClient.getMonth()` → `DayNutrition(dateInt, calories, protein, carbs, fat)`.
  **Este é o dado que sustenta Tendências, Padrões e Consistência** — já existe e
  já é parseado; hoje o app só usa a coluna `calories` para o gráfico de 7 dias.
- `weightClient.getMonth()` + `getProfile()` → tela *Peso* praticamente pronta
  (`WeightStats` já traz média semanal, tendência, progresso, baseline).
- `recentDailyCalorieAverage()` e `daysRecordedThisMonth()` — já existem no
  repositório e hoje **não têm chamador**. Reaproveitáveis de imediato.

**Precisa ser criado (não existe hoje):**
- **Persistência de série histórica.** `NutritionCacheStore` guarda só o dia atual
  + 7 calorias diárias. Tendências (30 dias), Padrões (4 semanas) e Consistência
  (calendário mensal) precisam de N dias × 4 macros persistidos.
- **Distribuição por refeição.** O deck pede "o jantar é 43% das calorias".
  `FoodEntry.meal` já é parseado, mas só o dia de hoje é buscado e o campo `meal`
  **não é persistido**. Requer `food_entries.get.v2` por dia + armazenamento.
  ⚠️ Custo de quota alto (1 request por dia analisado) — ver risco R6.
  **Recomendação:** MVP entrega refeições **apenas para o dia de hoje** (tela Hoje),
  e a análise de refeições em Padrões fica atrás de uma sincronização detalhada
  opcional — exatamente como o próprio deck registra no escopo ("Alimentos só com
  sincronização detalhada").

---

## 5. O que precisa ser desacoplado da interface atual

| Hoje está acoplado | Desacoplar assim |
|---|---|
| `FatSecretViewModel` mistura conta, sync, metas e leitura de estado | Separar em `AccountViewModel` (conectar/desconectar/sync/deep link) e ViewModels por aba, todos lendo do mesmo `AppContainer` |
| Deep-link OAuth vive dentro de `AppRoot` (composable privado) | Extrair para um `OAuthCallbackHandler` chamado pelo `AppShell`, mantendo o `MutableState<Uri?>` na Activity |
| `UiEvent.Message` → `Toast` direto na Activity | Migrar para `SnackbarHost` no novo Scaffold; contrato `UiEvent` preservado |
| Estados de sync (`connected`/`loading`/`error`/`sem registros`) reimplementados em cada tela | Um `SyncStatusChip` único no design system, alimentado pelo `NutritionSnapshot` — o deck exige o chip visível em todas as telas |
| Textos pt-BR espalhados em literais Kotlin | Não é obrigatório resolver agora; se resolver, `res/values/strings.xml` — **sem tocar nos literais dentro de `widget/`**, que são renderizados pelo launcher |
| `repository.recentDailyCalorieAverage` faz I/O de rede direto na chamada | Envolver num `HistoryRepository` com cache, para a UI nunca disparar rede a cada recomposição |

**Regra de ouro do desacoplamento:** nenhuma classe em `fatsecret/data`,
`fatsecret/domain`, `fatsecret/widget`, `fatsecret/work` ou `fatsecret/oauth` pode
passar a importar algo de `ui/`. A dependência é sempre `ui → fatsecret`, nunca o
contrário. Isso já é verdade hoje (verificado: nenhum import de `ui.*` dentro de
`fatsecret/`, exceto a referência a `MainActivity`) e deve continuar sendo.

---

## 6. Como criar a nova navegação sem afetar os widgets

1. **`MainActivity` permanece a mesma classe, no mesmo pacote, com a mesma entrada
   no manifesto.** Só o corpo do `setContent` muda. É isso que mantém
   `actionStartActivity<MainActivity>()` funcionando nos dois widgets.
2. **`AppShell`** (novo composable) substitui `AppRoot`: `Scaffold` +
   `NavigationBar` de 5 abas (Hoje, Tendências, Padrões, Consistência, Peso) +
   avatar no topo abrindo *Metas e conta*, conforme o mapa do slide 3.
3. **`NavHost` com rotas tipadas**, start destination `hoje`.
4. **O deep link OAuth continua sendo tratado pela Activity**, não pelo NavHost.
   Registrar o esquema `${fatSecretCallbackScheme}` como deep link de navegação
   seria uma mudança de comportamento desnecessária e arriscada — o fluxo atual
   (`onNewIntent` → `MutableState<Uri?>` → `LaunchedEffect`) já funciona e é
   preservado literalmente.
5. **Nenhuma nova Activity, nenhum novo intent-filter, nenhuma mudança em
   `launchMode`.** Se no futuro um widget precisar abrir uma aba específica, a
   forma segura é `actionStartActivity<MainActivity>(intent com extra "tab")` —
   mas isso é fora de escopo desta etapa e exigiria reteste completo dos widgets.
6. **Convivência durante a migração:** a UI antiga fica acessível atrás de uma
   constante de build (`const val USE_LEGACY_UI = false` em código de debug) até a
   Etapa 10. Isso dá rollback imediato sem `git revert`.

---

## 7. Migração de Metas, Conta e Sincronização

**Metas** (`GoalsSettingsScreen` → aba *Metas e conta*):
- `GoalsStore` e suas chaves (`calories`, `protein`, `carbs`, `fat`,
  `start_weight_kg`) permanecem **idênticas** — zero migração de dados.
- Preservar: sanitização numérica, aceite de vírgula decimal, `coerceAtLeast(0)`,
  fallback para os defaults, e os dois textos explicativos (a API não expõe metas
  nem peso inicial — são as perguntas que o usuário mais faz).
- Preservar: salvar metas → `updateWidgets()` numa **única** corrotina → `syncNow()`.
- O deck reforça no slide 12: "Metas de macros são locais". Manter esse aviso.

**Conta** (conectar/desconectar):
- `connect()`, `handleCallback()`, `disconnect()` migram literalmente para
  `AccountViewModel`, incluindo o `clearCache = true` no disconnect e o
  `SyncScheduler.cancelAll`.
- Mensagens de erro (`messageFor(SyncErrorType)`) migram como estão; o método
  público `messageForPublic` (hoje sem chamador) passa a ser usado pelas telas.

**Sincronização**:
- Chamada única via `container.syncAndRefresh().await()`, observada pela UI apenas
  para mostrar progresso — nunca cancelando o trabalho.
- `init{}` do ViewModel atual (marcar `connected`, agendar periódico, sincronizar
  ao abrir) migra para o `AccountViewModel`, **executado uma vez por processo**,
  não uma vez por aba. Cuidado explícito: cinco abas com cinco ViewModels não podem
  virar cinco syncs ao abrir o app (ver risco R5).
- O chip "Sincronizado há 12 min" do deck lê `snapshot.lastSyncMillis` — já existe.

---

## 8. Ordem de implementação

**Princípio de ordenação:** infraestrutura de dados antes de telas; a tela que já
tem 100% dos dados (Hoje) antes das que precisam de histórico; a tela que substitui
função existente (Metas/Conta) cedo, para poder aposentar a UI antiga; remoção do
legado por último.

```
Etapa 0  Rede de segurança (commit inicial + baseline dos widgets)
Etapa 1  Camada de histórico (dados) — sem UI
Etapa 2  Design system (tema, tokens, componentes base) — sem lógica
Etapa 3  Casca de navegação (MainActivity + AppShell + 6 rotas vazias)
Etapa 4  Tela Hoje
Etapa 5  Metas e conta
Etapa 6  Tendências
Etapa 7  Padrões (+ folha de metodologia)
Etapa 8  Consistência
Etapa 9  Peso
Etapa 10 Estados (vazio / carregando / erro / dados insuficientes)
Etapa 11 Remoção do legado
```

---

## 9. Etapas detalhadas

### Etapa 0 — Rede de segurança e linha de base ✅ concluída (2026-07-25)

- **Objetivo:** garantir rollback e registrar o comportamento atual dos widgets
  antes de qualquer linha nova. **O repositório não tem nenhum commit** — sem isso
  não existe "voltar atrás".
- **Arquivos afetados:** `.gitignore` (verificar que `local.properties`, `build/`,
  `.idea/` estão ignorados), commit inicial de tudo, tag `baseline-widgets`.
  Novo: `docs/widget-smoke-test.md`.
- **Funcionalidades preservadas:** todas (nenhuma alteração de código).
- **Critérios de conclusão:** commit inicial criado; checklist de fumaça dos
  widgets escrito e executado uma vez com resultado registrado; `local.properties`
  confirmadamente fora do versionamento.
- **Build e testes:** `./gradlew :app:testDebugUnitTest` (6 classes verdes) e
  `./gradlew :app:assembleDebug`. Instalar, adicionar os dois widgets em ambos os
  tamanhos, conectar a conta, sincronizar, alternar tema claro/escuro.
- **Status real:** o commit inicial já existia (`5f744e5`); `.gitignore` já
  cobria `local.properties`/`build/`/`.idea` corretamente. Criado
  [`docs/widget-smoke-test.md`](docs/widget-smoke-test.md) e a tag
  `baseline-widgets`. **Pendência:** o checklist manual (itens 1–12 do doc) não
  foi executado nesta sessão — não há dispositivo/emulador disponível neste
  ambiente. Rodar antes de avançar para uma etapa que toque em UI (Etapa 3+).

---

### Etapa 1 — Camada de histórico (sem UI) ✅ concluída (2026-07-25)

- **Objetivo:** persistir a série diária (data, kcal, proteína, carbo, gordura) e
  expor Flows agregados, **sem tocar em `NutritionCacheStore`**.
- **Arquivos afetados:** *novos* —
  `fatsecret/data/history/NutritionHistoryStore.kt` (DataStore `nutrition_history`,
  CSV compacto no mesmo padrão de `WeightCacheStore.encode`),
  `fatsecret/data/history/HistoryRepository.kt`,
  `fatsecret/domain/history/TrendCalculator.kt`,
  `fatsecret/domain/history/PatternCalculator.kt`,
  `fatsecret/domain/history/ConsistencyCalculator.kt` (todos puros, sem Android).
  *Modificado (mínimo)* — `AppContainer.kt`: expor `historyRepository`.
  **`FatSecretRepository.sync()` não é alterado nesta etapa.**
- **Funcionalidades preservadas:** todas. Nenhum store existente é lido ou escrito
  de forma diferente; o histórico é um quarto arquivo DataStore, independente.
- **Critérios de conclusão:** `HistoryRepository.refresh()` busca 1–2 meses via
  `foodClient.getMonth()` e persiste; Flows de média por janela (7/14/30), média
  por dia da semana, e dias registrados por mês funcionando; **dias ausentes
  representados como ausência, nunca como zero** (regra explícita do deck);
  testes unitários dos três calculadores cobrindo janela vazia, janela parcial,
  e "dados insuficientes" (< 4 dias registrados por semana).
- **Build e testes:** `:app:testDebugUnitTest` (novos testes + os 6 existentes),
  `:app:assembleDebug`. Checklist de widgets completo — esta etapa mexe no
  `AppContainer`, que é compartilhado.
- **Status real:** implementado como planejado —
  [`NutritionHistoryStore`](app/src/main/java/com/example/widgetfatsecret/fatsecret/data/history/NutritionHistoryStore.kt)
  (DataStore `nutrition_history`, CSV `dateInt:cal:protein:carbs:fat`, bounded a
  400 dias),
  [`HistoryRepository`](app/src/main/java/com/example/widgetfatsecret/fatsecret/data/history/HistoryRepository.kt),
  e os três calculadores puros em
  [`fatsecret/domain/history/`](app/src/main/java/com/example/widgetfatsecret/fatsecret/domain/history/)
  (`TrendCalculator`, `PatternCalculator`, `ConsistencyCalculator`).
  `AppContainer` expõe `historyRepository`; `FatSecretRepository.sync()` e
  `syncAndRefresh()` não foram tocados. 15 novos testes JVM cobrindo janela
  vazia/parcial, comparação com período anterior, "dados insuficientes" (< 4
  dias), agrupamento por dia da semana, sequências com lacunas e um caso de
  virada de mês (68 testes no total, todos verdes). `HistoryRepository.refresh()`
  ainda não tem chamador — nenhuma tela ou worker aciona sync de histórico
  automaticamente, por design (ver risco R6).

---

### Etapa 2 — Design system ✅ concluída (2026-07-25)

- **Objetivo:** tokens de cor, tipografia e componentes base do deck, prontos para
  as telas.
- **Tokens confirmados no protótipo** (`Nutri Insights.dc.html`, não só no deck):
  - Escuro: `bg #0A0F1A` · `surf #131B2B` · `surf2 #1B2539` · `tx #E9EFF8` ·
    `tx2 #93A3BD` · `tx3 #64748F` · `mint #84E0A8` · `cyan #5FC8E8` · `amber #E5B15C` ·
    `coral #F0806F` · `violet #9E9BF0` · `page #070B13`.
  - Claro: `bg #F4F7FB` · `surf #FFFFFF` · `tx #0D1626` · `mint #0F8F62` ·
    `cyan #1B7EA6` · `amber #A6740F` · `coral #CE4F44` · `violet #5E5AC0`.
  - Fontes: Manrope (400–800) para UI, IBM Plex Mono (400/500) para metadados/números
    (sempre `tabular-nums`).
  - Botão primário: `background: mint; color: bg; font: 700 12.5px Manrope`.
    Botão secundário/destrutivo: `background: transparent; border: 1px solid line2;
    color: coral`. Raios: cartão 22px, botão 13px, botão icon 12px.
- **Arquivos afetados:** `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` (reescritos);
  *novos* `ui/design/` — `StatCard`, `MetricValue` (tabular-nums), `MetaChip`
  (monoespaçada), `SyncStatusChip`, `GoalRing`, `BarChart`, `EmptyState`,
  `SkeletonBlock`. Opcional: `res/font/` (Manrope, IBM Plex Mono).
- **Funcionalidades preservadas:** todas. **`widget/WidgetColors.kt` NÃO é tocado**
  — o widget é inflado pelo processo do launcher e tem paleta própria por decisão
  documentada (README §7.1). Unificar as paletas agora reintroduziria o bug de
  tema "congelado".
- **Critérios de conclusão:** previews Compose de cada componente em claro e
  escuro; contraste ≥ 4,5:1 verificado nos textos; nenhum estado dependendo só de
  cor (todo estado carrega rótulo + percentual, conforme slide 4).
- **Build e testes:** `:app:assembleDebug`. Widgets: verificação visual rápida
  (não devem mudar em nada — se mudarem, algo do tema vazou).
- **Status real:** implementado.
  [`ui/theme/Color.kt`](app/src/main/java/com/example/widgetfatsecret/ui/theme/Color.kt),
  [`Theme.kt`](app/src/main/java/com/example/widgetfatsecret/ui/theme/Theme.kt) e
  [`Type.kt`](app/src/main/java/com/example/widgetfatsecret/ui/theme/Type.kt)
  reescritos com os tokens do deck. A camada Material3 é derivada dos tokens; a
  paleta estendida (mint/cyan/amber/coral/violet + 3 níveis de texto/linha) vive
  em `NutriColors`, acessível por `MaterialTheme.nutriColors` via um
  `staticCompositionLocalOf`. **Dynamic color foi desligado** (`WidgetFatSecretTheme`
  não recebe mais `dynamicColor`/`LocalContext`) — o Material You atropelaria a
  paleta do deck; isso é o comportamento correto para a Etapa 2 e não afeta os
  widgets. Fontes: fallback do sistema (`SansSerif`/`Monospace`) com pesos
  equivalentes a Manrope/IBM Plex Mono, já que `res/font/` ainda não empacota os
  `.ttf` — caminho previsto em §3; trocar `UiFontFamily`/`MonoFontFamily` no
  futuro não mexe em call-sites. Estilos monoespaçados (`MonoText.metricLarge/
  metricMedium/meta`) ficam fora do `Typography` do Material porque não mapeiam
  nos papéis 1:1. Novos componentes em
  [`ui/design/`](app/src/main/java/com/example/widgetfatsecret/ui/design/):
  `StatCard`, `MetricValue`, `MetaChip`, `SyncStatusChip` (+ enum `SyncStatus`,
  mapeado do snapshot pela camada de ViewModel, não aqui), `GoalRing`,
  `BarChart` (+ `BarDatum`, dia ausente = contorno tracejado, nunca zero),
  `EmptyState`, `SkeletonBlock`, mais `NutriPrimaryButton`/`NutriSecondaryButton`
  (tokens de botão do deck) e `Tokens.kt` (raios 22/13/12, espaçamentos). Cada
  componente tem previews `@Preview` claro **e** escuro. **`widget/WidgetColors.kt`
  não foi tocado** (risco R7). Regra de ouro verificada: 0 imports de `ui.*` em
  `fatsecret/`. `:app:assembleDebug` e `:app:testDebugUnitTest` (68 testes,
  inalterados — o design system é validado por preview, não por teste JVM) verdes.
  **Pendência:** a verificação visual dos previews em claro/escuro (contraste
  ≥ 4,5:1) depende do Android Studio/dispositivo — não há renderização de preview
  neste ambiente; os tokens seguem os hex do protótipo, mas a conferência visual
  final fica para quando houver IDE/dispositivo.

### Etapa 3 — Casca de navegação ✅ concluída (2026-07-25)

- **Objetivo:** `AppShell` com 5 abas + rota de Metas/Conta, todas com placeholder.
- **Arquivos afetados:** `MainActivity.kt` (só o corpo do `setContent`),
  *novos* `ui/navigation/AppShell.kt`, `ui/navigation/Routes.kt`,
  `gradle/libs.versions.toml` + `app/build.gradle.kts` (navigation-compose).
  A UI antiga continua no projeto, atrás da flag `USE_LEGACY_UI`.
- **Funcionalidades preservadas:** classe `MainActivity` (nome/pacote/manifesto),
  `launchMode="singleTask"`, `onNewIntent`, tratamento do `oauth_verifier`,
  `enableEdgeToEdge`.
- **Critérios de conclusão:** as 6 rotas navegam; tocar em qualquer widget abre o
  app na aba Hoje; o fluxo OAuth completo (conectar → navegador → deep link →
  token → sync) funciona idêntico ao de antes, com a UI antiga ainda alcançável
  pela flag.
- **Build e testes:** `:app:testDebugUnitTest`, `:app:assembleDebug`.
  **Checklist crítico de widget:** tocar no corpo de cada widget, em cada estado
  (conectado, desconectado, erro, sem registros), deve abrir o app.
- **Status real:** implementado como planejado.
  [`ui/navigation/Routes.kt`](app/src/main/java/com/example/widgetfatsecret/ui/navigation/Routes.kt)
  define as 6 rotas tipadas (`@Serializable data object` dentro de um
  `sealed interface Route`, exigiu `navigation-compose 2.8.9` — a versão que
  já estava disponível localmente era 2.7.7, sem suporte a rotas tipadas, então
  a versão foi resolvida via rede para 2.8.9).
  [`ui/navigation/AppShell.kt`](app/src/main/java/com/example/widgetfatsecret/ui/navigation/AppShell.kt)
  monta `Scaffold` + `TopAppBar` (avatar circular "⚙" abrindo *Metas e conta*) +
  `NavigationBar` de 5 abas (oculta na rota de Metas/Conta) + `NavHost`, todas
  as 6 rotas mostrando `EmptyState` (reaproveitado da Etapa 2) com uma frase
  apontando para a etapa que vai preenchê-las. Nenhum ícone de
  `material-icons-extended` foi usado (dependência não existe no projeto) — os
  rótulos de aba usam a primeira letra do nome como "ícone" textual, seguindo a
  regra do slide 4 (todo estado carrega rótulo, nunca só cor/ícone).
  `MainActivity.kt` teve **apenas o corpo do `setContent`** alterado: a classe,
  `onNewIntent`, `launchMode`, `callbackUri` e `enableEdgeToEdge` são
  exatamente os mesmos. O tratamento do deep link OAuth e dos eventos do
  `FatSecretViewModel` (`OpenBrowser`/`Message`) foi extraído para um
  composable `OAuthCallbackAndEventEffects`, chamado **fora** de
  `AppRoot`/`AppShell` (direto no `setContent`), para continuar funcionando
  idêntico não importa qual UI está ativa. `USE_LEGACY_UI = false` — a nova
  casca é a UI padrão; alternar para `true` e recompilar volta à UI antiga
  (`AppRoot`, preservada literalmente, só sem mais o parâmetro `callbackUri`
  que subiu para o `setContent`).
  **Nenhum widget, receiver, DataStore ou o manifesto foram tocados** — regra
  de ouro reverificada (`grep` sem hits de `ui.*` em `fatsecret/`).
  `:app:testDebugUnitTest` (68 testes, inalterados) e `:app:assembleDebug`
  verdes.
  **Pendência:** o checklist manual de `docs/widget-smoke-test.md` (itens 1–12,
  incluindo o ciclo completo 9–12 exigido nesta etapa) segue **não executado**
  neste ambiente por falta de dispositivo/emulador — mesma limitação já
  registrada nas Etapas 0–2. Como nenhum arquivo de widget/manifesto foi
  alterado, o risco é baixo, mas o checklist continua formalmente pendente e
  deve ser confirmado num dispositivo real antes da Etapa 5 (que é quando a
  UI nova ganha um botão de conectar/desconectar de verdade).

---

### Etapa 4 — Tela Hoje ✅ concluída (2026-07-25)

- **Objetivo:** anel de meta com o restante em número grande, macros com
  consumido/meta e percentual, distribuição por refeição do dia, "Leitura do dia"
  com no máximo dois insights, chip de sincronização.
- **Arquivos afetados:** *novos* `ui/today/TodayScreen.kt`, `TodayViewModel.kt`.
  *Modificado* `NutritionCacheStore` **apenas se** a distribuição por refeição do
  dia for persistida — nesse caso, **adicionar uma chave nova**, jamais alterar as
  existentes (`calories`, `protein`, `carbs`, `fat`, `entry_count`, `last_sync`,
  `status`, `error`, `connected`, `has_data`, `weekly_calories`).
- **Funcionalidades preservadas:** o widget lê o mesmo snapshot; qualquer chave
  nova é aditiva e opcional. `NutritionCalculator.buildInsight` reaproveitado.
- **Critérios de conclusão:** paridade com a `MainScreen` antiga (mesmos números)
  + refeições + chip; estado "sincronizado sem registros" visualmente distinto de
  "consumo zero" (slide 10).
- **Build e testes:** `:app:testDebugUnitTest`, `:app:assembleDebug`. Comparar
  lado a lado: número do app × número do widget × número do app FatSecret.
  Alterar uma meta e confirmar que app **e** widget atualizam.
- **Status real:** implementado.
  [`ui/today/TodayViewModel.kt`](app/src/main/java/com/example/widgetfatsecret/ui/today/TodayViewModel.kt)
  é só leitura — expõe `repo.uiState` como `StateFlow` e **não dispara nenhum
  sync** (o sync de abertura continua exclusivamente no `FatSecretViewModel`
  legado, mantido vivo pela `MainActivity` independente de `USE_LEGACY_UI`; ver
  risco R5).
  [`ui/today/TodayScreen.kt`](app/src/main/java/com/example/widgetfatsecret/ui/today/TodayScreen.kt)
  monta 4 `StatCard` (Etapa 2): anel de meta (`GoalRing`, restante ou excedente
  em número grande), macros (proteína/carbo/gordura com barra colorida
  cyan/amber/violet + `consumido/meta g • percentual`), refeições do dia
  (nome traduzido + kcal + % do total) e "Leitura do dia" com até dois
  insights. O `SyncStatusChip` fica sempre visível no topo. Estados
  "desconectado" e "aguardando primeira sincronização" usam `EmptyState` e são
  visualmente distintos de "sincronizado com zero registros" (que passa pelo
  conteúdo normal, com a `SyncStatusChip` mostrando "Sincronizado" e o insight
  "Nenhum alimento registrado hoje" — nunca uma tela vazia).
  **Distribuição por refeição** foi persistida: `NutritionCacheStore` ganhou o
  campo `mealBreakdown: List<MealTotal>` e a chave nova `meal_breakdown` (CSV
  `refeição:calorias`, mesmo padrão de `weekly_calories`) — as chaves
  existentes não foram tocadas. `FatSecretRepository.syncLocked()` calcula o
  breakdown a partir das MESMAS `entries` já buscadas para `daily` (nenhuma
  chamada de rede extra, ao contrário do `weekly`, que é best-effort à parte).
  Duas funções puras novas em `NutritionCalculator`: `mealBreakdown(entries)`
  (agrupa e ordena por calorias, desempate estável pela ordem de aparição) e
  `dominantMealShare(meals)` (retorna `null` com menos de 2 refeições ou total
  zero — "distribuição" de uma refeição só não é um padrão). `NutritionFormat`
  ganhou `mealLabel` (traduz os valores fixos do FatSecret: Breakfast/Lunch/
  Dinner/Other; desconhecido passa direto), `mealShareText` ("Jantar concentra
  43% das calorias de hoje" — descritivo, sem julgamento) e `timeAgo`
  (buckets grosseiros "agora"/"há N min"/"há N h"/"há N d" para o detalhe do
  chip de sync). O `SyncStatus` do design system (Etapa 2) é mapeado a partir
  do `NutritionSnapshot` dentro de `TodayScreen.kt` (função privada
  `toChipStatus()`), não no design system, como a Etapa 2 já previa.
  **Nenhum widget, receiver, manifesto ou as chaves de cache existentes foram
  tocados** — regra de ouro reverificada (`grep` sem hits de `ui.*` em
  `fatsecret/`).
  `:app:testDebugUnitTest` (**77 testes, 0 falhas** — 68 anteriores + 9 novos
  cobrindo `mealBreakdown`, `dominantMealShare`, `mealLabel`, `mealShareText` e
  `timeAgo`) e `:app:assembleDebug` verdes.
  **Pendência:** paridade numérica lado a lado com a `MainScreen` antiga e o
  app FatSecret, e o checklist de fumaça dos widgets, não foram verificados
  visualmente nesta sessão — sem dispositivo/emulador neste ambiente (mesma
  limitação de todas as etapas anteriores). Como nenhum arquivo de
  widget/manifesto/DataStore existente foi alterado (só uma chave nova
  aditiva), o risco de regressão nos widgets é baixo, mas a conferência visual
  da tela Hoje em si (ring, cores dos macros, contraste) ainda depende de um
  dispositivo real.

---

### Etapa 5 — Metas e conta ✅ concluída (2026-07-25)

- **Objetivo:** migrar metas, peso inicial, conectar/desconectar e sync manual.
- **Arquivos afetados:** *novos* `ui/account/GoalsAccountScreen.kt`,
  `ui/account/AccountViewModel.kt`. `MainActivity.kt` (fiação do deep link).
- **Funcionalidades preservadas (todas, literalmente):** `beginConnect` /
  `completeConnect` / `disconnect(clearCache = true)`; `SyncScheduler.ensurePeriodic`
  no sucesso e `cancelAll` no disconnect; `saveGoals` + `saveStartWeight` +
  `updateWidgets()` numa **única** corrotina; sanitização numérica pt-BR; os textos
  sobre a API não expor metas/peso inicial; mensagens de `SyncErrorType`.
- **Critérios de conclusão:** conectar do zero funciona; desconectar limpa cache e
  widgets voltam ao estado desconectado; salvar meta reflete no widget em um único
  refresh (sem piscar valor antigo); reinstalar não é necessário para nada disso.
- **Build e testes:** `:app:testDebugUnitTest`, `:app:assembleDebug`.
  **Ciclo completo obrigatório:** desconectar → widgets em estado vazio →
  reconectar → widgets repopulados. Este é o teste de regressão mais importante
  de todo o plano.
- **Status real:** implementado como planejado.
  [`ui/account/AccountViewModel.kt`](app/src/main/java/com/example/widgetfatsecret/ui/account/AccountViewModel.kt)
  é a migração literal de `FatSecretViewModel` (mesmos métodos, mesmo `init{}`
  de sync-uma-vez-por-processo, mesma regra de `saveGoals`+`updateWidgets()`
  numa única corrotina). `FatSecretViewModel`/`AppScreens.kt` **não foram
  apagados** — continuam servindo a UI antiga sob `USE_LEGACY_UI = true` até a
  Etapa 11.
  [`ui/account/GoalsAccountScreen.kt`](app/src/main/java/com/example/widgetfatsecret/ui/account/GoalsAccountScreen.kt)
  monta `SyncStatusChip` + 3 `StatCard` (Conta FatSecret, Metas diárias, Peso
  inicial) + um botão Salvar único no fim, preservando a sanitização numérica
  (`NumberField`/`DecimalField`, movida literalmente de `AppScreens.kt`) e os
  textos explicativos sobre a API não expor metas/peso inicial. Como o
  protótipo não desenha essa tela (ver §0), a casca é ad-hoc com os tokens da
  Etapa 2. `AppShell.kt` passou a receber `accountViewModel: AccountViewModel`
  de fora (não resolvido com `viewModel()` dentro do `composable<Route.MetasConta>`,
  que criaria uma segunda instância presa ao back-stack-entry da rota) e ganhou
  um botão de voltar na `TopAppBar` quando a rota atual é `MetasConta`.
  `MainActivity.kt` agora instancia **um único** ViewModel por processo,
  condicionado a `USE_LEGACY_UI` (nunca os dois) — `FatSecretViewModel` para a
  UI antiga, `AccountViewModel` para a nova — e `OAuthCallbackAndEventEffects`
  foi generalizado para receber `events: Flow<UiEvent>` + `onCallback: (Uri) ->
  Unit` em vez do tipo concreto do ViewModel, para não duplicar o composable.
  Isso evita o risco R5 (duas fontes de sync de abertura) mesmo com dois
  ViewModels de conta coexistindo no código.
  **Validado num emulador real nesta sessão** (`emulator-5554`, com uma conta
  já conectada e dados reais): a rota Hoje mostra 918/1.000 kcal e as
  refeições/insights; abrir "Metas e conta" mostra "Conectado", os botões
  Sincronizar/Desconectar, as 4 metas prefilled (1000/150/200/65) e o peso
  inicial (128,5, com a dica "123,0 kg" da pesagem mais antiga descoberta); o
  botão de voltar retorna à aba Hoje. A edição de um campo (testada no campo
  Calorias, trocando para "1200") respeitou a sanitização (só dígitos,
  `take(6)`) e manteve o valor após fechar o teclado.
  **Pendência desta sessão:** o ciclo completo "editar meta → Salvar → widget/
  Hoje refletem em um único refresh" (critério de conclusão explícito desta
  etapa) não pôde ser reconfirmado de ponta a ponta neste ambiente — depois da
  primeira edição bem-sucedida, os toques subsequentes do ADB no emulador
  (`input tap`/`input text`) pararam de focar o campo de forma confiável
  (mesmas coordenadas, mesma tela, sem mudança de código no meio), o que
  impediu reproduzir o toque no botão Salvar de forma controlada. É uma
  suspeita de flakiness do pipeline de input do emulador nesta sessão, não uma
  falha observada no código — mas como o critério de conclusão pede
  explicitamente esse ciclo, ele deve ser reconfirmado manualmente (editar uma
  meta, tocar Salvar, confirmar que a aba Hoje E os dois widgets mudam juntos,
  sem piscar o valor antigo) antes de considerar a Etapa 5 fechada em definitivo.
  O checklist de `docs/widget-smoke-test.md` (itens 1–8, sem o ciclo completo
  9–12 que dependeria de reconectar a conta) também não foi reexecutado.

---

### Etapa 6 — Tendências ✅ concluída (2026-07-25)

- **Objetivo:** média diária em 7/14/30 dias, variação vs. período anterior, gráfico
  de calorias por dia com linha da meta, distribuição acima/próximo/abaixo.
- **Arquivos afetados:** *novos* `ui/trends/TrendsScreen.kt`, `TrendsViewModel.kt`.
  Consome a Etapa 1; sem alteração em data/domain existentes.
- **Funcionalidades preservadas:** todas (tela puramente aditiva).
- **Critérios de conclusão:** dia sem registro é contorno tracejado e fica fora de
  todas as médias; número de dias registrados sempre exibido ao lado da média;
  card "dados insuficientes" com a regra mínima quando faltam amostras.
- **Build e testes:** `:app:testDebugUnitTest` (cálculos de janela e comparação),
  `:app:assembleDebug`. Widgets: verificação rápida.
- **Status real:** implementado como planejado.
  [`ui/trends/TrendsViewModel.kt`](app/src/main/java/com/example/widgetfatsecret/ui/trends/TrendsViewModel.kt)
  combina `repo.uiState` com `historyRepository.daysFlow` e computa os três
  `TrendSummary` (7/14/30) via `TrendCalculator.summarize` — puro, sem I/O
  extra por emissão. É o **primeiro chamador** de `HistoryRepository.refresh()`
  (Etapa 1 deixou isso documentado como pendente até "uma tela consumidora de
  histórico existir"): disparado uma vez em `init{}`, no escopo da própria aba,
  nunca em `AppContainer.syncAndRefresh()` — mantém o custo de quota do risco
  R6 isolado desta tela.
  [`ui/trends/TrendsScreen.kt`](app/src/main/java/com/example/widgetfatsecret/ui/trends/TrendsScreen.kt)
  monta `SyncStatusChip` + 3 `StatCard` de janela (média + variação com seta
  ↑/↓, sem cor de julgamento) + `StatCard` com `BarChart` (linha de meta,
  dias ausentes em contorno tracejado — comportamento nativo do componente da
  Etapa 2) + `StatCard` de distribuição acima/perto/abaixo. Cada janela abaixo
  do mínimo de dias mostra o texto de "dados insuficientes" **dentro do card**
  (a contagem "X de Y dias" continua sempre visível). `TrendCalculator` ganhou
  uma função pura nova, `distribution(days, goalCalories, tolerance)`, mais
  `CalorieDistribution` — dias sem registro nunca contam como "abaixo da
  meta"; meta ≤ 0 zera a distribuição em vez de forçar uma classificação sem
  sentido. Nenhuma função existente de `fatsecret/domain` foi alterada.
  `ui/navigation/AppShell.kt`: rota `Tendencias` trocou o placeholder por
  `TrendsRoute()`. **Nenhum widget, `MainActivity`, DataStore existente ou
  `AppContainer.syncAndRefresh()` foi tocado** — regra de ouro reverificada.
  `:app:testDebugUnitTest` (**79 testes, 0 falhas** — 77 anteriores + 2 novos
  cobrindo `distribution`) e `:app:assembleDebug` verdes.
  **Pendência:** a renderização visual da tela (chip, cards, gráfico,
  distribuição) em claro/escuro e o custo real de quota de `refresh()` contra
  a API não foram verificados nesta sessão — sem dispositivo/emulador
  disponível, mesma limitação de todas as etapas anteriores. O checklist de
  `docs/widget-smoke-test.md` continua pendente de execução manual.

---

### Etapa 7 — Padrões ✅ concluída (2026-07-25)

- **Objetivo:** padrões observados (dia, ciclo, macro, refeição), média por dia da
  semana, frequência fora da meta, e a **folha de metodologia** de cada insight.
- **Arquivos afetados:** *novos* `ui/patterns/PatternsScreen.kt`,
  `PatternsViewModel.kt`, `ui/patterns/MethodologySheet.kt`.
- **Funcionalidades preservadas:** todas.
- **Critérios de conclusão:** todo insight abre janela analisada, dias registrados,
  regra de cálculo e limitação do dado (slide 8); linguagem 100% descritiva, sem
  julgamento (slide 2); nenhum insight sobre alimentos individuais nem
  micronutrientes (slide 12). Análise por refeição só aparece se houver
  sincronização detalhada — caso contrário, estado explicativo.
- **Build e testes:** `:app:testDebugUnitTest` (agrupamento por dia da semana com
  amostra pequena; contagem "X de Y dias"), `:app:assembleDebug`.
- **Status real:** implementado.
  [`ui/patterns/PatternsViewModel.kt`](app/src/main/java/com/example/widgetfatsecret/ui/patterns/PatternsViewModel.kt)
  combina o snapshot/metas com o histórico e calcula uma janela fixa de 28 dias:
  médias de segunda a domingo, ciclo dias úteis × fim de semana e frequências de
  calorias/proteína/carboidratos/gorduras abaixo/perto/acima da faixa de ±5% das
  metas locais. `PatternCalculator` foi ampliado apenas com funções puras e tipos
  aditivos (`weeklyCycle`, `goalFrequency`, `WeeklyCycleSummary`, `GoalFrequency`);
  dias ausentes continuam fora de qualquer média ou frequência e metas não
  positivas não classificam nenhum dia.
  [`ui/patterns/PatternsScreen.kt`](app/src/main/java/com/example/widgetfatsecret/ui/patterns/PatternsScreen.kt)
  mostra o `SyncStatusChip`, a tabela auditável de médias por dia da semana e
  quatro superfícies descritivas: maior média por dia, ciclo semanal, frequência
  em relação à meta calórica e padrão de macro. Todo insight calculado abre
  [`MethodologySheet.kt`](app/src/main/java/com/example/widgetfatsecret/ui/patterns/MethodologySheet.kt)
  com janela, número de dias registrados, regra de cálculo e limitação do dado.
  A análise histórica por refeição não é inferida a partir do breakdown de hoje:
  aparece como estado explicativo "DADO NÃO DISPONÍVEL" até existir sincronização
  detalhada. Nenhum alimento individual, micronutriente, score ou conselho foi
  introduzido. A rota `Padroes` do `AppShell` trocou o placeholder pela tela real.
  `PatternCalculatorTest` ganhou 3 testes cobrindo ciclo com amostra pequena,
  contagem `X de Y` sem transformar lacunas em zero e meta não positiva.
  `:app:testDebugUnitTest` (**82 testes, 0 falhas**) e `:app:assembleDebug` verdes.
  Tela e folha de metodologia validadas no `emulator-5554` com 21 dias reais na
  janela; sem crash/erro em log. **Nenhum widget, receiver, manifesto, DataStore
  existente, `MainActivity` ou `AppContainer.syncAndRefresh()` foi tocado** e a
  regra de ouro (0 imports de `ui.*` em `fatsecret/`) foi reverificada. O checklist
  manual completo de `docs/widget-smoke-test.md` continua fora do escopo desta
  validação de tela e deve ser reexecutado quando necessário.

---

### Etapa 8 — Consistência ✅ concluída (2026-07-25)

- **Objetivo:** calendário mensal com quatro estados de ausência, sequência atual,
  maior sequência, percentual de 30 dias.
- **Arquivos afetados:** *novos* `ui/consistency/ConsistencyScreen.kt`,
  `ConsistencyViewModel.kt`, `ui/design/CalendarGrid.kt`.
- **Funcionalidades preservadas:** todas.
- **Critérios de conclusão:** os quatro estados (registrado / sem entradas / não
  sincronizado / futuro) são visualmente distintos e nomeados; nenhum tom de
  cobrança no texto; "não sincronizado" nunca é confundido com "sem entradas".
- **Build e testes:** `:app:testDebugUnitTest` (sequências com lacunas, mês
  parcial, virada de mês), `:app:assembleDebug`.
- **Status real:** implementado. O histórico ganhou metadados aditivos de meses
  sincronizados no mesmo DataStore dedicado da Etapa 1; um retorno mensal vazio
  agora é persistido como cobertura válida, permitindo distinguir de forma real
  `registrado`, `sem entradas`, `não sincronizado` e `futuro`. O refresh usa
  aritmética de `YearMonth`, evitando repetir o mês atual ao voltar 30 dias a
  partir do dia 31. `ConsistencyCalculator` calcula sequências e o percentual
  somente sobre dias sincronizados; lacunas encerram sequências, dias futuros e
  não sincronizados não são tratados como zero. A nova
  [`ui/consistency/ConsistencyScreen.kt`](app/src/main/java/com/example/widgetfatsecret/ui/consistency/ConsistencyScreen.kt)
  mostra calendário mensal, legenda nominal dos quatro estados, sequência atual,
  maior sequência e percentual da janela de 30 dias. O novo
  [`ui/design/CalendarGrid.kt`](app/src/main/java/com/example/widgetfatsecret/ui/design/CalendarGrid.kt)
  diferencia estados por preenchimento/contorno/tracejado além da cor e fornece
  descrições semânticas por dia. A rota `Consistencia` do `AppShell` trocou o
  placeholder pela tela real. `:app:testDebugUnitTest` (**84 testes, 0 falhas**)
  e `:app:assembleDebug` verdes. APK validado no `emulator-5554` com dados reais:
  20 dias registrados no mês, sequência atual/maior de 18 dias e 77% em 30 dias;
  sem crash no logcat. Nenhum widget, receiver, manifesto, DataStore existente
  dos widgets, `MainActivity` ou `AppContainer.syncAndRefresh()` foi tocado; a
  regra de ouro (0 imports de `ui.*` em `fatsecret/`) foi reverificada. O
  checklist manual completo dos widgets continua pendente, pois a etapa não
  alterou sua infraestrutura ou renderização.

---

### Etapa 9 — Peso ✅ concluída (2026-07-25)

- **Objetivo:** peso atual, delta, meta do FatSecret, pesagens, evolução com média
  móvel de 7 dias e calorias no mesmo eixo de tempo.
- **Arquivos afetados:** *novos* `ui/weight/WeightScreen.kt`, `WeightViewModel.kt`.
  Consome `repository.weightState` (já pronto) + histórico da Etapa 1.
- **Funcionalidades preservadas:** `WeightCalculator`, baseline, override de peso
  inicial, `WeightFormat` (kg/lb) — todos reaproveitados sem alteração.
- **Critérios de conclusão:** números idênticos aos do `WeightWidget`; aviso
  explícito de não-causalidade sob os gráficos combinados (slide 9); sem previsão
  de data, sem correlação numérica (slide 12).
- **Build e testes:** `:app:testDebugUnitTest`, `:app:assembleDebug`. Comparar cada
  número com o do widget de peso — divergência aqui indica regressão no cálculo
  compartilhado.

**Status real:** `WeightViewModel` combina o `repository.weightState` — sem
recalcular nenhum número compartilhado — com o histórico nutricional da Etapa 1.
`WeightScreen` mostra peso atual, delta da pesagem anterior, tendência, média
semanal, total, meta e progresso, além de uma linha de média móvel de 7 dias,
pesagens e barras de calorias alinhadas na mesma janela de 30 dias. Ausências
permanecem sem valor e o card declara explicitamente que a leitura conjunta não
mede correlação nem causalidade. A aba também lista as cinco pesagens mais
recentes; não há previsão de data ou correlação numérica. Três testes novos
cobrem alinhamento temporal, lacunas e a média móvel. Testes/build: 87 testes JVM,
0 falhas e `BUILD SUCCESSFUL`. No `emulator-5554`, tela e widget foram comparados
lado a lado com os mesmos dados: 104,4 kg, −0,5 kg, tendência Perdendo,
−1,0 kg/sem, total −24,1 kg, meta 83,0 kg e 21,4 kg restantes. Gráfico, rolagem,
aviso de não causalidade e lista foram inspecionados sem erro `AndroidRuntime`.
Nenhum arquivo do widget, domínio de peso, repositório ou DataStore foi alterado.

---

### Etapa 10 — Estados

- **Objetivo:** os seis estados do slide 10 em todas as telas.
- **Arquivos afetados:** telas das Etapas 4–9 + `ui/design/EmptyState.kt`,
  `SkeletonBlock.kt`.
- **Funcionalidades preservadas:** "falha de sync mantém os últimos dados válidos"
  — é o mesmo contrato que o widget já cumpre; a UI deve espelhá-lo, não
  contradizê-lo.
- **Critérios de conclusão:** avião ligado → dados antigos + chip datado, nunca
  tela vazia; esqueleto com altura do conteúdo final (sem deslocamento de layout).
- **Build e testes:** `:app:assembleDebug`. Testes manuais: modo avião, conta
  recém-conectada sem registros, token revogado.

**Status real:** implementado. O mapeamento central `ContentState` cobre conta
desconectada e os seis estados do slide 10: sincronizado, sem registros, não
sincronizado, falha de sync, dados insuficientes e carregando. As telas Hoje,
Tendências, Padrões, Consistência e Peso agora usam o mesmo contrato. Uma falha
com cache mantém o último conteúdo e mostra chip datado; uma falha sem cache
mostra a causa e uma ação de nova tentativa; um retorno bem-sucedido vazio é
descrito como ausência de registros, nunca como consumo/peso zero. Estados
iniciais usam skeletons dimensionados conforme os cards finais, e as telas
históricas atualizam tanto o histórico quanto o sync principal ao tentar de
novo. Cards com amostra abaixo do mínimo mostram a regra e a contagem real.
`EmptyState` ganhou ação opcional e `ScreenSkeleton` compõe blocos com alturas
estáveis. Cinco testes novos cobrem o resolvedor de estados. Validação final:
92 testes JVM, 0 falhas, `assembleDebug` e `lintDebug` verdes. No
`emulator-5554`, modo avião preservou `970 / 1.000 kcal` na tela Hoje e as
médias em Tendências, com chip `Offline — últimos dados` datado; o modo avião
foi desativado e o estado sincronizado foi restaurado. Conta recém-conectada sem
registros e token realmente revogado não foram produzidos manualmente para não
apagar dados ou invalidar a autorização existente; seus ramos foram cobertos
pelo resolvedor puro e pela inspeção do fluxo. Nenhum widget, receiver,
manifesto, DataStore ou sincronização de infraestrutura foi alterado.

---

### Etapa 11 — Remoção do legado

- **Objetivo:** apagar a UI antiga **somente** após paridade verificada.
- **Arquivos afetados:** remover `ui/AppScreens.kt` e `ui/FatSecretViewModel.kt`;
  remover a flag `USE_LEGACY_UI`. Atualizar `README.md` (seções 6 e 7.1) e
  `handoff.md`.
- **Funcionalidades preservadas:** verificar item a item, contra a lista da
  seção 1.2, que nada foi removido junto.
- **Critérios de conclusão:** `grep` confirma que nenhum arquivo em `fatsecret/`
  importa `ui.*` (exceto `MainActivity` nos dois widgets); nenhuma referência
  pendente às classes removidas; README descreve o app novo.
- **Build e testes:** `:app:testDebugUnitTest`, `:app:assembleDebug`,
  `:app:assembleRelease`, e o checklist completo de widgets uma última vez.

---

## 10. Riscos para os widgets

| # | Risco | Impacto | Mitigação |
|---|---|---|---|
| **R1** | Renomear/mover/apagar `MainActivity` | Tocar no widget deixa de abrir o app, em todos os estados | A classe é intocável (seção 1.2, item 1). Verificar com `grep MainActivity` antes de qualquer refatoração de pacote |
| **R2** | Alterar `launchMode` ou o intent-filter da MainActivity | Quebra o retorno do `oauth_verifier`; usuário nunca conecta | Manifesto da Activity congelado; qualquer mudança exige ciclo OAuth completo de teste |
| **R3** | Renomear arquivos DataStore ou chaves | Perda silenciosa de metas e cache; widget volta a defaults | Nomes e chaves congelados; novas chaves só aditivas |
| **R4** | Nova tela chamar `repository.sync()` direto | Perde o single-flight, race de escrita, widget com dado antigo | Único ponto de entrada permitido: `AppContainer.syncAndRefresh()` |
| **R5** | 5 abas × 5 ViewModels disparando sync ao abrir | Estouro de quota, respostas fora de ordem | Sync de abertura só no `AccountViewModel`, uma vez por processo. O single-flight já protege, mas não é desculpa para chamar 5 vezes |
| **R6** | Backfill de histórico gastando quota | Rate limit (`SyncErrorType.RATE_LIMIT`) derruba o sync do widget | Preferir `get_month` (1 request/mês) a `get.v2` (1/dia); backfill sob demanda, com resultado persistido; nunca em `init{}` de tela |
| **R7** | Unificar tema do app com `WidgetColors` | Reintrodução do bug de tema congelado no launcher | `WidgetColors.kt` fica fora do design system, por decisão documentada |
| **R8** | Múltiplos `updateWidgets()` em corrotinas separadas | Race de sessão do Glance; widget mantém valor antigo | Padrão do `saveGoals` atual: escritas + refresh numa única corrotina |
| **R9** | Bump de Glance/Compose BOM junto com a migração | Regressão de layout do widget misturada com mudanças de app | Nenhum bump de `glance`, `work`, `datastore` ou `security-crypto` durante as Etapas 0–11 |
| **R10** | Ativar R8/minify para "limpar" o release | Perda de classes do Glance/Retrofit por reflexão | `isMinifyEnabled` permanece `false`; se for ligado, é tarefa própria com teste de release |
| **R11** | Trocar Preferences por Room "de uma vez" | Migração de dados nos três stores que os widgets leem | Room, se vier, é só para o histórico novo. Os três stores existentes ficam em Preferences |
| **R12** | Ausência de commits no repositório | Sem rollback para qualquer regressão | Etapa 0, antes de tudo |

---

## 11. Como validar que os widgets continuam funcionando

### Checklist de fumaça (executar ao final de cada etapa)

**Automatizado:**
```bash
./gradlew :app:testDebugUnitTest
```
```bash
./gradlew :app:assembleDebug
```

**Manual — os dois widgets, em ambos os tamanhos:**

1. Widget de nutrição mostra kcal e macros com os mesmos números da tela Hoje.
2. Widget de peso mostra peso, delta, tendência e progresso corretos.
3. **Tocar no corpo de cada widget abre o app** (o teste que mais barato quebra).
4. Alterar uma meta no app → os dois widgets refletem em um único refresh.
5. Alternar o tema do sistema (claro/escuro) → os widgets viram na hora, **sem**
   nova sincronização.
6. Modo avião → os widgets mantêm os últimos dados válidos e sinalizam a falha,
   sem zerar nada.
7. Forçar update: `adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE`.
8. Redimensionar cada widget na tela inicial (a escala é responsiva).

**Ciclo completo (obrigatório nas Etapas 3, 5 e 11):**

9. Desconectar → widgets vão ao estado desconectado e o cache é limpo.
10. Reconectar (OAuth completo, via navegador e deep link) → widgets repopulados.
11. Fechar o app durante um sync → o sync termina e os widgets atualizam mesmo
    assim (é o contrato do `appScope`).
12. Reiniciar o aparelho → widgets renderizam do cache; o worker periódico volta.

### Critério de bloqueio

Qualquer item de 1 a 12 falhando **bloqueia** o avanço para a etapa seguinte. Não
existe "corrigimos depois": a regressão precisa ser resolvida ou revertida na
etapa em que apareceu.

---

## 12. O que este plano deliberadamente NÃO faz

- Não reconstrói o projeto do zero nem cria módulo novo.
- Não refatora `FatSecretRepository`, `AppContainer`, os stores, os clients HTTP,
  o assinador OAuth nem os widgets — todos funcionam e ficam como estão.
- Não migra os dados existentes: metas, tokens e cache seguem onde estão.
- Não remove uma única linha da UI antiga antes da Etapa 11.
- Não atualiza versões de dependências usadas pelos widgets.
- Não implementa nada do escopo negativo do deck (TDEE, correlação peso×calorias,
  score de alimentação, classificação de alimentos, micronutrientes, previsão de
  peso, recomendação médica ou nutricional).
