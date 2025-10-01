package com.example.deliveryapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    @SerializedName("access_token")
    val accessToken: String,  // ← SỬA: Thêm @SerializedName để map snake_case từ backend
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class LoginRequestDto(val email: String, val password: String)
data class SignupRequestDto(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val address: String
)

// Verify OTP request (đăng ký/email verify)
data class VerifyOtpRequestDto(
    val email: String,
    val otp: String
)

// Forgot password request
data class ForgotPasswordRequestDto(
    val email: String
)

// Reset password request
data class ResetPasswordDto(
    val token: String,
    @SerializedName("new_password")
    val newPassword: String  // ← Thêm @SerializedName nếu backend dùng snake_case
)

// Reset password response (server trả về token)
data class ResetTokenDto(
    @SerializedName("reset_token")
    val resetToken: String,
    @SerializedName("expires_in")
    val expiresIn: Int
)

// ✅ SỬA: Chỉ 1 field, dùng @SerializedName để map JSON backend
data class RefreshTokenRequestDto(
    @SerializedName("refresh_token")
    val refreshToken: String  // ← Giữ tên Kotlin camelCase, map sang snake_case JSON
)