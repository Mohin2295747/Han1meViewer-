package com.yenaly.han1meviewer.logic.model

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2022/07/05 005 15:30
 */
data class MyListItems<I>(
    val hanimeInfo: List<I>,
    /** 清單的介紹，一般用於播放清單 */
    var desc: String? = null,
    val csrfToken: String? = null,
)
