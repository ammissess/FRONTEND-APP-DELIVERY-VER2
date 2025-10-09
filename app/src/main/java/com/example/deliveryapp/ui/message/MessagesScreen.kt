package com.example.deliveryapp.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.data.local.DataStoreManager
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    orderId: Long,
    shipperId: Long,
    shipperName: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val inputText by viewModel.inputText
    val isEnabled by viewModel.isChatEnabled
    val name by viewModel.shipperName
    val context = LocalContext.current
    val dataStore = remember { DataStoreManager(context) }
    val token by dataStore.accessToken.map { it ?: "" }.collectAsState(initial = "")

    // ✅ Gọi initChat thay connect
    LaunchedEffect(orderId, shipperId, shipperName, token) {
        if (token.isNotEmpty()) {
            viewModel.initChat(orderId, shipperId, shipperName, token)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Chat với $name") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            }
        )

        if (!isEnabled) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Đơn hàng đã hoàn tất, không thể trò chuyện.", color = Color.Gray)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isFromUser = msg.fromUserId != shipperId
                val bubbleColor = if (isFromUser) Color(0xFFDCF8C6) else Color(0xFFE0E0E0)
                val alignment = if (isFromUser) Alignment.CenterEnd else Alignment.CenterStart
                Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
                    Box(
                        Modifier.background(bubbleColor, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .widthIn(max = 250.dp)
                    ) {
                        Text("${if (isFromUser) "Bạn" else name}: ${msg.content}")
                    }
                }
            }
        }

        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.inputText.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") }
            )
            IconButton(onClick = { viewModel.sendMessage() }) {
                Icon(Icons.Default.Send, contentDescription = null)
            }
        }
    }
}