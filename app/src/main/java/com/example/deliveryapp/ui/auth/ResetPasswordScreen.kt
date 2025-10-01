package com.example.deliveryapp.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.ui.common.InfoDialog
import com.example.deliveryapp.utils.Resource

@Composable
fun ResetPasswordScreen(
    navController: NavController,
    email: String,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val verifyState by viewModel.verifyResetOtpState.collectAsState()
    val resetState by viewModel.resetPasswordState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Đặt lại mật khẩu", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Nhập mã OTP và mật khẩu mới cho $email")

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("OTP") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Mật Khẩu Mới") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.verifyOtpForReset(email, otp) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Xác minh OTP")
            }

            AnimatedVisibility(visible = verifyState is Resource.Loading || resetState is Resource.Loading) {
                CircularProgressIndicator()
            }

            // Kiểm tra verify OTP
            when (val s = verifyState) {
                is Resource.Error -> {
                    LaunchedEffect(s) {
                        dialogMessage = "❌ ${s.message}"
                        showDialog = true
                    }
                }
                is Resource.Success -> {
                    LaunchedEffect(s) {
                        val resetToken = s.data?.resetToken ?: ""
                        if (newPassword.isNotBlank()) {
                            viewModel.resetPassword(resetToken, newPassword)
                        } else {
                            dialogMessage = "❌ Vui lòng nhập mật khẩu mới"
                            showDialog = true
                        }
                    }
                }
                else -> {}
            }

            // Kiểm tra reset password
            when (val s = resetState) {
                is Resource.Error -> {
                    LaunchedEffect(s) {
                        dialogMessage = "❌ ${s.message}"
                        showDialog = true
                    }
                }
                is Resource.Success -> {
                    LaunchedEffect(s) {
                        dialogMessage = "✅ Đổi mật khẩu thành công!"
                        showDialog = true
                    }
                }
                else -> {}
            }
        }
    }

    if (showDialog) {
        InfoDialog(
            message = dialogMessage,
            onDismiss = {
                showDialog = false
                // Nếu đổi mật khẩu thành công thì điều hướng về login
                if (resetState is Resource.Success) {
                    navController.navigate("login") {
                        popUpTo("reset_password/$email") { inclusive = true }
                    }
                }
            }
        )
    }
}