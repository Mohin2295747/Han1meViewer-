package com.yenaly.han1meviewer.util

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset

object TagDictionary {
    private var _dict: Map<String, String>? = null

    val dict: Map<String, String>
        get() = _dict ?: emptyMap()

    fun init(context: Context) {
        if (_dict != null) return // already loaded

        val inputStream = context.assets.open("search_options/tags.json")
        val jsonString = inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
        val root = JSONObject(jsonString)

        val tempDict = mutableMapOf<String, String>()

        // loop over top-level keys (like "video_attributes", "genre", etc.)
        root.keys().forEach { category ->
            val items = root.getJSONArray(category)
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val lang = item.getJSONObject("lang")

                val zhCN = lang.optString("zh-rCN")
                val zhTW = lang.optString("zh-rTW")
                val en = lang.optString("en")

                if (en.isNotEmpty()) {
                    if (zhCN.isNotEmpty()) tempDict[zhCN] = en
                    if (zhTW.isNotEmpty()) tempDict[zhTW] = en
                }
            }
        }

        _dict = tempDict
    }
}
