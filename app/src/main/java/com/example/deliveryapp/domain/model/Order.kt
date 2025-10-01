package com.example.deliveryapp.domain.model

data class Order(
    val id: Long,
    val orderStatus: String,
    val totalAmount: Double,
    val latitude: Double,
    val longitude: Double,
    val items: List<OrderItem> = emptyList()
)

data class OrderItem(
    val productId: Long,
    val productName: String,
    val productImage: String?,
    val quantity: Long,
    val price: Double,
    val subtotal: Double
)