package com.example.deliveryapp.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.data.remote.dto.OrderDetailDto
import com.example.deliveryapp.ui.map.MapScreen
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.launch

@Composable
fun OrderStatusScreen(
    orderId: Long,
    navController: NavController,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val state by viewModel.orderDetail.collectAsState()
    val isChatEnabled by viewModel.isChatEnabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()


    // Khi vào màn hình: tải chi tiết đơn hàng
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    LaunchedEffect(orderId) {
        viewModel.pollStatus(orderId) { status, shipperId ->
            when (status) {
                "received" -> {
                    launch {
                        val token = viewModel.getToken()  // ✅ được phép gọi suspend ở đây
                        viewModel.openChat(orderId, shipperId, "Shipper", token)
                    }
                }

                "completed" -> {
                    viewModel.onOrderCompleted()
                }
            }
        }
    }


    when (state) {
        is Resource.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is Resource.Error -> {
            Text((state as Resource.Error).message ?: "Lỗi khi tải đơn hàng")
        }

        is Resource.Success -> {
            val dto = (state as Resource.Success<OrderDetailDto>).data!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text("Đơn hàng #${dto.order.id} - ${dto.order.order_status}")
                Spacer(modifier = Modifier.height(8.dp))

                MapScreen(
                    userLat = dto.order.latitude,
                    userLng = dto.order.longitude,
                    driverLat = dto.order.latitude, // TODO: cập nhật vị trí shipper thực
                    driverLng = dto.order.longitude
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nếu chat được bật, hiển thị nút Chat
                if (isChatEnabled) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val shipperId = dto.order.shipper_id ?: return@launch
                                navController.navigate("chat/${dto.order.id}/$shipperId/Shipper")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chat với Shipper")
                    }
                }
            }
        }
    }
}
