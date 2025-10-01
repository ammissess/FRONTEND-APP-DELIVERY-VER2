package com.example.deliveryapp.ui.order

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.deliveryapp.ui.home.CartItem
import com.example.deliveryapp.ui.home.formatPrice
import com.example.deliveryapp.utils.Resource

private const val TAG = "CheckoutDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    // ✅ Lấy cart từ navigation args
    val cart = navController.previousBackStackEntry?.savedStateHandle?.get<List<CartItem>>("checkout_cart") ?: emptyList()

    var paymentMethod by remember { mutableStateOf("unpaid") }

    val profileState by viewModel.profileState.collectAsState()
    val confirmState by viewModel.confirmOrderState.collectAsState()
    val deliveryInfo by viewModel.deliveryInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // ✅ Xử lý kết quả đặt hàng
    LaunchedEffect(confirmState) {
        if (confirmState is Resource.Success && (confirmState as Resource.Success).data?.isNotEmpty() == true) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // ✅ SỬA: Lắng nghe địa chỉ mới từ LocationPicker - Trigger khi nav entry thay đổi (popBack)
    LaunchedEffect(navController) {  // Key là navController để trigger khi pop
        navController.currentBackStackEntry?.savedStateHandle?.let { handle ->
            val lat = handle.get<Double>("selectedLat")
            val lng = handle.get<Double>("selectedLng")
            val address = handle.get<String>("selectedAddress")

            if (lat != null && lng != null && address != null) {
                Log.d(TAG, "Received from LocationPicker: lat=$lat, lng=$lng, address=$address")
                viewModel.updateDeliveryAddress(lat, lng, address)
                // Clear sau khi đọc
                handle.remove<Double>("selectedLat")
                handle.remove<Double>("selectedLng")
                handle.remove<String>("selectedAddress")
            }
        }
    }

// ✅ Fallback: Nếu chưa có lat/lng, dùng default Hà Nội (profile không có tọa độ)
    LaunchedEffect(profileState) {
        if (profileState is Resource.Success && deliveryInfo.latitude == null) {
            val defaultLat = 21.028511  // Hà Nội
            val defaultLng = 105.804817
            val defaultAddress = (profileState.data?.address ?: "Địa chỉ mặc định tại Hà Nội")
            viewModel.updateDeliveryAddress(defaultLat, defaultLng, defaultAddress)
            Log.d(TAG, "Fallback to default: lat=$defaultLat, lng=$defaultLng")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xác nhận đơn hàng") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val profile = profileState) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("Lỗi: ${profile.message}")
                }
            }
            is Resource.Success -> {
                val user = profile.data
                if (user == null) return@Scaffold

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ✅ Thông tin nhận hàng với tọa độ
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Thông tin nhận hàng", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    navController.navigate("location_picker")
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa")
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            Text("Người nhận: ${user.name}")
                            Text("SĐT: ${user.phone ?: "Chưa cập nhật"}")

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text(deliveryInfo.address ?: user.address ?: "⚠️ Chưa chọn địa chỉ giao hàng")

                                    // ✅ Hiển thị tọa độ để debug
                                    if (deliveryInfo.latitude != null && deliveryInfo.longitude != null) {
                                        Text(
                                            "📍 (${deliveryInfo.latitude}, ${deliveryInfo.longitude})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            "⚠️ Chưa có tọa độ (kiểm tra log)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Danh sách sản phẩm
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Sản phẩm đã chọn", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))

                            cart.forEach { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.product.images.firstOrNull()?.url,
                                        contentDescription = item.product.name,
                                        modifier = Modifier.size(60.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.product.name, style = MaterialTheme.typography.bodyLarge)
                                        Text("x${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        formatPrice(item.product.price * item.quantity),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (cart.last() != item) Divider()
                            }
                        }
                    }

                    // Phương thức thanh toán
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Phương thức thanh toán", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = paymentMethod == "unpaid",
                                    onClick = { paymentMethod = "unpaid" }
                                )
                                Text("Thanh toán khi nhận hàng")
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = paymentMethod == "paid",
                                    onClick = { paymentMethod = "paid" }
                                )
                                Text("Chuyển khoản")
                            }
                        }
                    }

                    // Tổng tiền
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng cộng:", style = MaterialTheme.typography.titleLarge)
                            Text(
                                formatPrice(cart.sumOf { it.product.price * it.quantity }),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // ✅ Nút xác nhận với validation
                    Button(
                        onClick = {
                            Log.d(TAG, "Confirm order: lat=${deliveryInfo.latitude}, lng=${deliveryInfo.longitude}")
                            viewModel.confirmOrder(
                                cart = cart,
                                paymentMethod = paymentMethod
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = confirmState !is Resource.Loading &&
                                deliveryInfo.latitude != null &&
                                deliveryInfo.longitude != null  // Validation giữ nguyên
                    ) {
                        when (confirmState) {
                            is Resource.Loading -> {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Đang xử lý...")
                                }
                            }
                            else -> Text("Xác nhận đặt hàng")
                        }
                    }

                    // Thông báo lỗi
                    if (confirmState is Resource.Error) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = (confirmState as Resource.Error).message ?: "Lỗi đặt hàng",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}