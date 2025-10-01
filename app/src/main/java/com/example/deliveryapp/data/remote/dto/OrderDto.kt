package com.example.deliveryapp.data.remote.dto

data class PlaceOrderRequestDto(
    val latitude: Double,
    val longitude: Double,
    val products: List<OrderProductDto>
)

data class OrderProductDto(val product_id: Long, val quantity: Long)

data class OrderDetailDto(
    val order: OrderDto,
    val items: List<OrderItemDto>
)

data class OrderDto(
    val id: Long,
    val user_id: Long,
    val order_status: String,
    val payment_status: String,
    val latitude: Double,
    val longitude: Double,
    val total_amount: Double,
    val thumbnail_id: Long?,
    val created_at: String?
)

data class OrderItemDto(
    val product_id: Long,
    val product_name: String,
    val product_image: String?,
    val quantity: Long,
    val price: Double,
    val subtotal: Double
)