package com.example.deliveryapp.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.R
import com.example.deliveryapp.ui.navigation.Screen
import com.example.deliveryapp.ui.session.SessionViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    var dots by remember { mutableStateOf("") }

    // 👇 hiệu ứng chấm ... động
    LaunchedEffect(Unit) {
        while (true) {
            dots = when (dots) {
                "" -> "."
                "." -> ".."
                ".." -> "..."
                else -> ""
            }
            delay(500)
        }
    }

    // 👇 Lắng nghe session
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()

    // Sau 2s thì điều hướng dựa theo trạng thái đăng nhập
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn != null) {
            delay(2000) // splash chờ 2s rồi navigate (optional)
            if (isLoggedIn == true) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    // UI Splash
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_delivery),
            contentDescription = "Delivery Logo",
            modifier = Modifier.size(180.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "loading$dots",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}