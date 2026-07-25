package com.example.widgetfatsecret.ui

/** One-shot UI signals emitted by account actions. */
sealed interface UiEvent {
    data class OpenBrowser(val url: String) : UiEvent
    data class Message(val text: String) : UiEvent
}
