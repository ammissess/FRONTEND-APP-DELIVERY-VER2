package com.example.deliveryapp.ui.profile

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
import androidx.navigation.NavController
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.utils.Resource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProfile(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.resetUpdateState()
    }

    val profileState by viewModel.profileState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    LaunchedEffect(updateState) {
        if (updateState is Resource.Success && updateState.data?.isNotEmpty() == true) {
            // Delay để hiển thị success message
            delay(1000)
            // Refresh ProfileScreen bằng cách gọi lại loadProfile trong ProfileViewModel
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
        when (val state = profileState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "❌ ${state.message ?: "Lỗi tải profile"}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            is Resource.Success -> {
                val profile = state.data
                if (profile != null) {
                    var name by remember { mutableStateOf(profile.name) }
                    var phone by remember { mutableStateOf(profile.phone ?: "") }
                    var address by remember { mutableStateOf(profile.address ?: "") }

                    LaunchedEffect(profile) {
                        name = profile.name
                        phone = profile.phone ?: ""
                        address = profile.address ?: ""
                    }

                    LaunchedEffect(navController.currentBackStackEntry) {
                        navController.currentBackStackEntry?.savedStateHandle?.let { handle ->
                            val selectedAddress = handle.get<String>("selectedAddress")
                            if (selectedAddress != null) {
                                address = selectedAddress
                                // Clear saved state
                                handle.remove<String>("selectedAddress")
                                handle.remove<Double>("selectedLat")
                                handle.remove<Double>("selectedLng")
                            }
                        }
                    }

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
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                viewModel.updateProfile(name.trim(), phone.trim(), address.trim())
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
}
