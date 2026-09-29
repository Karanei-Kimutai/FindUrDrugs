package com.findurdrugz.android.data.api

import com.findurdrugz.android.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FindUrDrugzApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("api/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("sortBy") sortBy: String = "nearest"
    ): SearchResponse

    // --- Orders ---
    @POST("api/orders")
    suspend fun createOrder(@Body request: OrderRequest): GenericApiResponse<OrderOrReservation>

    @GET("api/orders/customer")
    suspend fun getCustomerOrders(): GenericApiResponse<List<OrderOrReservation>>

    // --- Reservations ---
    @POST("api/reservations")
    suspend fun createReservation(@Body request: ReservationRequest): GenericApiResponse<OrderOrReservation>

    @GET("api/reservations/customer")
    suspend fun getCustomerReservations(): GenericApiResponse<List<OrderOrReservation>>
}