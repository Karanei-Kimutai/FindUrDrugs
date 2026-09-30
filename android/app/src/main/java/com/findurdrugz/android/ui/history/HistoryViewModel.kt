package com.findurdrugz.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.findurdrugz.android.data.model.OrderOrReservation
import com.findurdrugz.android.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    object Idle : HistoryUiState()
    object Loading : HistoryUiState()
    data class Success(val items: List<OrderOrReservation>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class HistoryViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _ordersState = MutableStateFlow<HistoryUiState>(HistoryUiState.Idle)
    val ordersState: StateFlow<HistoryUiState> = _ordersState

    private val _reservationsState = MutableStateFlow<HistoryUiState>(HistoryUiState.Idle)
    val reservationsState: StateFlow<HistoryUiState> = _reservationsState

    fun loadOrders() {
        _ordersState.value = HistoryUiState.Loading
        viewModelScope.launch {
            val result = repository.getCustomerOrders()
            _ordersState.value = result.fold(
                onSuccess = { HistoryUiState.Success(it) },
                onFailure = { HistoryUiState.Error(it.message ?: "Failed to load orders") }
            )
        }
    }

    fun loadReservations() {
        _reservationsState.value = HistoryUiState.Loading
        viewModelScope.launch {
            val result = repository.getCustomerReservations()
            _reservationsState.value = result.fold(
                onSuccess = { HistoryUiState.Success(it) },
                onFailure = { HistoryUiState.Error(it.message ?: "Failed to load reservations") }
            )
        }
    }
}

class HistoryViewModelFactory(private val repository: OrderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(repository) as T
    }
}