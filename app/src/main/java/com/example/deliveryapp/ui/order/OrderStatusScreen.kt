package com.example.deliveryapp.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.ui.map.MapScreen
import com.example.deliveryapp.utils.Resource

@Composable
fun OrderStatusScreen(orderId: Long, viewModel: OrderViewModel = hiltViewModel()) {
    LaunchedEffect(orderId) { viewModel.loadOrderDetail(orderId) }
    val state by viewModel.orderDetail.collectAsState()

    when (state) {
        is Resource.Loading -> CircularProgressIndicator()
        is Resource.Error -> Text((state as Resource.Error).message ?: "Error")
        is Resource.Success -> {
            val dto = (state as Resource.Success<OrderDetailDto>).data!!
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Order #${dto.order.id} - ${dto.order.order_status}", modifier = Modifier.padding(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                MapScreen(
                    userLat = dto.order.latitude,
                    userLng = dto.order.longitude,
                    driverLat = dto.order.latitude, // Update with real driver loc
                    driverLng = dto.order.longitude
                )
            }
        }
    }
}