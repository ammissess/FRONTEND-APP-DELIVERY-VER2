package com.example.deliveryapp.domain.usecase

import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.utils.Resource
import javax.inject.Inject

class GetOrderDetailUseCase @Inject constructor(
    private val repo: OrderRepository
) {
    suspend operator fun invoke(id: Long): Resource<OrderDetailDto> = repo.getOrderDetail(id)
}
