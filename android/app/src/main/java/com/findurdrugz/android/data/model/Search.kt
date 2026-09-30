package com.findurdrugz.android.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val status: String,
    val results: Int,
    val data: List<MedicineResult>
)

@JsonClass(generateAdapter = true)
data class MedicineResult(
    @Json(name = "pharmacy_id") val pharmacyId: Int,
    @Json(name = "pharmacy_name") val pharmacyName: String,
    val address: String,
    val verified: Boolean,
    @Json(name = "delivery_available") val deliveryAvailable: Boolean,
    @Json(name = "delivery_fee") val deliveryFee: String,
    @Json(name = "medicine_id") val medicineId: Int,
    @Json(name = "medicine_name") val medicineName: String,
    val strength: String,
    val price: String,
    val quantity: Int,
    @Json(name = "last_updated") val lastUpdated: String,
    val distance: Double
)