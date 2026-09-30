package com.findurdrugz.android.data.repository

import com.findurdrugz.android.data.api.FindUrDrugzApi
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Wraps RevenueCat's callback-based SDK as suspend functions, and wraps our
 * own backend's /api/subscription/status as a normal repository call —
 * keeping both consistent with the rest of the app's Result<T> pattern.
 */
class SubscriptionRepository(private val api: FindUrDrugzApi) {

    // Fetches available subscription offerings/packages from RevenueCat
    suspend fun getOfferings(): Result<Offerings> {
        return try {
            suspendCoroutine { continuation ->
                Purchases.sharedInstance.getOfferingsWith(
                    onError = { error -> continuation.resumeWithException(Exception(error.message)) },
                    onSuccess = { offerings -> continuation.resume(offerings) }
                )
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Asks our OWN backend to verify premium status server-side — this is the
    // "don't trust the client" check per subscriptionController.ts.
    suspend fun checkServerVerifiedStatus(): Result<Boolean> {
        return try {
            val response = api.getSubscriptionStatus()
            if (response.status == "success" && response.data != null) {
                Result.success(response.data.isPremium)
            } else {
                Result.failure(Exception(response.message ?: "Failed to verify subscription"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}