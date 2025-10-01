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
import com.example.deliveryapp.utils.Resource

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val state by viewModel.loginState.collectAsState()
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
            Text("Chào Mừng! 👋", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật Khẩu") },
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Đăng nhập")
            }

            TextButton(onClick = { navController.navigate("forgot_password") }) {
                Text("Quên Mật Khẩu?")
            }
            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Bạn chưa có tài khoản? Đăng ký")
            }

            AnimatedVisibility(visible = state is Resource.Loading) {
                CircularProgressIndicator()
            }

            when (val s = state) {
                is Resource.Error -> {
                    LaunchedEffect(s) {
                        Toast.makeText(
                            context,
                            s.message ?: "Đăng nhập thất bại",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is Resource.Success -> {
                    LaunchedEffect(s) {
                        Toast.makeText(
                            context,
                            "✅ Đăng nhập thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}