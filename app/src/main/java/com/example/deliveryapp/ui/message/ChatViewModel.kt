package com.example.deliveryapp.ui.message

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.utils.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

// 🟢 Dùng tên khác để tránh trùng DTO trong layer khác
data class ChatMessageUi(
    val id: Long? = null,
    val fromUserId: Long,
    val toUserId: Long,
    val content: String,
    val createdAt: String,
    val orderId: Long
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dataStore: DataStoreManager
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessageUi>()
    val inputText = mutableStateOf("")
    val isChatEnabled = mutableStateOf(true)
    val customerName = mutableStateOf("Customer")

    private var wsManager: WebSocketManager? = null
    private var currentOrderId: Long = 0L
    private var customerId: Long = 0L
    private var shipperId: Long = 0L  // 🟢 shipperId sẽ cập nhật sau từ order detail / token
    val shipperName = mutableStateOf("")

    /**
     * Khởi tạo WebSocket chat giữa shipper và customer
     */
    fun initChat(
        orderId: Long,
        customerId: Long,
        customerName: String,
        token: String,
        shipperId: Long = 0L  // Có thể truyền từ OrderDetail sau
    ) {
        this.currentOrderId = orderId
        this.customerId = customerId
        this.customerName.value = customerName
        this.shipperId = shipperId

        viewModelScope.launch {
            val accessToken = token.ifEmpty {
                dataStore.accessToken.firstOrNull() ?: return@launch
            }

            wsManager = WebSocketManager(
                token = accessToken,
                onMessageReceived = { msg -> handleIncomingMessage(msg) },
                onClosed = { isChatEnabled.value = false }
            )

            wsManager?.connect(orderId)
        }
    }

    /**
     * Nhận tin nhắn từ server (qua WebSocket)
     */
    private fun handleIncomingMessage(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            if (json.optString("type") == "chat_message") {
                val message = ChatMessageUi(
                    fromUserId = json.optLong("from_user_id", 0L),
                    toUserId = json.optLong("to_user_id", 0L),
                    content = json.optString("content", ""),
                    createdAt = json.optString("created_at", ""),
                    orderId = json.optLong("order_id", 0L)
                )
                messages.add(message)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Parse message error: ${e.message}")
        }
    }

    /**
     * Gửi tin nhắn tới customer
     */
    fun sendMessage() {
        if (!isChatEnabled.value || inputText.value.isBlank()) return

        val content = inputText.value
        wsManager?.sendMessage(currentOrderId, customerId, content)

        val sentMsg = ChatMessageUi(
            fromUserId = shipperId,   // 🟢 Shipper là người gửi
            toUserId = customerId,
            content = content,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date()),
            orderId = currentOrderId
        )
        messages.add(sentMsg)
        inputText.value = ""
    }

    /**
     * Khi đơn hàng hoàn thành → đóng chat
     */
    fun onOrderCompleted() {
        isChatEnabled.value = false
        wsManager?.close()
        messages.clear()
    }

    override fun onCleared() {
        wsManager?.close()
        super.onCleared()
    }
}
