package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import android.util.Log
import android.util.SparseArray
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.model.TranslatableText
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.util.TranslationManager
import com.yenaly.han1meviewer.util.loadAssetAs
import com.yenaly.yenaly_libs.base.YenalyViewModel
import com.yenaly.yenaly_libs.utils.unsafeLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @author Yenaly Liew
 * @project Hanime1
 * @time 2022/06/13 013 22:29
 */
class SearchViewModel(application: Application) : YenalyViewModel(application) {

  var page: Int = 1
  var query: String? = null

  // START: Use in [ChildCommentPopupFragment.kt]

  var genre: String? = null
  var sort: String? = null
  var broad: Boolean = false
  var year: Int? = null
  var month: Int? = null
  var duration: String? = null

  var subscriptionBrand: String? = null

  var tagMap = SparseArray<Set<SearchOption>>()
  var brandMap = SparseArray<Set<SearchOption>>()

  // END: Use in [ChildCommentPopupFragment.kt]

  // START: Use in [SearchOptionsPopupFragment.kt]

  val genres by unsafeLazy {
    loadAssetAs<List<SearchOption>>("search_options/genre.json").orEmpty()
  }

  val tags by unsafeLazy {
    loadAssetAs<Map<String, List<SearchOption>>>("search_options/tags.json").orEmpty()
  }

  val brands by unsafeLazy {
    loadAssetAs<List<SearchOption>>("search_options/brands.json").orEmpty()
  }

  val sortOptions by unsafeLazy {
    loadAssetAs<List<SearchOption>>("search_options/sort_option.json").orEmpty()
  }

  val durations by unsafeLazy {
    loadAssetAs<List<SearchOption>>("search_options/duration.json").orEmpty()
  }

  // END: Use in [SearchOptionsPopupFragment.kt]

  private val _searchStateFlow =
    MutableStateFlow<PageLoadingState<List<HanimeInfo>>>(PageLoadingState.Loading)
  val searchStateFlow = _searchStateFlow.asStateFlow()

  private val _searchFlow = MutableStateFlow(emptyList<HanimeInfo>())
  val searchFlow = _searchFlow.asStateFlow()

  fun clearHanimeSearchResult() = _searchStateFlow.update { PageLoadingState.Loading }

  fun getHanimeSearchResult(
    page: Int,
    query: String?,
    genre: String?,
    sort: String?,
    broad: Boolean,
    year: Int?,
    month: Int?,
    duration: String?,
    tags: Set<String>,
    brands: Set<String>,
  ) {
    viewModelScope.launch {
      var date: String? = null
      if (year != null && month != null) {
        date = "$year 年 $month 月"
      }
      if (year != null && month == null) {
        date = "$year 年"
      }
      NetworkRepo.getHanimeSearchResult(
          page,
          query,
          genre,
          sort,
          broad,
          date,
          duration,
          tags,
          brands,
        )
        .collect { state ->
          when (state) {
            is PageLoadingState.Success -> {
              // Update flow with search results first (shows raw text immediately)
              _searchStateFlow.value = PageLoadingState.Success(state.info)
              _searchFlow.update { prevList ->
                if (page == 1) state.info else prevList + state.info
              }

              // Collect translatable texts and trigger batch translation
              val textsToTranslate = mutableListOf<TranslatableText>()
              state.info.forEach { hanimeInfo -> textsToTranslate.add(hanimeInfo.title) }

              // Trigger batch translation in background
              launch {
                TranslationManager.translateBatch(textsToTranslate)

                // Update flow again when translations complete
                _searchStateFlow.value = PageLoadingState.Success(state.info)
                _searchFlow.update { currentList ->
                  // Return current list to trigger UI update
                  currentList
                }
              }
            }
            is PageLoadingState.Loading -> {
              _searchStateFlow.value = state
              if (page == 1) {
                _searchFlow.value = emptyList()
              }
            }
            is PageLoadingState.Error -> {
              _searchStateFlow.value = state
            }
            is PageLoadingState.NoMoreData -> {
              _searchStateFlow.value = state
            }
          }
        }
    }
  }

  // Optional: Function to manually trigger translation refresh for current search results
  fun refreshTranslations() {
    viewModelScope.launch {
      val currentResults = _searchFlow.value
      if (currentResults.isNotEmpty()) {
        val textsToTranslate = mutableListOf<TranslatableText>()
        currentResults.forEach { hanimeInfo -> textsToTranslate.add(hanimeInfo.title) }

        TranslationManager.translateBatch(textsToTranslate)

        // Update state to trigger UI refresh
        _searchStateFlow.value = PageLoadingState.Success(currentResults)
        _searchFlow.update { it } // Trigger flow update
      }
    }
  }

  // Optional: Clear pending translations when needed
  fun clearTranslations() {
    TranslationManager.clearPending()
  }

  fun insertSearchHistory(history: SearchHistoryEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      DatabaseRepo.SearchHistory.insert(history)
      Log.d("insert_search_hty", "$history DONE!")
    }
  }

  fun deleteSearchHistory(history: SearchHistoryEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      DatabaseRepo.SearchHistory.delete(history)
      Log.d("delete_search_hty", "$history DONE!")
    }
  }

  @JvmOverloads
  fun loadAllSearchHistories(keyword: String? = null) =
    DatabaseRepo.SearchHistory.loadAll(keyword).flowOn(Dispatchers.IO)

  fun deleteSearchHistoryByKeyword(query: String) {
    viewModelScope.launch(Dispatchers.IO) {
      DatabaseRepo.SearchHistory.deleteByKeyword(query)
      Log.d("delete_search_hty", "$query DONE!")
    }
  }
}
