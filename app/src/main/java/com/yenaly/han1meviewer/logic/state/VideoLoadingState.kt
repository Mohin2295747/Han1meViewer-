package com.yenaly.han1meviewer.logic.state

/**
 * @author Yenaly Liew
 * @project Hanime1
 * @time 2022/06/18 018 18:14
 */
sealed class VideoLoadingState<out T> {
    data class Success<out T>(val info: T) : VideoLoadingState<T>()

    data class Error(val throwable: Throwable) : VideoLoadingState<Nothing>()

    data object Loading : VideoLoadingState<Nothing>()

    data object NoContent : VideoLoadingState<Nothing>()
}
