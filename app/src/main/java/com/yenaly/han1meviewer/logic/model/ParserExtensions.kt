// Create: app/src/main/java/com/yenaly/han1meviewer/util/ParserExtensions.kt
package com.yenaly.han1meviewer.util

fun <T> T?.logIfParseNull(tag: String, fieldName: String, loginNeeded: Boolean = false): T? = this

fun <T> T?.throwIfParseNull(tag: String, fieldName: String): T =
  this ?: throw IllegalStateException("$fieldName is null")

fun Element.childOrNull(index: Int): Element? = children().getOrNull(index)

fun Element.hasAttr(attr: String): Boolean = hasAttr(attr)

fun Element.ownText(): String = ownText()

fun Element.selectFirst(cssQuery: String): Element? = selectFirst(cssQuery)

fun Element.absUrl(attr: String): String = absUrl(attr)

fun Elements.getElementsByClass(className: String): Elements = getElementsByClass(className)

suspend fun Element.forEachStep2(action: suspend (Element) -> Unit) {
  children().forEach { action(it) }
}
