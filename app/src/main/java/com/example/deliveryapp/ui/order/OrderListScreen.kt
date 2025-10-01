package com.example.deliveryapp.ui.order

import androidx.compose.foundation.clickable
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
import com.example.deliveryapp.data.remote.api.OrderSummaryDto
import com.example.deliveryapp.ui.home.formatPrice
import com.example.deliveryapp.utils.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    navController: NavController,
    viewModel: OrderListViewModel = hiltViewModel()
) {
    val ordersState by viewModel.ordersState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đơn hàng của tôi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = ordersState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lỗi: ${state.message}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadOrders() }) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            is Resource.Success -> {
                val orders = state.data ?: emptyList()
                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có đơn hàng nào")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders, key = { it.id }) { order ->
                            OrderItemCard(order) {
                                navController.navigate("order_detail/${order.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: OrderSummaryDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = order.thumbnail,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Đơn hàng #${order.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))

                // Status badge
                val statusColor = when (order.order_status) {
                    "pending" -> MaterialTheme.colorScheme.secondary
                    "processing" -> MaterialTheme.colorScheme.tertiary
                    "shipped" -> MaterialTheme.colorScheme.primary
                    "delivered" -> MaterialTheme.colorScheme.primary
                    "cancelled" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }

                val statusText = when (order.order_status) {
                    "pending" -> "Chờ xử lý"
                    "processing" -> "Đang chuẩn bị"
                    "shipped" -> "Đang giao"
                    "delivered" -> "Đã giao"
                    "cancelled" -> "Đã hủy"
                    else -> order.order_status
                }

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = formatPrice(order.total_amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}