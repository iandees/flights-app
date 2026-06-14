package com.iandees.flights.core.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val AIRLABS_API_KEY = stringPreferencesKey("airlabs_api_key")
    }

    val airLabsApiKey: Flow<String> = context.dataStore.data
        .map { it[AIRLABS_API_KEY] ?: "" }

    suspend fun setAirLabsApiKey(key: String) {
        context.dataStore.edit { it[AIRLABS_API_KEY] = key }
    }
}
