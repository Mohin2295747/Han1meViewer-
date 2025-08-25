package com.yenaly.han1meviewer.logic.state

/**
 * @author Yenaly Liew
 * @project Hanime1
 * @time 2022/06/08 008 22:20
 */
sealed class WebsiteState<out T> {
    data class Success<out T>(val info: T) : WebsiteState<T>()

    data object Loading : WebsiteState<Nothing>()

    data class Error(val throwable: Throwable) : WebsiteState<Nothing>()
}
