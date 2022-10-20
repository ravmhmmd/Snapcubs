package com.cubing.snapcubs2.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.cubing.snapcubs2.network.ApiService

class MainViewModel(private val api: ApiService, private val token: String) : ViewModel() {
    val quote = Pager(
        config = PagingConfig(pageSize = 5),
        pagingSourceFactory = {
            StoryPagingDataSource(api, token)
        }).flow.cachedIn(viewModelScope)
}

