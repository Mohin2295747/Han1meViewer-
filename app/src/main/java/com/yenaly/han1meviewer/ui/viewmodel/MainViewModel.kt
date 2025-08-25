package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.model.HomePage
import com.yenaly.han1meviewer.logic.model.TranslatableText
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.util.TranslationManager
import com.yenaly.yenaly_libs.base.YenalyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : YenalyViewModel(application) {

    private val _homeStateFlow = MutableStateFlow<WebsiteState<HomePage>>(WebsiteState.Loading)
    val homeStateFlow = _homeStateFlow.asStateFlow()

    fun loadHome() {
        viewModelScope.launch {
            _homeStateFlow.value = WebsiteState.Loading

            NetworkRepo.getHome()
                .catch { exception -> _homeStateFlow.value = WebsiteState.Error(exception) }
                .collectLatest { homePage ->
                    // Collect all translatable text fields from home page
                    val textsToTranslate = mutableListOf<TranslatableText>()

                    // Collect from all sections
                    homePage.latestHanime?.forEach { textsToTranslate.add(it.title) }
                    homePage.latestRelease?.forEach { textsToTranslate.add(it.title) }
                    homePage.latestUpload?.forEach { textsToTranslate.add(it.title) }
                    homePage.chineseSubtitle?.forEach { textsToTranslate.add(it.title) }
                    homePage.hanimeTheyWatched?.forEach { textsToTranslate.add(it.title) }
                    homePage.hanimeCurrent?.forEach { textsToTranslate.add(it.title) }
                    homePage.hotHanimeMonthly?.forEach { textsToTranslate.add(it.title) }

                    // Collect banner title if available
                    homePage.banner?.title?.let { bannerTitle ->
                        textsToTranslate.add(TranslatableText.fromRaw(bannerTitle))
                    }

                    // Update flow with home page first (shows raw text immediately)
                    _homeStateFlow.value = WebsiteState.Success(homePage)

                    // Trigger batch translation in background
                    launch {
                        TranslationManager.translateBatch(textsToTranslate)

                        // Update flow again when translations complete
                        // This will trigger UI recomposition with translated text
                        _homeStateFlow.value = WebsiteState.Success(homePage)
                    }
                }
        }
    }

    // Optional: Function to manually trigger translation refresh
    fun refreshTranslations(homePage: HomePage) {
        viewModelScope.launch {
            val textsToTranslate = mutableListOf<TranslatableText>()

            // Collect from all sections
            homePage.latestHanime?.forEach { textsToTranslate.add(it.title) }
            homePage.latestRelease?.forEach { textsToTranslate.add(it.title) }
            homePage.latestUpload?.forEach { textsToTranslate.add(it.title) }
            homePage.chineseSubtitle?.forEach { textsToTranslate.add(it.title) }
            homePage.hanimeTheyWatched?.forEach { textsToTranslate.add(it.title) }
            homePage.hanimeCurrent?.forEach { textsToTranslate.add(it.title) }
            homePage.hotHanimeMonthly?.forEach { textsToTranslate.add(it.title) }

            // Collect banner title if available
            homePage.banner?.title?.let { bannerTitle ->
                textsToTranslate.add(TranslatableText.fromRaw(bannerTitle))
            }

            TranslationManager.translateBatch(textsToTranslate)

            // Update state to trigger UI refresh
            _homeStateFlow.value = WebsiteState.Success(homePage)
        }
    }

    // Optional: Clear pending translations when needed
    fun clearTranslations() {
        TranslationManager.clearPending()
    }
}
