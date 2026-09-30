package com.findurdrugz.android.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val status: String,
    val data: AuthData?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class AuthData(
    val user: User,
    val token: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    val role: String = "CUSTOMER"
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)