package com.yenaly.han1meviewer.util

import android.util.Log
import com.yenaly.han1meviewer.logic.model.TranslatableText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TranslationManager {
    private val pendingTranslations = mutableListOf<TranslatableText>()
    private val mutex = Mutex()
    private var translationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun translateBatch(texts: List<TranslatableText>) {
        mutex.withLock {
            texts.forEach { text ->
                if (!text.isTranslated() && !pendingTranslations.contains(text)) {
                    pendingTranslations.add(text)
                }
            }
            startTranslationIfNeeded()
        }
    }

    private suspend fun startTranslationIfNeeded() {
        if (translationJob?.isActive == true || pendingTranslations.isEmpty()) return

        translationJob =
            scope.launch {
                mutex.withLock {
                    val textsToTranslate = pendingTranslations.toList()
                    pendingTranslations.clear()

                    textsToTranslate.forEach { text ->
                        try {
                            if (!text.isTranslated()) {
                                text.translated = MLKitTranslator.translate(text.raw)
                            }
                        } catch (e: Exception) {
                            // Keep raw text if translation fails
                            Log.e("TranslationManager", "Failed to translate: ${text.raw}", e)
                        }
                    }
                }
            }
    }

    fun clearPending() {
        mutex.withLock {
            pendingTranslations.clear()
            translationJob?.cancel()
        }
    }
}
