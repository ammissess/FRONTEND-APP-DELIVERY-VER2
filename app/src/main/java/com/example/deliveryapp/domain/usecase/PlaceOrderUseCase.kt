package com.example.deliveryapp.domain.usecase

import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.utils.Resource
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(
    private val repo: OrderRepository
) {
    suspend operator fun invoke(req: PlaceOrderRequestDto): Resource<String> {
        return repo.placeOrder(req) // đã là Resource<String>
    }
}
