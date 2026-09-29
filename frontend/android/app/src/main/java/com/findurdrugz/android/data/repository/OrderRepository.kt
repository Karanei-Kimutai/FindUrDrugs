package com.findurdrugz.android.data.repository

import com.findurdrugz.android.data.api.FindUrDrugzApi
import com.findurdrugz.android.data.model.OrderOrReservation
import com.findurdrugz.android.data.model.OrderRequest
import com.findurdrugz.android.data.model.ReservationRequest

class OrderRepository(private val api: FindUrDrugzApi) {

    suspend fun createOrder(request: OrderRequest): Result<OrderOrReservation> {
        return try {
            val response = api.createOrder(request)
            if (response.status == "success" && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Order failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReservation(request: ReservationRequest): Result<OrderOrReservation> {
        return try {
            val response = api.createReservation(request)
            if (response.status == "success" && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Reservation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetches the customer's past delivery orders
    suspend fun getCustomerOrders(): Result<List<OrderOrReservation>> {
        return try {
            val response = api.getCustomerOrders()
            if (response.status == "success" && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load orders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetches the customer's past pickup reservations
    suspend fun getCustomerReservations(): Result<List<OrderOrReservation>> {
        return try {
            val response = api.getCustomerReservations()
            if (response.status == "success" && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load reservations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}