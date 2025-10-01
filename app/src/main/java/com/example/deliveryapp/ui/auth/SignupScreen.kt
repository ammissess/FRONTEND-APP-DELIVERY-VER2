package com.example.deliveryapp.ui.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.data.remote.dto.SignupRequestDto
import com.example.deliveryapp.utils.Resource

@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val state by viewModel.signupState.collectAsState()
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚚 Tạo Tài Khoản", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên") })
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật Khẩu") },
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số Điện Thoại") })
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Địa chỉ") })

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && phone.isNotBlank() && address.isNotBlank()) {
                        viewModel.signup(SignupRequestDto(name, email, password, phone, address))
                    } else {
                        Toast.makeText(context, "❌ Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Đăng Ký") }

            TextButton(onClick = { navController.navigate("login") }) {
                Text("Bạn đã có tài khoản? Đăng Nhập")
            }

            AnimatedVisibility(visible = state is Resource.Loading) {
                CircularProgressIndicator()
            }

            when (val s = state) {
                is Resource.Error -> {
                    LaunchedEffect(s) {
                        Toast.makeText(context, s.message ?: "Đăng ký thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
                is Resource.Success -> {
                    LaunchedEffect(s) {
                        Toast.makeText(
                            context,
                            "✅ Tạo tài khoản thành công! Vui lòng kiểm tra email để nhập OTP.",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.navigate("otp_verify/$email")
                    }
                }
                else -> {}
            }
        }
    }
}