package com.yenaly.han1meviewer.util

import android.content.Context
import java.nio.charset.Charset
import org.json.JSONArray
import org.json.JSONObject

object TagDictionary {
  private var _dict: Map<String, String>? = null

  val dict: Map<String, String>
    get() = _dict ?: emptyMap()

  fun init(context: Context) {
    if (_dict != null) return // already loaded

    val tempDict = mutableMapOf<String, String>()

    // 🔧 Add all JSON files you want to merge here
    val files = listOf("search_options/tags.json", "search_options/genre.json")

    for (file in files) {
      val inputStream = context.assets.open(file)
      val jsonString = inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }

      // Try to parse as JSONObject (tags.json style), fallback to JSONArray (genre.json
      // style)
      try {
        val root = JSONObject(jsonString)

        // case 1: tags.json → has categories
        root.keys().forEach { category ->
          val items = root.getJSONArray(category)
          parseItems(items, tempDict)
        }
      } catch (_: Exception) {
        // case 2: genre.json → array root
        val rootArr = JSONArray(jsonString)
        parseItems(rootArr, tempDict)
      }
    }

    _dict = tempDict
  }

  private fun parseItems(items: JSONArray, dict: MutableMap<String, String>) {
    for (i in 0 until items.length()) {
      val item = items.getJSONObject(i)
      val lang = item.getJSONObject("lang")

      val zhCN = lang.optString("zh-rCN")
      val zhTW = lang.optString("zh-rTW")
      val en = lang.optString("en")

      if (en.isNotEmpty()) {
        if (zhCN.isNotEmpty()) dict[zhCN] = en
        if (zhTW.isNotEmpty()) dict[zhTW] = en
      }
    }
  }
}
