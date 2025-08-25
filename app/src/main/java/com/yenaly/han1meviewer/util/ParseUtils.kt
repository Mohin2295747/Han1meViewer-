package com.yenaly.han1meviewer.util

fun <T> T?.logIfParseNull(tag: String = "Parser"): T? {
    if (this == null) {
        android.util.Log.w(tag, "Parse returned null")
    }
    return this
}
