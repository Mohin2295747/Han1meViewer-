package com.yenaly.han1meviewer.ui.view

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

/**
 * https://stackoverflow.com/questions/31235183
 *
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2024/04/04 004 17:49
 */
class LinearSmoothToStartScroller(context: Context?) : LinearSmoothScroller(context) {
    override fun getVerticalSnapPreference(): Int = SNAP_TO_START

    override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
}
