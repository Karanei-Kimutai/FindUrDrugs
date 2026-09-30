package com.findurdrugz.android.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.findurdrugz.android.data.model.OrderOrReservation
import com.findurdrugz.android.data.model.OrderRequest
import com.findurdrugz.android.data.model.ReservationRequest
import com.findurdrugz.android.data.repository.OrderRepository
import com.findurdrugz.android.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OrderUiState {
    object Idle : OrderUiState()
    object Loading : OrderUiState()
    data class Success(val result: OrderOrReservation) : OrderUiState()
    data class Error(val message: String) : OrderUiState()
}

class OrderViewModel(
    private val repository: OrderRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderUiState>(OrderUiState.Idle)
    val uiState: StateFlow<OrderUiState> = _uiState

    // Defaults to false; only flips to true once the server confirms premium status.
    // This matches the backend's own approach — the discount is never assumed
    // client-side, only reflected here as a preview once verified.
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    fun checkPremiumStatus() {
        viewModelScope.launch {
            val result = subscriptionRepository.checkServerVerifiedStatus()
            _isPremium.value = result.getOrNull() ?: false
        }
    }

    fun placeOrder(pharmacyId: Int, medicineId: Int, quantity: Int, address: String, phone: String) {
        _uiState.value = OrderUiState.Loading
        viewModelScope.launch {
            val result = repository.createOrder(
                OrderRequest(pharmacyId, medicineId, quantity, address, phone)
            )
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

class OrderViewModelFactory(
    private val repository: OrderRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return OrderViewModel(repository, subscriptionRepository) as T
    }
}