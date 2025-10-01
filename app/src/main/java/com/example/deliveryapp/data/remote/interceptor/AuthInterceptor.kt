package com.example.deliveryapp.data.remote.interceptor

import android.util.Log
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.di.RawAuthApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

private const val TAG = "AuthInterceptor"

class AuthInterceptor @Inject constructor(
    private val dataStore: DataStoreManager,
    @RawAuthApi private val authApi: AuthApi // RawAuthApi chỉ dùng refresh
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val token = runBlocking { dataStore.accessToken.first() }

        if (!token.isNullOrBlank()) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }

        var response = chain.proceed(request)

        // Nếu access_token hết hạn → 401, ta gọi refresh
        if (response.code == 401) {
            // QUAN TRỌNG: Lưu lại body trước khi đóng response
            val responseBody = response.peekBody(Long.MAX_VALUE).string()
            response.close() // Đóng response cũ an toàn

            val refreshToken = runBlocking { dataStore.refreshToken.first() }
            Log.d(TAG, "Got 401, attempting refresh with token: ${refreshToken?.take(10)}...")

            if (!refreshToken.isNullOrBlank()) {
                try {
                    val refreshResponse = runBlocking {
                        authApi.refreshAccessToken(RefreshTokenRequestDto(refreshToken))
                    }

                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body()
                        if (body != null) {
                            Log.d(TAG, "Refresh successful, saving new tokens")
                            runBlocking {
                                dataStore.saveTokens(body.accessToken, body.refreshToken)
                            }

                            // Retry request với access_token mới
                            val newRequest = request.newBuilder()
                                .removeHeader("Authorization")
                                .addHeader("Authorization", "Bearer ${body.accessToken}")
                                .build()

                            return chain.proceed(newRequest)
                        }
                    } else {
                        Log.e(TAG, "Refresh failed: ${refreshResponse.code()}")
                        // Nếu refresh thất bại, trả về response 401 mới thay vì dùng cái cũ đã đóng
                        return Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(401)
                            .message("Unauthorized")
                            .body(responseBody.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during refresh: ${e.message}", e)
                    // Trả về response lỗi mới nếu có exception
                    return Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized")
                        .body("{\"error\":\"Session expired\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
            } else {
                Log.d(TAG, "No refresh token available")
            }

            // Nếu không có refresh token hoặc refresh thất bại, trả về response 401 mới
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(responseBody.toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        return response
    }
}