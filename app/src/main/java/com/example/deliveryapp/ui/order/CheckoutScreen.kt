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
import kotlinx.coroutines.delay
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.currentBackStackEntryAsState

private const val TAG = "CheckoutDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val cart = navController.previousBackStackEntry?.savedStateHandle?.get<List<CartItem>>("checkout_cart") ?: emptyList()

    // Lấy savedStateHandle từ CheckoutScreen
   // val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    var paymentMethod by remember { mutableStateOf("unpaid") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    val profileState by viewModel.profileState.collectAsState()
    val confirmState by viewModel.confirmOrderState.collectAsState()
    val deliveryInfo by viewModel.deliveryInfo.collectAsState()

    // ✅ Load profile chỉ 1 lần
    LaunchedEffect(Unit) {
        Log.d(TAG, "Loading profile...")
        viewModel.loadProfile()
    }

//    // ✅ Kiểm tra dữ liệu từ LocationPicker - sử dụng SideEffect
//    SideEffect {
//        val handle = navController.currentBackStackEntry?.savedStateHandle
//        val lat = handle?.get<Double>("selectedLat")
//        val lng = handle?.get<Double>("selectedLng")
//        val address = handle?.get<String>("selectedAddress")
//
//        if (lat != null && lng != null && address != null) {
//            Log.d(TAG, "SideEffect: Received from LocationPicker: lat=$lat, lng=$lng, address=$address")
//            viewModel.updateDeliveryAddress(lat, lng, address)
//
//            handle.remove<Double>("selectedLat")
//            handle.remove<Double>("selectedLng")
//            handle.remove<String>("selectedAddress")
//        }
//    }

    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current

    // Nhận dữ liệu từ LocationPickerScreen khi quay lại
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<Double>("selectedLat")?.observe(lifecycleOwner) { lat ->
            val lng = savedStateHandle.get<Double>("selectedLng")
            val address = savedStateHandle.get<String>("selectedAddress")
            if (lat != null && lng != null && address != null) {
                viewModel.updateDeliveryAddress(lat, lng, address)
                // Xóa key sau khi dùng (tránh bị gọi lại nhiều lần)
                savedStateHandle.remove<Double>("selectedLat")
                savedStateHandle.remove<Double>("selectedLng")
                savedStateHandle.remove<String>("selectedAddress")
            }
        }
    }

    // Xử lý kết quả đặt hàng
    LaunchedEffect(confirmState) {
        if (confirmState is Resource.Success && (confirmState as Resource.Success).data?.isNotEmpty() == true) {
            navController.previousBackStackEntry?.savedStateHandle?.set("clear_cart", true)
            delay(1000)
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Load profile vào EditDialog
    LaunchedEffect(profileState, showEditDialog) {
        if (showEditDialog && profileState is Resource.Success) {
            val profile = (profileState as Resource.Success).data
            editName = profile?.name ?: ""
            editPhone = profile?.phone ?: ""
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chỉnh sửa thông tin người nhận") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Tên người nhận") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateReceiverInfo(editName, editPhone)
                    showEditDialog = false
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Hủy")
                }
            }
        )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
// Thông tin nhận hàng (update hiển thị từ deliveryInfo)
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
                                Row {
                                    IconButton(onClick = { showEditDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa thông tin")
                                    }
                                    IconButton(onClick = { navController.navigate("location_picker") }) {
                                        Icon(Icons.Default.Place, contentDescription = "Chỉnh sửa địa chỉ")
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            Text("Người nhận: ${deliveryInfo.name ?: profile.data?.name ?: ""}")
                            Text("SĐT: ${deliveryInfo.phone ?: profile.data?.phone ?: "Chưa cập nhật"}")

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = when {
                                            deliveryInfo.address != null -> deliveryInfo.address!!
                                            profile.data?.address != null -> "${profile.data.address} (mặc định)"
                                            else -> "⚠️ Chưa chọn địa chỉ giao hàng"
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    // Hiển thị Latitude
                                    Text(
                                        text = if (deliveryInfo.latitude != null) {
                                            "Latitude: ${String.format("%.6f", deliveryInfo.latitude)}"
                                        } else {
                                            "Latitude: Chưa có dữ liệu"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (deliveryInfo.latitude != null)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )

                                    // Hiển thị Longitude
                                    Text(
                                        text = if (deliveryInfo.longitude != null) {
                                            "Longitude: ${String.format("%.6f", deliveryInfo.longitude)}"
                                        } else {
                                            "Longitude: Chưa có dữ liệu"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (deliveryInfo.longitude != null)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // Cảnh báo nếu chưa chọn vị trí
                            if (deliveryInfo.latitude == null || deliveryInfo.longitude == null) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Vui lòng chọn vị trí giao hàng từ bản đồ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Danh sách sản phẩm (giữ nguyên)
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

                    // Phương thức thanh toán (giữ nguyên)
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

                    // Tổng tiền (giữ nguyên)
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

                    // Nút xác nhận (giữ nguyên, nhưng thêm log)
                    Button(
                        onClick = {
                            Log.d(TAG, "Confirm order: lat=${deliveryInfo.latitude}, lng=${deliveryInfo.longitude}, address=${deliveryInfo.address}")
                            viewModel.confirmOrder(
                                cart = cart,
                                paymentMethod = paymentMethod
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = confirmState !is Resource.Loading &&
                                deliveryInfo.latitude != null &&
                                deliveryInfo.longitude != null &&
                                !deliveryInfo.name.isNullOrBlank() &&
                                !deliveryInfo.phone.isNullOrBlank()
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

                            is Resource.Error -> {
                                Text("Thử lại")

                                // Thông báo lỗi
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

                            is Resource.Success -> {
                                Text("Xác nhận đặt hàng")
                            }
                            else -> Text("Xác nhận đặt hàng")
                        }
                    }
                }
            }
        }
    }
}