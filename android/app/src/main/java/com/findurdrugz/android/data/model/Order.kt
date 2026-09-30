package com.findurdrugz.android.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Request body for POST /api/orders (delivery order)
@JsonClass(generateAdapter = true)
data class OrderRequest(
    val pharmacyId: Int,
    val medicineId: Int,
    val quantity: Int,
    val deliveryAddress: String,
    val customerPhone: String
)

// Request body for POST /api/reservations (pickup reservation) — no delivery fields needed
@JsonClass(generateAdapter = true)
data class ReservationRequest(
    val pharmacyId: Int,
    val medicineId: Int,
    val quantity: Int
)

/**
 * Shared response shape for both orders and reservations — the backend returns
 * very similar row shapes for both (id, status, quantity, plus joined names),
 * so one model covers both to avoid duplicating near-identical classes.
 */
@JsonClass(generateAdapter = true)
data class OrderOrReservation(
    val id: Int,
    val status: String, // e.g. PENDING, ACCEPTED, CONFIRMED, REJECTED, CANCELLED
    val quantity: Int,
    @Json(name = "pharmacy_name") val pharmacyName: String? = null,
    @Json(name = "medicine_name") val medicineName: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

/**
 * Generic wrapper matching the backend's consistent response envelope:
 * { "status": "success"|"error", "data": ..., "message": "..." }
 * Reused across orders/reservations instead of writing one wrapper class per endpoint.
 */
@JsonClass(generateAdapter = true)
data class GenericApiResponse<T>(
    val status: String,
    val data: T? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SubscriptionStatus(
    val isPremium: Boolean,
    val benefit: String
)