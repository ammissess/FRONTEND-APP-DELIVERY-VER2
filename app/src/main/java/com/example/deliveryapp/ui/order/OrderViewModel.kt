package com.example.deliveryapp.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.domain.usecase.GetOrderDetailUseCase
import com.example.deliveryapp.domain.usecase.PlaceOrderUseCase
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val getOrderDetail: GetOrderDetailUseCase
) : ViewModel() {

    private val _placeOrderResult = MutableStateFlow<Resource<String>>(Resource.Loading())
    val placeOrderResult: StateFlow<Resource<String>> = _placeOrderResult

    private val _orderDetail = MutableStateFlow<Resource<OrderDetailDto>>(Resource.Loading())
    val orderDetail: StateFlow<Resource<OrderDetailDto>> = _orderDetail

    fun placeOrder(req: PlaceOrderRequestDto) {
        viewModelScope.launch {
            _placeOrderResult.value = placeOrderUseCase(req) //  gọi đúng UseCase
        }
    }

    fun loadOrderDetail(id: Long) {
        viewModelScope.launch {
            _orderDetail.value = getOrderDetail(id)
        }
    }
}
