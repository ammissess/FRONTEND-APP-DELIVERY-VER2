package com.example.deliveryapp.data.repository

import android.util.Log
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.*
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.flow.first
import retrofit2.Response

private const val TAG = "AuthRepository"

class AuthRepository(
    private val api: AuthApi,
    private val dataStore: DataStoreManager
) {
    suspend fun signup(req: SignupRequestDto): Resource<Unit> = safeCall { api.signup(req) }

    suspend fun verifyOtp(req: VerifyOtpRequestDto): Resource<Unit> = safeCall { api.verifyOtp(req) }

    suspend fun login(req: LoginRequestDto): Resource<AuthResponseDto> {
        return try {
            Log.d(TAG, "Attempting login for: ${req.email}")
            val resp = api.login(req)
            if (resp.isSuccessful) {
                resp.body()?.let { authResp ->
                    // Log trước khi lưu
                    Log.d(TAG, "Login successful, saving tokens...")

                    // Lưu token vào DataStore khi login thành công
                    dataStore.saveTokens(authResp.accessToken, authResp.refreshToken)

                    // Kiểm tra xem đã lưu thành công chưa
                    val savedAccess = dataStore.accessToken.first()
                    val savedRefresh = dataStore.refreshToken.first()
                    Log.d(TAG, "Tokens saved - Access: ${savedAccess?.take(10)}..., Refresh: ${savedRefresh?.take(10)}...")

                    Resource.Success(authResp)
                } ?: Resource.Error("Empty response")
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Login failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login exception: ${e.message}", e)
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun forgotPassword(email: String): Resource<Unit> =
        safeCall { api.forgotPassword(ForgotPasswordRequestDto(email)) }

    suspend fun verifyOtpForReset(email: String, otp: String): Resource<ResetTokenDto> =
        safeCall { api.verifyOtpForReset(VerifyOtpRequestDto(email, otp)) }

    suspend fun resetPassword(token: String, newPassword: String): Resource<Unit> =
        safeCall { api.resetPassword(ResetPasswordDto(token, newPassword)) }

    suspend fun logout() {
        Log.d(TAG, "Logging out, clearing tokens")
        dataStore.clearTokens()
    }

    suspend fun getProfile(): Resource<ProfileDto> = safeCall { api.getProfile() }

    //sua thong tin nguoi dung
    suspend fun updateProfile(req: UpdateProfileRequest): Resource<Unit> = safeCall { api.updateProfile(req) }

    private inline fun <T> safeCall(apiCall: () -> Response<T>): Resource<T> {
        return try {
            val resp = apiCall()
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Success(Unit as T)
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "API error: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in API call: ${e.message}", e)
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
    suspend fun refreshToken(refreshToken: String): Resource<AuthResponseDto> {
        return try {
            Log.d(TAG, "Attempting to refresh token")
            val resp = api.refreshAccessToken(RefreshTokenRequestDto(refreshToken))
            if (resp.isSuccessful) {
                resp.body()?.let { authResp ->
                    // Lưu token mới
                    dataStore.saveTokens(authResp.accessToken, authResp.refreshToken)
                    Log.d(TAG, "Token refreshed successfully")
                    Resource.Success(authResp)
                } ?: Resource.Error("Empty response")
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Refresh failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh exception: ${e.message}", e)
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
}