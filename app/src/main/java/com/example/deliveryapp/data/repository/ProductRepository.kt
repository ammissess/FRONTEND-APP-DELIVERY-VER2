package com.example.deliveryapp.data.repository

import com.example.deliveryapp.data.remote.api.ProductApi
import com.example.deliveryapp.data.remote.dto.*
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ProductRepository(private val api: ProductApi) {

    suspend fun getProducts(page: Int = 1, limit: Int = 20): Resource<List<ProductDto>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getProducts(page, limit)
                if (resp.isSuccessful) {
                    Resource.Success(resp.body()?.products ?: emptyList())
                } else {
                    Resource.Error("Error: ${resp.code()}")
                }
            } catch (e: IOException) {
                Resource.Error("Network error")
            } catch (e: HttpException) {
                Resource.Error("Server error")
            }
        }

    suspend fun getProductById(id: Long): Resource<ProductDto> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getProductById(id)
                if (resp.isSuccessful) {
                    resp.body()?.product?.let { Resource.Success(it) }
                        ?: Resource.Error("Empty body")
                } else {
                    Resource.Error("Error: ${resp.code()}")
                }
            } catch (e: Exception) {
                Resource.Error("Error: ${e.message}")
            }
        }

    // ⭐ Tạo review
    suspend fun createReview(request: ReviewRequestDto): Resource<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.createReview(request)
                if (resp.isSuccessful) Resource.Success(Unit)
                else Resource.Error(resp.errorBody()?.string() ?: "Error create review")
            } catch (e: Exception) {
                Resource.Error("Error: ${e.message}")
            }
        }

    // ⭐ Lấy danh sách review
    suspend fun getReviews(productId: Long): Resource<List<ReviewResponseDto>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getReviews(productId)
                if (resp.isSuccessful) {
                    Resource.Success(resp.body()?.reviews ?: emptyList())
                } else {
                    Resource.Error(resp.errorBody()?.string() ?: "Error load reviews")
                }
            } catch (e: Exception) {
                Resource.Error("Error: ${e.message}")
            }
        }
}