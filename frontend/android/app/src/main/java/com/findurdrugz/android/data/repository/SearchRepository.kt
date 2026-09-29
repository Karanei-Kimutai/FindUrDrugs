package com.findurdrugz.android.data.repository

import com.findurdrugz.android.data.api.FindUrDrugzApi
import com.findurdrugz.android.data.model.MedicineResult

class SearchRepository(private val api: FindUrDrugzApi) {
    suspend fun search(query: String, lat: Double, lng: Double, sortBy: String): Result<List<MedicineResult>> {
        return try {
            val response = api.search(query, lat, lng, sortBy)
            if (response.status == "success") {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}