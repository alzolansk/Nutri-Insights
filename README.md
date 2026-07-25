# WidgetFatSecret

App Android pessoal que lê o seu diário alimentar do **FatSecret** (via OAuth 1.0a
de três etapas) e transforma os registros em dois **widgets de tela inicial**
(Jetpack Glance) e no app **Nutri Insights** (Jetpack Compose). O app reúne Hoje,
Tendências, Padrões, Consistência e Peso, além de Metas e conta.

O que o widget mostra: as calorias consumidas em destaque, a meta do dia, uma
barra de progresso e o restante/excedido. No tamanho largo, ao lado disso,
proteínas/carboidratos/gorduras (consumido × meta) com barras finas. Mais os
estados de carregando / erro / desconectado / sem registros. A análise completa
fica no app — ver [seção 7.1](#71-ícone-tema-e-interação-dos-widgets).

> Os insights e as metas são **apenas cálculos** sobre os valores que você
> registra e as metas que você define. Não são recomendações nutricionais nem
> avaliações médicas.

---

## 1. Onde inserir as 3 credenciais

As credenciais **nunca** ficam no código Kotlin. Elas são lidas de
`local.properties` (arquivo já ignorado pelo Git) e expostas ao app via
`BuildConfig`.

Abra **`local.properties`** na raiz do projeto e preencha:

```properties
FATSECRET_CONSUMER_KEY=coloque_aqui_o_consumer_key
FATSECRET_CONSUMER_SECRET=coloque_aqui_o_consumer_secret
FATSECRET_CALLBACK_URL=widgetfatsecret://oauth-callback
```

- **FATSECRET_CONSUMER_KEY** e **FATSECRET_CONSUMER_SECRET**: em
  <https://platform.fatsecret.com/> → seção **“REST API OAuth 1.0 Credentials”**
  → botões *Show Consumer Key* e *Show Consumer Secret*.
  (Use as credenciais **OAuth 1.0**, não as de OAuth 2.0 — só o OAuth 1.0 de três
  etapas dá acesso ao diário privado da sua conta.)
- **FATSECRET_CALLBACK_URL**: deep link que o app registra para receber o
  `oauth_verifier`. Pode manter o valor padrão acima. **Não** use `oob` nem uma
  URL `http(s)` — precisa ser um deep link (`scheme://host`).

Depois de preencher, **sincronize o Gradle** (o Android Studio recompila o
`BuildConfig`).

> ⚠️ Se você reaproveitar credenciais que já apareceram em qualquer print ou
> chat, **gere novas** em *Reset Consumer Secret* — um segredo exposto deve ser
> considerado comprometido.

---

## 2. Como registrar o callback / deep link

Nada a fazer manualmente no `AndroidManifest.xml`: o `intent-filter` do deep link
é gerado a partir de `FATSECRET_CALLBACK_URL` via `manifestPlaceholders`
(`scheme` e `host` são extraídos automaticamente em `app/build.gradle.kts`).

O que você precisa garantir **do lado do FatSecret**:

1. No painel do FatSecret, cadastre **Whitelisted IP Addresses** (obrigatório
   para a API funcionar — pode levar até 24h para valer).
2. O `oauth_callback` enviado no fluxo é exatamente o seu `FATSECRET_CALLBACK_URL`.
   O FatSecret redireciona o navegador para ele com `?oauth_verifier=...`, e o
   Android entrega esse deep link para a `MainActivity` (`launchMode="singleTask"`,
   tratado em `onNewIntent`).

Se você trocar o scheme/host do callback, basta mudar `FATSECRET_CALLBACK_URL` —
manifesto e código continuam sincronizados.

---

## 3. Como conectar a conta pela primeira vez

1. Preencha as credenciais (passo 1) e instale o app.
2. Abra o app, entre em **Metas e conta** pelo avatar e toque em
   **“Conectar ao FatSecret”**.
3. O navegador abre a página de autorização do FatSecret. Faça login na sua conta
   e **autorize** o acesso.
4. O FatSecret redireciona de volta para o app (deep link). O app troca o
   `request token` + `oauth_verifier` pelo **access token**, guarda-o
   criptografado (EncryptedSharedPreferences / Android Keystore) e faz a primeira
   sincronização.
5. Adicione o widget à tela inicial (pressione e segure a tela → *Widgets* →
   *WidgetFatSecret*). Ele existe em dois tamanhos (compacto e médio).

Para **desconectar**: em **Metas e conta**, toque em *Desconectar*. A ação remove
os tokens, cancela o WorkManager e limpa o cache (decisão documentada: o cache é
apagado para o widget nunca mostrar dados de uma conta desconectada).

---

## 4. Metas nutricionais

### A API do FatSecret **não** expõe as metas da conta

Isto foi verificado na documentação oficial, endpoint por endpoint — não é
suposição:

| Endpoint | O que devolve | Traz meta? |
|---|---|---|
| `food_entries.get.v2` | valores **consumidos** por registro (`calories`, `protein`, `carbohydrate`, `fat`, micronutrientes) | ❌ |
| `food_entries.get_month.v2` | totais **consumidos** por dia do mês | ❌ |
| `profile.get` | `weight_measure`, `height_measure`, `height_cm`, `last_weight_kg`, `last_weight_date_int`, `goal_weight_kg` | ❌ (só meta de **peso**, nada de kcal/macros) |

Ou seja: a plataforma expõe **o que foi consumido** e, no perfil, apenas a meta
de **peso**. As metas de calorias, proteína, carboidratos e gorduras que você
configura no app/site do FatSecret **não têm endpoint de leitura** na Platform
API — nem na superfície OAuth 1.0a (`rest/server.api`) nem na REST OAuth 2.0.

### Alternativa adotada

As metas são **configuradas dentro deste app** e salvas **localmente**
(`DataStore`, `GoalsStore.kt`). Abra **Metas e conta** pelo avatar: calorias,
proteína, carboidratos e gorduras. Valores iniciais (totalmente editáveis):
**2000 kcal / 150 g proteína / 200 g carbo / 65 g gordura**. O widget atualiza
imediatamente após salvar.

### O **peso inicial** tem a mesma limitação

O “Peso Inicial” que o app do FatSecret mostra (e do qual ele calcula o “perdeu
até agora”) **não é a pesagem mais antiga do diário** e **não vem em nenhum
endpoint** — `profile.get` só traz a **última** pesagem e a meta. Confirmado
contra a API ao vivo: caminhando mês a mês por `weights.get_month.v2` até o
histórico acabar, a pesagem mais antiga que a plataforma devolve é mais recente
(e mais leve) que o peso inicial exibido no app.

Por isso **Metas e conta** também tem um campo **Peso inicial (kg)**.
Preenchido, ele ancora o **total perdido** e o **progresso da meta** no widget de
peso, fazendo os números baterem com o app do FatSecret. Em branco, o app usa a
pesagem mais antiga que encontrou no diário (mostrada como dica no próprio
campo). A **média semanal** e a **tendência** nunca usam esse valor: elas medem
o ritmo dos últimos 30 dias de propósito.

Consequência a ter em mente: se você mudar a meta no FatSecret, ela **não**
sincroniza — é preciso reajustar aqui. Não há como automatizar isso enquanto a
API não publicar o dado.

---

## 5. Sincronização

Ocorre: ao concluir o login, ao abrir o app, ao alterar uma meta e
periodicamente (**WorkManager**, a cada 30 min, só com rede, com *backoff*
exponencial). O WorkManager não garante horário exato — o
widget sempre mostra o horário do **último dado válido**. Em falha de rede o
cache **não** é sobrescrito por zeros; o widget mantém os últimos dados e sinaliza
discretamente a falha.

---

## 6. Arquitetura

- **Kotlin**, **Jetpack Compose** + Navigation Compose (cinco abas e a rota
  **Metas e conta**) e **Jetpack Glance 1.1** (dois widgets).
- **Retrofit + OkHttp** para HTTP; **kotlinx.serialization** para JSON.
- **DataStore** para metas, caches dos widgets e histórico nutricional;
  **EncryptedSharedPreferences** para tokens.
- **WorkManager** (sincronização); **ViewModels + StateFlow** por superfície de
  leitura. `AccountViewModel` é o único dono do sync ao abrir, conexão,
  desconexão e metas.
- Camada de assinatura **OAuth 1.0 isolada e testável**
  (`fatsecret/oauth/OAuth1Signer.kt`).

`MainActivity` hospeda somente o `AppShell` do Nutri Insights e continua tratando
o retorno OAuth. As cinco abas leem os mesmos repositórios e caches que alimentam
os widgets; nenhuma tela chama a sincronização do repositório diretamente. O
ponto único continua sendo `AppContainer.syncAndRefresh()`, que busca, persiste e
atualiza os dois widgets na ordem definida.

O histórico usado por Tendências, Padrões, Consistência e Peso vive num DataStore
dedicado. Dias ausentes permanecem ausentes — nunca são convertidos em consumo
zero — e as análises exibem janela e amostra. A interface segue o princípio
“padrão mensurável, nunca julgamento”.

Endpoints usados (método-based `rest/server.api`, compatível com OAuth 1.0):

- Diário: `method=food_entries.get.v2&format=json&date=<dias desde 1970>`
- Mensal (insights): `method=food_entries.get_month.v2`

O parâmetro `date` é o **número inteiro de dias desde 1970-01-01** (não é
timestamp em ms), calculado no fuso horário local do aparelho.

---

## 7. Limitação real da API

O endpoint por **URL** `GET /rest/food-entries/v2` pertence à superfície REST
**OAuth 2.0** do FatSecret e **não** concede acesso delegado ao diário privado de
uma conta existente. O acesso ao diário de terceiro (3-legged) é feito pelo
endpoint **método-based** `rest/server.api` com `method=food_entries.get.v2`,
assinado com OAuth 1.0a — que é o que este app usa. Por isso o “fallback” pedido
é, na prática, o **caminho principal e correto** para esta necessidade.

Além disso, a API exige **whitelist de IP** (OAuth 1.0), o que pode dificultar o
uso a partir de redes móveis com IP variável — configure no painel do FatSecret.

Uma terceira limitação, das metas diárias, está documentada na
[seção 4](#4-metas-nutricionais): a API **não** expõe meta de calorias nem de
macros — só meta de peso, em `profile.get`.

---

## 7.1 Ícone, tema e interação dos widgets

**Ícone.** Ícone adaptativo próprio (anel de progresso da meta diária + folha),
em `drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml` e
`ic_launcher_monochrome.xml` (camada *themed icon* do Android 13+). Toda a marca
cabe na *safe zone* de 66 dp, então nenhuma máscara de launcher corta o desenho.
Os `mipmap-*/ic_launcher*.webp` são só o *fallback* legado e foram regerados a
partir da mesma geometria.

**Tema claro/escuro do widget.** O widget é inflado pelo processo do
**launcher**, não pelo nosso — por isso ele segue o modo noturno do launcher, e
não o do app. Em `WidgetColors.kt` cada cor é um par dia/noite
(`androidx.glance.color.ColorProvider(day, night)`); o Glance traduz isso para
`RemoteViews.setColorInt(day, night)` (API 31+, e o `minSdk` daqui é 34), de modo
que **o host escolhe a cor na hora de inflar**. O widget vira junto com o tema do
sistema na hora, sem depender de uma nova sincronização. Cores fixas ficariam
“congeladas” no tema vigente no último render — foi exatamente esse o bug
corrigido. Cores dinâmicas (Material You) foram evitadas de propósito: perderiam
o verde da marca e podem cair em combinações de contraste não verificáveis.

**Toque abre o Nutri Insights.** Tocar em qualquer ponto do corpo de qualquer
widget abre a `MainActivity` deste app, em **todos** os estados. A aba Hoje permite
comparar os números do widget nutricional; Peso faz o mesmo para o widget de peso;
o avatar abre **Metas e conta**, onde se ajustam metas, se reconecta a conta e se
força uma sincronização após falha. Uma versão
anterior abria o app oficial do FatSecret (com cascata para Play Store → site),
mas isso foi revertido a pedido; o helper `FatSecretApp.kt` e o bloco
`<queries>` do manifest que o acompanhava foram removidos junto. O `<queries>`
não é mais necessário porque nenhum código consulta mais
`getLaunchIntentForPackage`/`resolveActivity` — o `ACTION_VIEW` do fluxo OAuth
usa `startActivity` direto, que não sofre a filtragem de visibilidade de pacotes
do Android 11+.

**Redesenho enxuto.** A partir desta versão o widget prioriza uma leitura calma:
as calorias são o único elemento dominante, os macros são secundários (barras
finas, sem cartões preenchidos) e não há mais botão *Atualizar* nem bordas/
sombras/ícones. As cores (`WidgetColors.kt`) foram dessaturadas de propósito
para não “gritar” ao lado do papel de parede, mantendo todo texto ≥ 4.5:1 de
contraste (WCAG AA) em ambos os temas. O botão de atualizar saiu porque a
sincronização já é automática (abertura do app + WorkManager periódico); o
`SyncScheduler.syncNow` foi mantido, mas hoje sem chamador. O pequeno mostra só
o bloco de calorias — a ~86 dp úteis não cabe uma linha de macros sem risco de
corte quando a fonte do sistema está ampliada; os macros aparecem no tamanho
largo.

---

## 8. Testes

Testes unitários JVM em `app/src/test/`:

- OAuth: percent-encoding (RFC 3986), ordenação/normalização, *signature base
  string*, HMAC-SHA1 (vetor conhecido).
- Conversão `LocalDate` → dias desde 1970.
- Soma de nutrientes, percentuais, insights, metas iguais a zero, acima da meta.
- Parsing de `food_entry` único, lista, sem registros, números como string,
  nutrientes ausentes, objeto de erro, endpoint mensal.

Rodar:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
