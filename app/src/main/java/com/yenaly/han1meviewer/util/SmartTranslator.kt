package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.logic.Parser.translateTag
import kotlinx.coroutines.*

object SmartTranslator {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    /**
     * Same return type as runBlocking { MLKitTranslator.translate(raw) }.
     * Immediately returns `raw`, later calls `updater(translated)` on the
     * background thread so the same object field is swapped in-place.
     */
    fun <T> translateAsync(
        obj: T,
        raw: String,
        updater: T.(String) -> Unit
    ): String {
        scope.launch {
            val translated = withContext(Dispatchers.IO) { translateTag(raw) }
            obj.updater(translated)
        }
        return raw
    }
}
