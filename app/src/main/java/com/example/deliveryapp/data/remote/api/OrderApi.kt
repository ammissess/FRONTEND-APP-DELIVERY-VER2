package com.example.deliveryapp.data.remote.api

import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OrderApi {
    @POST("create-order")
    suspend fun placeOrder(@Body req: PlaceOrderRequestDto): Response<PlaceOrderResponse>

    @GET("orders")
    suspend fun getOrders(): Response<OrdersListResponse>

    @GET("orders/{id}")
    suspend fun getOrderDetail(@Path("id") id: Long): Response<OrderDetailDto>
}

data class PlaceOrderResponse(val message: String)
data class OrdersListResponse(val orders: List<OrderSummaryDto>)
data class OrderSummaryDto(
    val id: Long,
    val order_status: String,
    val total_amount: Double,
    val thumbnail: String?
)