package com.example.deliveryapp.data.remote.interceptor

import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.di.RawAuthApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val dataStore: DataStoreManager,
    @RawAuthApi private val authApi: AuthApi    // ✅ RawAuthApi chỉ dùng refresh
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
            response.close()
            val refreshToken = runBlocking { dataStore.refreshToken.first() }

            if (!refreshToken.isNullOrBlank()) {
                val refreshResponse = runBlocking {
                    try {
                        authApi.refreshAccessToken(RefreshTokenRequestDto(refreshToken))  // ✅ Giờ chỉ 1 arg OK
                    } catch (e: Exception) {
                        null
                    }
                }

                if (refreshResponse != null && refreshResponse.isSuccessful) {
                    val body = refreshResponse.body()
                    if (body != null) {
                        runBlocking {
                            dataStore.saveTokens(body.accessToken, body.refreshToken)  // ✅ SỬA: Dùng tên Kotlin (accessToken, refreshToken)
                        }

                        // Retry request với access_token mới
                        val newRequest = request.newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", "Bearer ${body.accessToken}")  // ✅ SỬA: accessToken
                            .build()

                        response = chain.proceed(newRequest)
                    }
                }
            }
        }

        return response
    }
}