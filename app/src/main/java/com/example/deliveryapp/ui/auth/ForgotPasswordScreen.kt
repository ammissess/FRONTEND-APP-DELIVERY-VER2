package com.example.deliveryapp.ui.auth

import androidx.compose.animation.AnimatedVisibility
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
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    val state by viewModel.forgotPassState.collectAsState()

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
            Text("🔐 Quên Mật Khẩu?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.forgotPassword(email) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gửi OTP")
            }

            AnimatedVisibility(visible = state is Resource.Loading) {
                CircularProgressIndicator()
            }

            when (val s = state) {
                is Resource.Error -> {
                    LaunchedEffect(s) {
                        dialogMessage = "❌ ${s.message}"
                        showDialog = true
                    }
                }
                is Resource.Success -> {
                    LaunchedEffect(s) {
                        dialogMessage = "✅ OTP đã được gửi đến $email"
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
                if (state is Resource.Success) {
                    navController.navigate("reset_password/$email")
                }
            }
        )
    }
}