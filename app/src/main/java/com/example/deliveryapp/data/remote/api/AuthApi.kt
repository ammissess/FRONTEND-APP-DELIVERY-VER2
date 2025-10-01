package com.example.deliveryapp.data.remote.api

import com.example.deliveryapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("signup")
    suspend fun signup(@Body req: SignupRequestDto): Response<Unit>

    @POST("verify-otp") // dành cho signup
    suspend fun verifyOtp(@Body req: VerifyOtpRequestDto): Response<Unit>

    @POST("login")
    suspend fun login(@Body req: LoginRequestDto): Response<AuthResponseDto>

    @POST("forgot-password")
    suspend fun forgotPassword(@Body req: ForgotPasswordRequestDto): Response<Unit>

    @POST("verify-otp-for-reset") // dành cho reset password
    suspend fun verifyOtpForReset(@Body req: VerifyOtpRequestDto): Response<ResetTokenDto>

    @POST("reset-password")
    suspend fun resetPassword(@Body req: ResetPasswordDto): Response<Unit>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileDto>
    
    @POST("refresh-access-token")
    suspend fun refreshAccessToken(@Body req: RefreshTokenRequestDto): Response<AuthResponseDto>

    // Thêm vào interface AuthApi
    @PUT("profile")
    suspend fun updateProfile(@Body req: UpdateProfileRequest): Response<Unit>
}