package com.findurdrugz.android.data.repository

import com.findurdrugz.android.data.api.FindUrDrugzApi
import com.findurdrugz.android.data.local.TokenManager
import com.findurdrugz.android.data.model.LoginRequest
import com.findurdrugz.android.data.model.RegisterRequest
import com.findurdrugz.android.data.model.User
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository(
    private val api: FindUrDrugzApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = api.login(LoginRequest(email, password))
            val data = response.data
            if (response.status == "success" && data != null) {
                tokenManager.saveToken(data.token)
                linkRevenueCatUser(data.user.id) // sync RevenueCat's subscriber ID with our backend's user id
                Result.success(data.user)
            } else {
                Result.failure(Exception(response.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String, phone: String? = null): Result<User> {
        return try {
            val response = api.register(RegisterRequest(name, email, phone, password))
            val data = response.data
            if (response.status == "success" && data != null) {
                tokenManager.saveToken(data.token)
                linkRevenueCatUser(data.user.id)
                Result.success(data.user)
            } else {
                Result.failure(Exception(response.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearToken()
        // Reset RevenueCat back to an anonymous user so the next login on this
        // device doesn't accidentally inherit the previous user's subscriber state.
        Purchases.sharedInstance.logOut(object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {}
            override fun onError(error: PurchasesError) {}
        })
    }

    /**
     * Tells RevenueCat "this device's purchases now belong to backend user <userId>".
     * Uses the same numeric id as the backend's users.id / JWT payload, so that
     * subscriptionController.ts's checkPremiumStatus(userId) — which queries
     * RevenueCat by that same id — finds the correct subscriber.
     *
     * suspendCoroutine wraps RevenueCat's callback-based API as a suspend function
     * so it fits naturally into our coroutine-based repository methods.
     */
    private suspend fun linkRevenueCatUser(userId: Int) {
        suspendCoroutine<Unit> { continuation ->
            Purchases.sharedInstance.logIn(userId.toString(), object : LogInCallback {
                override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo, created: Boolean) {
                    continuation.resume(Unit)
                }
                override fun onError(error: PurchasesError) {
                    // Non-fatal: login/register already succeeded on the backend.
                    // Worst case, premium status check may be briefly inconsistent
                    // until the next successful RevenueCat sync.
                    continuation.resume(Unit)
                }
            })
        }
    }
}