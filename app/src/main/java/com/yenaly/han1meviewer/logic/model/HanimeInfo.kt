package com.yenaly.han1meviewer.logic.model

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 */
interface HanimeInfoType : MultiItemEntity

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2022/06/08 008 22:56
 */
data class HanimeInfo(
    val title: TranslatableText, // Changed from String
    val coverUrl: String,
    val videoCode: String,
    val duration: String? = null,
    val uploader: String? = null,
    val views: String? = null,
    val uploadTime: String? = null,
    val genre: String? = null,
    val isPlaying: Boolean = false, // for video playlist only.
    override var itemType: Int,
) : HanimeInfoType {
    companion object {
        const val NORMAL = 0
        const val SIMPLIFIED = 1
    }
}
