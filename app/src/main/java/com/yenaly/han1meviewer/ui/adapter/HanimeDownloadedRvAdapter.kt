package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import com.chad.library.adapter4.BaseDifferAdapter
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.yenaly.han1meviewer.HFileManager
import com.yenaly.han1meviewer.LOCAL_DATE_TIME_FORMAT
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.VIDEO_CODE
import com.yenaly.han1meviewer.databinding.ItemHanimeDownloadedBinding
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import com.yenaly.han1meviewer.ui.activity.VideoActivity
import com.yenaly.han1meviewer.ui.fragment.home.download.DownloadedFragment
import com.yenaly.han1meviewer.util.HImageMeower.loadUnhappily
import com.yenaly.han1meviewer.util.MediaUtils
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoInActivity
import com.yenaly.han1meviewer.util.showAlertDialog
import com.yenaly.yenaly_libs.utils.activity
import com.yenaly.yenaly_libs.utils.dpF
import com.yenaly.yenaly_libs.utils.formatFileSizeV2
import com.yenaly.yenaly_libs.utils.startActivity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

/**
 * @author Yenaly Liew
 * @project Han1meViewer
 * @time 2023/11/26 026 16:57
 */
class HanimeDownloadedRvAdapter(private val fragment: DownloadedFragment) :
  BaseDifferAdapter<VideoWithCategories, DataBindingHolder<ItemHanimeDownloadedBinding>>(
    COMPARATOR
  ) {

  init {
    isStateViewEnable = true
  }

  companion object {
    val COMPARATOR =
      object : DiffUtil.ItemCallback<VideoWithCategories>() {
        override fun areContentsTheSame(
          oldItem: VideoWithCategories,
          newItem: VideoWithCategories,
        ): Boolean {
          return oldItem == newItem
        }

        override fun areItemsTheSame(
          oldItem: VideoWithCategories,
          newItem: VideoWithCategories,
        ): Boolean {
          return oldItem.video.id == newItem.video.id
        }
      }
  }

  override fun onBindViewHolder(
    holder: DataBindingHolder<ItemHanimeDownloadedBinding>,
    position: Int,
    item: VideoWithCategories?,
  ) {
    item ?: return
    holder.binding.tvTitle.text = item.video.title
    holder.binding.tvVideoCode.text = item.video.videoCode
    holder.binding.ivCover.loadUnhappily(item.video.coverUri, item.video.coverUrl)
    holder.itemView.post {
      // fast path
      if (holder.itemView.height == holder.binding.vCoverBg.height) return@post
      holder.binding.vCoverBg.updateLayoutParams { height = holder.itemView.height }
      holder.binding.ivCoverBg.updateLayoutParams { height = holder.itemView.height }
    }
    holder.binding.ivCoverBg.apply {
      loadUnhappily(item.video.coverUri, item.video.coverUrl)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(RenderEffect.createBlurEffect(8.dpF, 8.dpF, Shader.TileMode.CLAMP))
      }
    }
    holder.binding.tvAddedTime.text =
      Instant.fromEpochMilliseconds(item.video.addDate)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(LOCAL_DATE_TIME_FORMAT)

    val realSize = item.video.videoUri.toUri().toFile().length()
    holder.binding.tvSize.text =
      if (realSize == 0L) {
        "???"
      } else {
        item.video.length.formatFileSizeV2()
      }
    holder.binding.tvQuality.text = item.video.quality
  }

  override fun onCreateViewHolder(
    context: Context,
    parent: ViewGroup,
    viewType: Int,
  ): DataBindingHolder<ItemHanimeDownloadedBinding> {
    return DataBindingHolder(
        ItemHanimeDownloadedBinding.inflate(LayoutInflater.from(context), parent, false)
      )
      .also { viewHolder ->
        viewHolder.itemView.setOnClickListener {
          val position = viewHolder.bindingAdapterPosition
          val item = getItem(position) ?: return@setOnClickListener
          context.activity?.startActivity<VideoActivity>(VIDEO_CODE to item.video.videoCode)
        }
        viewHolder.binding.btnDelete.setOnClickListener {
          val position = viewHolder.bindingAdapterPosition
          // #issue-158: 这里可能为空
          val item = getItem(position)
          item?.let {
            context.showAlertDialog {
              setTitle(R.string.sure_to_delete)
              setMessage(context.getString(R.string.prepare_to_delete_s, it.video.title))
              setPositiveButton(R.string.confirm) { _, _ ->
                // if (file.exists()) file.delete()
                HFileManager.getDownloadVideoFolder(it.video.videoCode).deleteRecursively()
                fragment.viewModel.deleteDownloadHanimeBy(it.video.videoCode, it.video.quality)
              }
              setNegativeButton(R.string.cancel, null)
            }
          }
        }
        viewHolder.binding.btnLocalPlayback.setOnClickListener {
          val position = viewHolder.bindingAdapterPosition
          val item = getItem(position) ?: return@setOnClickListener
          //                context.openDownloadedHanimeVideoLocally(item.video.videoUri,
          // onFileNotFound = {
          //                    context.showAlertDialog {
          //                        setTitle(R.string.video_not_exist)
          //                        setMessage(R.string.video_deleted_sure_to_delete_item)
          //                        setPositiveButton(R.string.delete) { _, _ ->
          //                            fragment.viewModel.deleteDownloadHanimeBy(
          //                                item.video.videoCode,
          //                                item.video.quality
          //                            )
          //                        }
          //                        setNegativeButton(R.string.cancel, null)
          //                    }
          //                })
          context.openDownloadedHanimeVideoInActivity(item.video.videoCode)
        }
        viewHolder.binding.btnExternalPlayback.setOnClickListener {
          val position = viewHolder.bindingAdapterPosition
          val item = getItem(position) ?: return@setOnClickListener
          MediaUtils.playMedia(context, item.video.videoUri)
        }
      }
  }
}
