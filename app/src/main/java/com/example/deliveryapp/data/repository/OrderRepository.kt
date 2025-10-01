package com.example.deliveryapp.data.repository

import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.api.OrderApi
import com.example.deliveryapp.data.remote.api.OrderSummaryDto
import com.example.deliveryapp.data.remote.dto.AuthResponseDto
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.utils.Constants
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class OrderRepository(
    private val orderApi: OrderApi,
    private val authApi: AuthApi  // ← THÊM: Để gọi refresh
) {
    suspend fun placeOrder(req: PlaceOrderRequestDto): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val resp = orderApi.placeOrder(req)
            if (resp.isSuccessful) {
                Resource.Success(resp.body()?.message ?: "Order placed")
            } else {
                Resource.Error("Error: ${resp.code()}")
            }
        } catch (e: IOException) {
            Resource.Error("Network error")
        } catch (e: HttpException) {
            Resource.Error("Server error")
        }
    }

    suspend fun getOrders(): Resource<List<OrderSummaryDto>> = withContext(Dispatchers.IO) {
        try {
            val resp = orderApi.getOrders()
            if (resp.isSuccessful) {
                Resource.Success(resp.body()?.orders ?: emptyList())
            } else {
                Resource.Error("Error: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }

    suspend fun getOrderDetail(id: Long): Resource<OrderDetailDto> = withContext(Dispatchers.IO) {
        try {
            val resp = orderApi.getOrderDetail(id)
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Error("Empty body")
            } else {
                Resource.Error("Error: ${resp.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }

    // ✅ SỬA: Gọi refresh trước, lấy access token mới, rồi dùng cho create-order
    suspend fun placeOrderWithRefreshToken(req: PlaceOrderRequestDto, refreshToken: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            // Bước 1: Gọi refresh để lấy access token mới
            val refreshReq = RefreshTokenRequestDto(refreshToken = refreshToken)
            val refreshResp = authApi.refreshAccessToken(refreshReq)
            if (!refreshResp.isSuccessful) {
                return@withContext Resource.Error("Refresh token failed: ${refreshResp.code()}")
            }
            val newTokens = refreshResp.body() ?: return@withContext Resource.Error("No new tokens")

            // Bước 2: Tạo client với access token mới (Bearer)
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                    chain.proceed(request)
                }
                .build()

            // Bước 3: Tạo retrofit mới với client
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val customOrderApi = retrofit.create(OrderApi::class.java)
            val resp = customOrderApi.placeOrder(req)

            if (resp.isSuccessful) {
                Resource.Success(resp.body()?.message ?: "Đặt hàng thành công")
            } else {
                Resource.Error("Error: ${resp.code()}")
            }
        } catch (e: IOException) {
            Resource.Error("Lỗi mạng")
        } catch (e: HttpException) {
            Resource.Error("Lỗi server")
        } catch (e: Exception) {
            Resource.Error("Lỗi không xác định: ${e.message}")
        }
    }
}