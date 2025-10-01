// Product.kt (updated)
package com.example.deliveryapp.domain.model

data class Product(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Double,
    val createdAt: String?,  // Changed to String? (parse to Date if needed)
    val images: List<String> = emptyList()  // Extract URLs from ProductImageDto in mapper if needed
)