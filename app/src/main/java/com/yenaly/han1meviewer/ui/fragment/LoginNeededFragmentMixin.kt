package com.yenaly.han1meviewer.ui.fragment

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin

/**
 * 用於需要登錄的 Fragment
 *
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2023/08/29 029 15:32
 */
@JvmDefaultWithoutCompatibility
interface LoginNeededFragmentMixin {
  /** 檢查是否已經登錄，如果沒有則返回上一個 Fragment */
  fun Fragment.checkLogin() {
    if (!isAlreadyLogin) {
      findNavController().navigateUp()
    }
  }
}
