package com.findurdrugz.android.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.findurdrugz.android.data.repository.SubscriptionRepository
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PremiumUiState {
    object Loading : PremiumUiState()
    data class OfferingsLoaded(val packages: List<Package>) : PremiumUiState()
    data class AlreadyPremium(val benefit: String) : PremiumUiState()
    data class Error(val message: String) : PremiumUiState()
    object PurchaseInProgress : PremiumUiState()
    data class PurchaseSuccess(val benefit: String) : PremiumUiState()
}

class PremiumViewModel(private val repository: SubscriptionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<PremiumUiState>(PremiumUiState.Loading)
    val uiState: StateFlow<PremiumUiState> = _uiState

    fun loadPremiumScreen() {
        _uiState.value = PremiumUiState.Loading
        viewModelScope.launch {
            // First check our own backend — if already premium, skip straight to that state
            val statusResult = repository.checkServerVerifiedStatus()
            val isPremium = statusResult.getOrNull() ?: false

            if (isPremium) {
                _uiState.value = PremiumUiState.AlreadyPremium("50% off delivery fees")
                return@launch
            }

            // Not premium yet — load purchasable offerings
            val offeringsResult = repository.getOfferings()
            offeringsResult.fold(
                onSuccess = { offerings ->
                    val current = offerings.current
                    if (current != null && current.availablePackages.isNotEmpty()) {
                        _uiState.value = PremiumUiState.OfferingsLoaded(current.availablePackages)
                    } else {
                        _uiState.value = PremiumUiState.Error("No subscription plans available right now.")
                    }
                },
                onFailure = { _uiState.value = PremiumUiState.Error(it.message ?: "Failed to load plans") }
            )
        }
    }

    // Called after PurchasesActivityResultLauncher (see PremiumScreen.kt) confirms
    // a purchase succeeded at the Play Billing level. We still verify with our
    // OWN backend afterward — that's the actual source of truth for premium status.
    fun onPurchaseCompleted() {
        _uiState.value = PremiumUiState.PurchaseInProgress
        viewModelScope.launch {
            val statusResult = repository.checkServerVerifiedStatus()
            statusResult.fold(
                onSuccess = { isPremium ->
                    if (isPremium) {
                        _uiState.value = PremiumUiState.PurchaseSuccess("50% off delivery fees")
                    } else {
                        // Purchase succeeded on Play's side but backend hasn't confirmed yet —
                        // RevenueCat webhooks to the backend can take a few seconds.
                        _uiState.value = PremiumUiState.Error(
                            "Purchase received — confirming with server, please check back shortly."
                        )
                    }
                },
                onFailure = { _uiState.value = PremiumUiState.Error(it.message ?: "Verification failed") }
            )
        }
    }

    fun onPurchaseFailed(message: String) {
        _uiState.value = PremiumUiState.Error(message)
    }
}

class PremiumViewModelFactory(private val repository: SubscriptionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PremiumViewModel(repository) as T
    }
}