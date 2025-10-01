package com.example.deliveryapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.ui.common.InfoDialog
import com.example.deliveryapp.utils.Resource

@Composable
fun OtpVerifyScreen(
    navController: NavController,
    email: String,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var otp by remember { mutableStateOf("") }
    val state by viewModel.verifyOtpState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Xác Thực Email", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Nhập mã OTP đã gửi đến $email")

        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it },
            label = { Text("Mã OTP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.verifyOtp(email, otp) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Xác minh") }

        when (val s = state) {
            is Resource.Loading -> {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            is Resource.Error -> {
                LaunchedEffect(s) {
                    dialogMessage = "❌ Lỗi: ${s.message}"
                    showDialog = true
                }
            }
            is Resource.Success -> {
                LaunchedEffect(s) {
                    dialogMessage = "✅ Email đã được xác thực thành công!"
                    showDialog = true
                }
            }
            else -> {}
        }
    }

    if (showDialog) {
        InfoDialog(
            message = dialogMessage,
            onDismiss = {
                showDialog = false
                if (state is Resource.Success) {
                    navController.navigate("login") {
                        popUpTo("otp_verify/$email") { inclusive = true }
                    }
                }
            }
        )
    }
}
