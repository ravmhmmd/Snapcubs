package com.cubing.snapcubs2.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cubing.snapcubs2.network.ApiService

class StoryViewModelFactory(
    private val api: ApiService,
    private val token: String
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(api, token) as T
    }
}
