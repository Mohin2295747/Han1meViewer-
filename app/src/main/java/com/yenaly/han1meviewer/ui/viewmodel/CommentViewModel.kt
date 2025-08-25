package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.CommentPlace
import com.yenaly.han1meviewer.logic.model.TranslatableText
import com.yenaly.han1meviewer.logic.model.VideoCommentArgs
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel.csrfToken
import com.yenaly.han1meviewer.util.TranslationManager
import com.yenaly.yenaly_libs.base.YenalyViewModel
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : YenalyViewModel(application) {

    lateinit var code: String
    var currentUserId: String? = null

    private val _videoCommentStateFlow =
        MutableStateFlow<WebsiteState<VideoComments>>(WebsiteState.Loading)
    val videoCommentStateFlow = _videoCommentStateFlow.asStateFlow()

    private val _videoReplyStateFlow =
        MutableStateFlow<WebsiteState<VideoComments>>(WebsiteState.Loading)
    val videoReplyStateFlow = _videoReplyStateFlow.asStateFlow()

    private val _videoCommentFlow = MutableStateFlow<List<VideoComments.VideoComment>>(emptyList())
    val videoCommentFlow = _videoCommentFlow.asStateFlow()

    private val _videoReplyFlow = MutableStateFlow<List<VideoComments.VideoComment>>(emptyList())
    val videoReplyFlow = _videoReplyFlow.asStateFlow()

    private val _postCommentFlow = MutableSharedFlow<WebsiteState<Unit>>()
    val postCommentFlow = _postCommentFlow.asSharedFlow()

    private val _postReplyFlow = MutableSharedFlow<WebsiteState<Unit>>()
    val postReplyFlow = _postReplyFlow.asSharedFlow()

    private val _commentLikeFlow = MutableSharedFlow<WebsiteState<VideoCommentArgs>>()
    val commentLikeFlow = _commentLikeFlow.asSharedFlow()

    fun getComment(type: String, code: String) {
        viewModelScope.launch {
            _videoCommentStateFlow.value = WebsiteState.Loading
            NetworkRepo.getComments(type, code)
                .catch { _videoCommentStateFlow.value = WebsiteState.Error(it) }
                .collectLatest { state ->
                    _videoCommentStateFlow.value = state
                    if (state is WebsiteState.Success) {
                        // Update flow with comments first (shows raw text immediately)
                        _videoCommentFlow.value = state.info.videoComment

                        // Collect all comment content for translation
                        val textsToTranslate = mutableListOf<TranslatableText>()
                        state.info.videoComment.forEach { comment ->
                            textsToTranslate.add(comment.content)
                        }

                        // Trigger batch translation in background
                        launch {
                            TranslationManager.translateBatch(textsToTranslate)

                            // Update flow again when translations complete
                            _videoCommentFlow.value = state.info.videoComment
                        }
                    }
                }
        }
    }

    fun getCommentReply(commentId: String) {
        viewModelScope.launch {
            _videoReplyStateFlow.value = WebsiteState.Loading
            NetworkRepo.getCommentReply(commentId)
                .catch { _videoReplyStateFlow.value = WebsiteState.Error(it) }
                .collectLatest { state ->
                    _videoReplyStateFlow.value = state
                    if (state is WebsiteState.Success) {
                        // Update flow with replies first (shows raw text immediately)
                        _videoReplyFlow.value = state.info.videoComment

                        // Collect all reply content for translation
                        val textsToTranslate = mutableListOf<TranslatableText>()
                        state.info.videoComment.forEach { reply ->
                            textsToTranslate.add(reply.content)
                        }

                        // Trigger batch translation in background
                        launch {
                            TranslationManager.translateBatch(textsToTranslate)

                            // Update flow again when translations complete
                            _videoReplyFlow.value = state.info.videoComment
                        }
                    }
                }
        }
    }

    fun postComment(currentUserId: String, targetUserId: String, type: String, text: String) {
        viewModelScope.launch {
            NetworkRepo.postComment(csrfToken, currentUserId, targetUserId, type, text)
                .catch { _postCommentFlow.emit(WebsiteState.Error(it)) }
                .collectLatest(_postCommentFlow::emit)
        }
    }

    fun postReply(replyCommentId: String, text: String) {
        viewModelScope.launch {
            NetworkRepo.postCommentReply(csrfToken, replyCommentId, text)
                .catch { _postReplyFlow.emit(WebsiteState.Error(it)) }
                .collectLatest(_postReplyFlow::emit)
        }
    }

    fun likeComment(
        isPositive: Boolean,
        commentPosition: Int,
        comment: VideoComments.VideoComment,
    ) = likeCommentInternal(CommentPlace.COMMENT, isPositive, commentPosition, comment)

    fun likeChildComment(
        isPositive: Boolean,
        commentPosition: Int,
        comment: VideoComments.VideoComment,
    ) = likeCommentInternal(CommentPlace.CHILD_COMMENT, isPositive, commentPosition, comment)

    private fun likeCommentInternal(
        place: CommentPlace,
        isPositive: Boolean,
        position: Int,
        comment: VideoComments.VideoComment,
    ) {
        viewModelScope.launch {
            NetworkRepo.likeComment(
                    csrfToken,
                    place,
                    comment.post.foreignId,
                    isPositive,
                    comment.post.likeUserId,
                    comment.post.commentLikesCount ?: 0,
                    comment.post.commentLikesSum ?: 0,
                    comment.post.likeCommentStatus,
                    comment.post.unlikeCommentStatus,
                    position,
                    comment,
                )
                .catch { _commentLikeFlow.emit(WebsiteState.Error(it)) }
                .collectLatest { argState ->
                    _commentLikeFlow.emit(argState)
                    if (argState is WebsiteState.Success) {
                        when (place) {
                            CommentPlace.COMMENT ->
                                _videoCommentFlow.update {
                                    it.toMutableList().apply {
                                        this[position] =
                                            this[position].handleCommentLike(argState.info)
                                    }
                                }
                            CommentPlace.CHILD_COMMENT ->
                                _videoReplyFlow.update {
                                    it.toMutableList().apply {
                                        this[position] =
                                            this[position].handleCommentLike(argState.info)
                                    }
                                }
                        }
                    }
                }
        }
    }

    private fun VideoComments.VideoComment.handleCommentLike(args: VideoCommentArgs) =
        if (args.isPositive) incLikesCount(cancel = post.likeCommentStatus)
        else decLikesCount(cancel = post.unlikeCommentStatus)

    fun handleCommentLike(args: VideoCommentArgs) {
        val msg =
            when {
                args.isPositive && args.comment.post.likeCommentStatus ->
                    R.string.cancel_thumb_up_success
                args.isPositive -> R.string.thumb_up_success
                !args.isPositive && args.comment.post.unlikeCommentStatus ->
                    R.string.cancel_thumb_down_success
                else -> R.string.thumb_down_success
            }
        showShortToast(msg)
    }

    // Optional: Function to manually trigger translation refresh for comments
    fun refreshCommentTranslations() {
        viewModelScope.launch {
            val currentComments = _videoCommentFlow.value
            if (currentComments.isNotEmpty()) {
                val textsToTranslate = mutableListOf<TranslatableText>()
                currentComments.forEach { comment -> textsToTranslate.add(comment.content) }

                TranslationManager.translateBatch(textsToTranslate)

                // Update flow to trigger UI refresh
                _videoCommentFlow.value = currentComments
            }
        }
    }

    // Optional: Function to manually trigger translation refresh for replies
    fun refreshReplyTranslations() {
        viewModelScope.launch {
            val currentReplies = _videoReplyFlow.value
            if (currentReplies.isNotEmpty()) {
                val textsToTranslate = mutableListOf<TranslatableText>()
                currentReplies.forEach { reply -> textsToTranslate.add(reply.content) }

                TranslationManager.translateBatch(textsToTranslate)

                // Update flow to trigger UI refresh
                _videoReplyFlow.value = currentReplies
            }
        }
    }

    // Optional: Clear pending translations when needed
    fun clearTranslations() {
        TranslationManager.clearPending()
    }
}
