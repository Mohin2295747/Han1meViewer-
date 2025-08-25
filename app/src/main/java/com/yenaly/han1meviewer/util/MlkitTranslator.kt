package com.yenaly.han1meviewer.util

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object MLKitTranslator {
    private var jaToEn: Translator? = null
    private var zhToEn: Translator? = null

    fun init(context: Context) {
        if (jaToEn == null) {
            val jaOptions =
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.JAPANESE)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()
            jaToEn = Translation.getClient(jaOptions)
            jaToEn!!.downloadModelIfNeeded()
        }

        if (zhToEn == null) {
            val zhOptions =
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.CHINESE)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()
            zhToEn = Translation.getClient(zhOptions)
            zhToEn!!.downloadModelIfNeeded()
        }

        // ✅ Initialize cache
        TranslationCache.init(context)
    }

    private fun detectLanguage(text: String): Translator? {
        return when {
            text.any { it.code in 0x3040..0x30FF } -> jaToEn // Hiragana/Katakana
            text.any { it.code in 0x4E00..0x9FFF } -> zhToEn // Chinese characters
            else -> null
        }
    }

    suspend fun translate(text: String): String = suspendCancellableCoroutine { cont ->
        // ✅ Check cache first (memory + disk)
        TranslationCache.get(text)?.let {
            cont.resume(it)
            return@suspendCancellableCoroutine
        }

        val translator = detectLanguage(text)
        if (translator == null) {
            cont.resume(text)
            return@suspendCancellableCoroutine
        }

        translator
            .translate(text)
            .addOnSuccessListener { translated ->
                // ✅ Save to cache
                TranslationCache.put(text, translated)
                cont.resume(translated)
            }
            .addOnFailureListener { cont.resume(text) }
    }

    suspend fun translate(text: TranslatableText) {
        if (!text.isTranslated()) {
            text.translated = translate(text.raw)
        }
    }
}
