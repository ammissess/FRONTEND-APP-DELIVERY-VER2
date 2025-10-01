package com.example.deliveryapp.data.repository

import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.*
import com.example.deliveryapp.utils.Resource
import retrofit2.Response

class AuthRepository(
    private val api: AuthApi,
    private val dataStore: DataStoreManager
) {
    suspend fun signup(req: SignupRequestDto): Resource<Unit> = safeCall { api.signup(req) }

    suspend fun verifyOtp(req: VerifyOtpRequestDto): Resource<Unit> = safeCall { api.verifyOtp(req) }

    suspend fun login(req: LoginRequestDto): Resource<AuthResponseDto> {
        return try {
            val resp = api.login(req)
            if (resp.isSuccessful) {
                resp.body()?.let { authResp ->
                    // ✅ Lưu token vào DataStore khi login thành công
                    dataStore.saveTokens(authResp.accessToken, authResp.refreshToken)
                    Resource.Success(authResp)
                } ?: Resource.Error("Empty response")
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "Unknown error"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun forgotPassword(email: String): Resource<Unit> =
        safeCall { api.forgotPassword(ForgotPasswordRequestDto(email)) }

    suspend fun verifyOtpForReset(email: String, otp: String): Resource<ResetTokenDto> =
        safeCall { api.verifyOtpForReset(VerifyOtpRequestDto(email, otp)) }

    suspend fun resetPassword(token: String, newPassword: String): Resource<Unit> =
        safeCall { api.resetPassword(ResetPasswordDto(token, newPassword)) }

    suspend fun logout() = dataStore.clearTokens()

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
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
}