package com.findurdrugz.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.findurdrugz.android.data.model.MedicineResult
import com.findurdrugz.android.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<MedicineResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    fun search(query: String, lat: Double, lng: Double, sortBy: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Error("Enter a medicine name to search")
            return
        }
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            val result = repository.search(query, lat, lng, sortBy)
            _uiState.value = result.fold(
                onSuccess = { SearchUiState.Success(it) },
                onFailure = { SearchUiState.Error(it.message ?: "Search failed") }
            )
        }
    }
}

class SearchViewModelFactory(private val repository: SearchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(repository) as T
    }
}