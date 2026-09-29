package com.findurdrugz.android.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.findurdrugz.android.data.model.OrderOrReservation
import com.findurdrugz.android.data.model.OrderRequest
import com.findurdrugz.android.data.model.ReservationRequest
import com.findurdrugz.android.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Represents every state OrderScreen's UI can be in.
// Using a sealed class means the `when` block in the UI must handle every case,
// so we can't accidentally forget to show an error or a loading spinner.
sealed class OrderUiState {
    object Idle : OrderUiState()
    object Loading : OrderUiState()
    data class Success(val result: OrderOrReservation) : OrderUiState()
    data class Error(val message: String) : OrderUiState()
}

/**
 * Holds order/reservation submission state and survives configuration changes
 * (e.g. screen rotation) since it's a ViewModel, not plain Compose state.
 */
class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    // Private mutable state, exposed publicly as read-only StateFlow —
    // standard pattern so only the ViewModel itself can change the state.
    private val _uiState = MutableStateFlow<OrderUiState>(OrderUiState.Idle)
    val uiState: StateFlow<OrderUiState> = _uiState

    fun placeOrder(pharmacyId: Int, medicineId: Int, quantity: Int, address: String, phone: String) {
        _uiState.value = OrderUiState.Loading
        // viewModelScope ties this coroutine's lifetime to the ViewModel —
        // it's automatically cancelled if the ViewModel is cleared, preventing leaks.
        viewModelScope.launch {
            val result = repository.createOrder(
                OrderRequest(pharmacyId, medicineId, quantity, address, phone)
            )
            // Converts the Result<T> from the repository into UI state
            _uiState.value = result.fold(
                onSuccess = { OrderUiState.Success(it) },
                onFailure = { OrderUiState.Error(it.message ?: "Order failed") }
            )
        }
    }

    fun placeReservation(pharmacyId: Int, medicineId: Int, quantity: Int) {
        _uiState.value = OrderUiState.Loading
        viewModelScope.launch {
            val result = repository.createReservation(
                ReservationRequest(pharmacyId, medicineId, quantity)
            )
            _uiState.value = result.fold(
                onSuccess = { OrderUiState.Success(it) },
                onFailure = { OrderUiState.Error(it.message ?: "Reservation failed") }
            )
        }
    }
}

// ViewModel needs a constructor argument (repository), so it can't be created
// with the default no-arg viewModel() call — this factory tells Compose how to build it.
class OrderViewModelFactory(private val repository: OrderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return OrderViewModel(repository) as T
    }
}