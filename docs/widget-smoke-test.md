# Checklist de fumaça dos widgets

Executar ao final de cada etapa do `planning.md`. Qualquer item falhando
bloqueia o avanço para a etapa seguinte (planning.md §11).

## Automatizado

```bash
./gradlew :app:testDebugUnitTest
```
```bash
./gradlew :app:assembleDebug
```

## Manual — os dois widgets, em ambos os tamanhos

1. Widget de nutrição mostra kcal e macros com os mesmos números da tela do app.
2. Widget de peso mostra peso, delta, tendência e progresso corretos.
3. Tocar no corpo de cada widget abre o app.
4. Alterar uma meta no app → os dois widgets refletem em um único refresh.
5. Alternar o tema do sistema (claro/escuro) → os widgets viram na hora, sem
   nova sincronização.
6. Modo avião → os widgets mantêm os últimos dados válidos e sinalizam a
   falha, sem zerar nada.
7. Forçar update: `adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE`.
8. Redimensionar cada widget na tela inicial (a escala é responsiva).

## Ciclo completo (obrigatório nas Etapas 3, 5 e 11)

9. Desconectar → widgets vão ao estado desconectado e o cache é limpo.
10. Reconectar (OAuth completo, via navegador e deep link) → widgets repopulados.
11. Fechar o app durante um sync → o sync termina e os widgets atualizam
    mesmo assim.
12. Reiniciar o aparelho → widgets renderizam do cache; o worker periódico
    volta.

## Registro de execuções

| Data | Etapa | Resultado | Observações |
|---|---|---|---|
| 2026-07-25 | Etapa 0 (linha de base) | ver abaixo | ambiente sem dispositivo/emulador disponível nesta sessão; checklist automatizado executado, checklist manual pendente de execução em dispositivo |
