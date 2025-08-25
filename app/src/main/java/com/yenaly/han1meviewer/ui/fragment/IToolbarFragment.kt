package com.yenaly.han1meviewer.ui.fragment

import androidx.appcompat.app.AppCompatActivity

/**
 * 用於 Fragment 要求 Activity 實現 Toolbar 的接口
 *
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2023/08/27 027 16:29
 */
interface IToolbarFragment<T : AppCompatActivity> {

    /** 設置 Toolbar */
    fun T.setupToolbar()
}
