package com.yenaly.han1meviewer.logic.network

import java.net.InetAddress
import okhttp3.Dns

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2024/03/29 029 17:14
 */
object GitHubDns : Dns {
  override fun lookup(hostname: String): List<InetAddress> {
    return when (hostname) {
      "api.github.com" -> listOf(InetAddress.getByName("140.82.121.6"))
      "github.com" -> listOf(InetAddress.getByName("140.82.121.4"))
      else -> Dns.SYSTEM.lookup(hostname)
    }
  }
}
