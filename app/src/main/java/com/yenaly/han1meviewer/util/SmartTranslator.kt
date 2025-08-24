package com.yenaly.han1meviewer.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.*

object SmartTranslator {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    // --- Progress tracking ---
    private var totalToTranslate = 0
    private var doneSoFar = 0
    private var lastShownPercent = 0
    private var appContext: Context? = null

    /** Call this before starting a batch of translations */
    fun init(context: Context, total: Int) {
        appContext = context.applicationContext
        totalToTranslate = total
        doneSoFar = 0
        lastShownPercent = 0
    }

    /** Core tag translation */
    suspend fun translateTag(rawText: String): String {
        val dictTranslation = TagDictionary.dict[rawText] as? String
        if (dictTranslation != null) return dictTranslation
        return MLKitTranslator.translate(rawText)
    }

    /** Async translation with UI update + progress popup */
    fun <T> translateAsync(
        obj: T,
        raw: String,
        updater: T.(String) -> Unit,
        notifyUI: (() -> Unit)? = null
    ): String {
        scope.launch {
            val translated = withContext(Dispatchers.IO) { translateTag(raw) }
            obj.updater(translated)
            notifyUI?.invoke()

            // Update progress
            doneSoFar++
            showProgressIfNeeded()
        }
        return raw
    }

    private fun showProgressIfNeeded() {
        if (totalToTranslate == 0) return
        val percent = (doneSoFar * 100) / totalToTranslate

        if (percent >= lastShownPercent + 20) { // show every 20%
            appContext?.let {
                val msg = if (percent >= 100) "Translation Completed ✅"
                          else "Translating... $percent%"
                Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
            }
            lastShownPercent = percent
        }
    }
}
