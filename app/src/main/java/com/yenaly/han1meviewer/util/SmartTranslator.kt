package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.util.TagDictionary
import kotlinx.coroutines.*

object SmartTranslator {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    /* 1️⃣  Make translateTag available here */
    private suspend fun translateTag(rawText: String): String {
        val dictTranslation = TagDictionary.dict[rawText] as? String
        if (dictTranslation != null) return dictTranslation
        return MLKitTranslator.translate(rawText)
    }

    /* 2️⃣  Explicit type parameter to help the compiler */
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
