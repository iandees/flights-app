package com.iandees.flights.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.network.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: AppSettingsRepository,
) : ViewModel() {

    val apiKey = repo.airLabsApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "",
    )

    fun saveApiKey(key: String) {
        viewModelScope.launch { repo.setAirLabsApiKey(key.trim()) }
    }
}
