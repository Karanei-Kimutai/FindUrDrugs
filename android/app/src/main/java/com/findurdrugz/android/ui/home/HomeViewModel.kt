package com.findurdrugz.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.findurdrugz.android.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Checks the user's premium status when Home loads, so the "Go Premium" button
 * can be swapped for a premium badge if they already have an active subscription.
 * isPremium defaults to false rather than null, so the UI never has to handle
 * a third "unknown" state — worst case it briefly shows "Go Premium" before the
 * real status loads.
 */
class HomeViewModel(private val repository: SubscriptionRepository) : ViewModel() {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    fun checkPremiumStatus() {
        viewModelScope.launch {
            val result = repository.checkServerVerifiedStatus()
            _isPremium.value = result.getOrNull() ?: false
        }
    }
}

class HomeViewModelFactory(private val repository: SubscriptionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}