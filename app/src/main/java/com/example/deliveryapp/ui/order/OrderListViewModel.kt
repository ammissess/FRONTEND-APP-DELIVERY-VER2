package com.example.deliveryapp.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.api.OrderSummaryDto
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<Resource<List<OrderSummaryDto>>>(Resource.Loading())
    val ordersState: StateFlow<Resource<List<OrderSummaryDto>>> = _ordersState

    fun loadOrders() {
        viewModelScope.launch {
            _ordersState.value = Resource.Loading()
            _ordersState.value = orderRepository.getOrders()
        }
    }
}