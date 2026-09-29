package com.findurdrugz.android.data.repository

import com.findurdrugz.android.data.api.FindUrDrugzApi
import com.findurdrugz.android.data.local.TokenManager
import com.findurdrugz.android.data.model.LoginRequest
import com.findurdrugz.android.data.model.RegisterRequest
import com.findurdrugz.android.data.model.User

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
    }

}