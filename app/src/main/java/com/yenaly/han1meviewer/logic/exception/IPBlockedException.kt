package com.yenaly.han1meviewer.logic.exception

/**
 * IP被封鎖
 *
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2023/08/07 007 12:40
 */
class IPBlockedException(reason: String) : CloudFlareBlockedException(reason)
