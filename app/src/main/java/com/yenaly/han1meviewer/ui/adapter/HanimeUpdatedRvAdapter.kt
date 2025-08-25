package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.chad.library.adapter4.BaseDifferAdapter
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ItemHanimeUpdatedBinding
import com.yenaly.han1meviewer.logic.entity.download.HUpdateEntity
import com.yenaly.han1meviewer.ui.fragment.home.download.DownloadedFragment
import com.yenaly.han1meviewer.util.installApkPackage
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.han1meviewer.util.updateFile
import com.yenaly.han1meviewer.worker.HUpdateWorker
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2023/11/26 026 16:57
 */
class HanimeUpdatedRvAdapter(private val fragment: DownloadedFragment) :
  BaseDifferAdapter<HUpdateEntity, DataBindingHolder<ItemHanimeUpdatedBinding>>(COMPARATOR) {

  init {
    isStateViewEnable = true
  }

  companion object {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val COMPARATOR =
      object : DiffUtil.ItemCallback<HUpdateEntity>() {
        override fun areContentsTheSame(oldItem: HUpdateEntity, newItem: HUpdateEntity): Boolean {
          return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: HUpdateEntity, newItem: HUpdateEntity): Boolean {
          return oldItem.id == newItem.id
        }
      }
  }

  override fun onBindViewHolder(
    holder: DataBindingHolder<ItemHanimeUpdatedBinding>,
    position: Int,
    item: HUpdateEntity?,
  ) {
    item ?: return
    holder.binding.tvTitle.text = item.name
    holder.itemView.post {
      // fast path
      if (holder.itemView.height == holder.binding.vCoverBg.height) return@post
      holder.binding.vCoverBg.updateLayoutParams { height = holder.itemView.height }
      holder.binding.ivCoverBg.updateLayoutParams { height = holder.itemView.height }
    }
    val realSize = context.updateFile.length()
    holder.binding.tvSize.text =
      if (realSize == 0L) {
        "???"
      } else {
        item.length.formatFileSizeV2()
      }
  }

  override fun onCreateViewHolder(
    context: Context,
    parent: ViewGroup,
    viewType: Int,
  ): DataBindingHolder<ItemHanimeUpdatedBinding> {
    return DataBindingHolder(
        ItemHanimeUpdatedBinding.inflate(LayoutInflater.from(context), parent, false)
      )
      .also { viewHolder ->
        viewHolder.binding.btnDelete.setOnClickListener {
          val position = viewHolder.bindingAdapterPosition
          // #issue-158: 这里可能为空
          val item = getItem(position)
          item?.let {
            context.showAlertDialog {
              setTitle(R.string.sure_to_delete)
              setMessage(context.getString(R.string.prepare_to_delete_s, it.name))
              setPositiveButton(R.string.confirm) { _, _ -> HUpdateWorker.deleteUpdate(context) }
              setNegativeButton(R.string.cancel, null)
            }
          }
        }
        viewHolder.binding.btnInstall.setOnClickListener {
          scope.launch { context.installApkPackage(context.updateFile) }
        }
      }
  }
}
