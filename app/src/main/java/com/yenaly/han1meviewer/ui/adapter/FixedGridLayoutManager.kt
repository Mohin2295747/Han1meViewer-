package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2022/09/10 010 00:24
 */
class FixedGridLayoutManager(context: Context?, spanCount: Int) :
  GridLayoutManager(context, spanCount) {
  override fun supportsPredictiveItemAnimations(): Boolean {
    return false
  }
}
