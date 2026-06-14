package com.iandees.flights.feature.importcsv

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.network.CsvImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Parsing : ImportUiState()
    data class Preview(
        val flightCount: Int,
        val skipped: Int,
        val errors: List<String>,
        val pendingUri: Uri,
    ) : ImportUiState()
    object Importing : ImportUiState()
    data class Done(val imported: Int) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

@HiltViewModel
class ImportCsvViewModel @Inject constructor(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    // Cached parse result to avoid re-parsing on confirm
    private var cachedResult: CsvImporter.ImportResult? = null

    fun onFilePicked(uri: Uri, openStream: (Uri) -> InputStream?) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Parsing
            try {
                val result = withContext(Dispatchers.IO) {
                    val stream = openStream(uri) ?: error("Could not open file")
                    CsvImporter.parse(stream)
                }
                cachedResult = result
                _uiState.value = ImportUiState.Preview(
                    flightCount = result.flights.size,
                    skipped     = result.skippedRows,
                    errors      = result.errors,
                    pendingUri  = uri,
                )
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun confirmImport() {
        val result = cachedResult ?: return
        viewModelScope.launch {
            _uiState.value = ImportUiState.Importing
            try {
                withContext(Dispatchers.IO) {
                    repository.importFlights(result.flights)
                }
                _uiState.value = ImportUiState.Done(result.flights.size)
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun reset() {
        cachedResult = null
        _uiState.value = ImportUiState.Idle
    }
}
