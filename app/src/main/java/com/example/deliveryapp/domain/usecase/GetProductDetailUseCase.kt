package com.example.deliveryapp.domain.usecase

import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.data.repository.ProductRepository
import com.example.deliveryapp.utils.Resource
import javax.inject.Inject

class GetProductDetailUseCase @Inject constructor(
    private val repo: ProductRepository
) {
    suspend operator fun invoke(id: Long): Resource<ProductDto> = repo.getProductById(id)
}