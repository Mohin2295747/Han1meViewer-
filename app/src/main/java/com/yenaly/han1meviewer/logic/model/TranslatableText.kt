package com.yenaly.han1meviewer.logic.model

import androidx.compose.runtime.Stable

@Stable
data class TranslatableText(
    val raw: String,
    var translated: String? = null,
    var isTranslating: Boolean = false,
    var translationError: Boolean = false,
) {
    val displayText: String
        get() = translated ?: raw

    val isTranslated: Boolean
        get() = translated != null

    fun markTranslating() = apply { isTranslating = true }

    fun markTranslated(result: String) = apply {
        translated = result
        isTranslating = false
        translationError = false
    }

    fun markTranslationFailed() = apply {
        isTranslating = false
        translationError = true
    }

    companion object {
        fun fromRaw(text: String): TranslatableText = TranslatableText(text)
    }
}
