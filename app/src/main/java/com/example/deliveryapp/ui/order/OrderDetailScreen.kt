package com.example.deliveryapp.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.ui.home.formatPrice
import com.example.deliveryapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    navController: NavController,
    orderId: Long,
    viewModel: OrderViewModel = hiltViewModel()
) {
    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    val orderDetail by viewModel.orderDetail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết đơn hàng #$orderId") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = orderDetail) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("Lỗi: ${state.message}")
                }
            }
            is Resource.Success -> {
                val detail = state.data
                if (detail == null) return@Scaffold

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trạng thái đơn hàng
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Trạng thái", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("Đơn hàng: ${detail.order.order_status}")
                                Text("Thanh toán: ${detail.order.payment_status}")
                            }
                        }
                    }

                    // Danh sách sản phẩm
                    item {
                        Text("Sản phẩm", style = MaterialTheme.typography.titleMedium)
                    }

                    items(detail.items) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.product_image,
                                    contentDescription = item.product_name,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.product_name)
                                    Text("x${item.quantity}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatPrice(item.subtotal), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Tổng tiền
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tổng cộng:", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    formatPrice(detail.order.total_amount),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}