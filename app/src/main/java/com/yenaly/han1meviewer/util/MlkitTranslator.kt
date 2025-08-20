package com.yenaly.han1meviewer.util

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation

object MLKitTranslator {
    private var jaToEn: Translator? = null
    private var zhToEn: Translator? = null

    fun init(context: Context) {
        if (jaToEn == null) {
            val jaOptions = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            jaToEn = Translation.getClient(jaOptions)
            jaToEn!!.downloadModelIfNeeded()
        }

        if (zhToEn == null) {
            val zhOptions = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.CHINESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            zhToEn = Translation.getClient(zhOptions)
            zhToEn!!.downloadModelIfNeeded()
        }
    }

    fun translate(text: String, callback: (String) -> Unit) {
        val translator = if (text.any { it.code in 0x3040..0x30FF || it.code in 0x4E00..0x9FFF }) {
            // contains Japanese or Kanji → assume Japanese
            jaToEn
        } else {
            zhToEn
        }

        translator?.translate(text)
            ?.addOnSuccessListener { callback(it) }
            ?.addOnFailureListener { callback(text) } // fallback to original
    }
}
