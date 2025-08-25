package com.yenaly.han1meviewer.util

import android.content.Context
import com.jakewharton.disklrucache.DiskLruCache
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object TranslationCache {
  private const val CACHE_SIZE = 30L * 1024L * 1024L // 30 MB
  private var diskCache: DiskLruCache? = null

  // In-memory cache (fast lookups during app session)
  private val memoryCache = ConcurrentHashMap<String, String>()

  fun init(context: Context) {
    if (diskCache == null) {
      val cacheDir = File(context.cacheDir, "translation_cache")
      if (!cacheDir.exists()) cacheDir.mkdirs()

      diskCache = DiskLruCache.open(cacheDir, 1, 1, CACHE_SIZE)
    }
  }

  private fun key(text: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(text.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun get(text: String): String? {
    // 1️⃣ Try memory
    memoryCache[text]?.let {
      return it
    }

    // 2️⃣ Try disk
    val snapshot = diskCache?.get(key(text)) ?: return null
    snapshot.getInputStream(0).bufferedReader().use {
      val translated = it.readText()
      memoryCache[text] = translated // put back to memory for next time
      return translated
    }
  }

  fun put(text: String, translated: String) {
    // Save in memory
    memoryCache[text] = translated

    // Save in disk
    val editor = diskCache?.edit(key(text)) ?: return
    editor.newOutputStream(0).bufferedWriter().use { it.write(translated) }
    editor.commit()
  }

  fun sizeInMB(): Double {
    val sizeBytes = diskCache?.size() ?: 0L
    return sizeBytes / (1024.0 * 1024.0)
  }

  fun clear() {
    diskCache?.delete()
    memoryCache.clear()
  }
}
