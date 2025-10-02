package com.example.deliveryapp.ui.profile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.delay

private const val TAG = "CustomProfile"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProfile(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.resetUpdateState()
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    // SỬA: Lắng nghe kết quả từ LocationPicker (dùng previousBackStackEntry để nhận từ child khi popBackStack)
    LaunchedEffect(navController) {
        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
            val lat = handle.get<Double>("selectedLat")
            val lng = handle.get<Double>("selectedLng")
            val addr = handle.get<String>("selectedAddress")

            if (lat != null && lng != null && addr != null) {
                Log.d(TAG, "Received from LocationPicker: lat=$lat, lng=$lng, address=$addr")
                selectedLat = lat
                selectedLng = lng
                address = addr  // Update textbox address ngay lập tức (recompose sẽ hiện)

                // Clear saved state để tránh trigger lặp
                handle.remove<Double>("selectedLat")
                handle.remove<Double>("selectedLng")
                handle.remove<String>("selectedAddress")
            }
        }
    }

    // Load initial profile data
    LaunchedEffect(profileState) {
        if (profileState is Resource.Success) {
            val profile = (profileState as Resource.Success).data
            profile?.let {
                name = it.name
                phone = it.phone ?: ""
                address = it.address ?: ""  // Load address từ profile nếu chưa có từ map
            }
        }
    }

    // Xử lý success update (navigate back và refresh parent nếu cần)
    LaunchedEffect(updateState) {
        if (updateState is Resource.Success && updateState.data?.isNotEmpty() == true) {
            delay(1000)  // Delay để user thấy message
            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_profile", true)
            viewModel.resetUpdateState()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (profileState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "❌ ${(profileState as Resource.Error).message ?: "Lỗi tải profile"}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            is Resource.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tên hiển thị
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên hiển thị") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Số điện thoại
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Địa chỉ với tọa độ (update từ map, clickable để navigate)
                    OutlinedTextField(
                        value = address,
                        onValueChange = { /* Read-only */ },
                        label = { Text("Địa chỉ mặc định") },
                        placeholder = { Text("Nhấn để chọn vị trí từ bản đồ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.LocationPicker.route) },
                        trailingIcon = {
                            Icon(Icons.Default.Place, contentDescription = "Chọn vị trí")
                        },
                        enabled = false,
                        singleLine = false,
                        maxLines = 2
                    )

                    // Hiển thị tọa độ nếu đã chọn (từ map hoặc fallback)
                    if (selectedLat != null && selectedLng != null) {
                        Text(
                            "📍 Tọa độ: ($selectedLat, $selectedLng)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            Log.d(TAG, "Updating profile: name=$name, phone=$phone, address=$address, lat=$selectedLat, lng=$selectedLng")
                            // Truyền lat/lng nếu có (nếu backend hỗ trợ, hoặc lưu vào DataStore như trước)
                            viewModel.updateProfile(name.trim(), phone.trim(), address.trim(), selectedLat, selectedLng)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = updateState !is Resource.Loading &&
                                name.trim().isNotBlank() &&
                                phone.trim().isNotBlank() &&
                                address.trim().isNotBlank()
                    ) {
                        when (updateState) {
                            is Resource.Loading -> {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Đang lưu...")
                                }
                            }
                            else -> Text("Lưu thay đổi")
                        }
                    }

                    when (val update = updateState) {
                        is Resource.Success -> {
                            if (update.data?.isNotEmpty() == true) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✅", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = update.data,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("❌", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = update.message ?: "Lỗi cập nhật",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}