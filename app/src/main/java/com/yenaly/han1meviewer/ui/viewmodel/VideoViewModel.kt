package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.logic.model.TranslatableText
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.util.TranslationManager
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : YenalyViewModel(application) {

    private val _videoDetailFlow = MutableStateFlow<VideoLoadingState<HanimeVideo>>(VideoLoadingState.Loading)
    val videoDetailFlow = _videoDetailFlow.asStateFlow()

    fun loadVideoDetail(code: String) {
        viewModelScope.launch {
            _videoDetailFlow.value = VideoLoadingState.Loading

            NetworkRepo.getVideoDetail(code)
                .catch { exception ->
                    _videoDetailFlow.value = VideoLoadingState.Error(exception)
                }
                .collectLatest { video ->
                    // Collect all translatable text fields
                    val textsToTranslate = mutableListOf<TranslatableText>()
                    textsToTranslate.add(video.title)
                    video.introduction?.let { textsToTranslate.add(it) }
                    video.tags.forEach { tag -> textsToTranslate.add(tag) }
                    
                    // Also collect from related videos
                    video.relatedHanimes.forEach { relatedVideo ->
                        textsToTranslate.add(relatedVideo.title)
                    }
                    
                    // Also collect from playlist videos
                    video.playlist?.video?.forEach { playlistVideo ->
                        textsToTranslate.add(playlistVideo.title)
                    }

                    // Update flow with video first (shows raw text immediately)
                    _videoDetailFlow.value = VideoLoadingState.Success(video)

                    // Trigger batch translation in background
                    launch {
                        TranslationManager.translateBatch(textsToTranslate)
                        
                        // Update flow again when translations complete
                        // This will trigger UI recomposition with translated text
                        _videoDetailFlow.value = VideoLoadingState.Success(video)
                    }
                }
        }
    }

    // Optional: Function to manually trigger translation refresh
    fun refreshTranslations(video: HanimeVideo) {
        viewModelScope.launch {
            val textsToTranslate = mutableListOf<TranslatableText>()
            textsToTranslate.add(video.title)
            video.introduction?.let { textsToTranslate.add(it) }
            video.tags.forEach { tag -> textsToTranslate.add(tag) }
            
            video.relatedHanimes.forEach { relatedVideo ->
                textsToTranslate.add(relatedVideo.title)
            }
            
            video.playlist?.video?.forEach { playlistVideo ->
                textsToTranslate.add(playlistVideo.title)
            }

            TranslationManager.translateBatch(textsToTranslate)
            
            // Update state to trigger UI refresh
            _videoDetailFlow.value = VideoLoadingState.Success(video)
        }
    }

    // Optional: Clear pending translations when needed
    fun clearTranslations() {
        TranslationManager.clearPending()
    }
}