package com.example.deliveryapp.ui.message

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.ChatApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

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
    private val dataStore: DataStoreManager,
    private val chatApi: ChatApi        // ✅ inject ChatApi mới
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessageUi>()
    val inputText = mutableStateOf("")
    val isChatEnabled = mutableStateOf(true)
    val customerName = mutableStateOf("Customer")

    private var wsManager: com.example.deliveryapp.utils.WebSocketManager? = null
    private var currentOrderId: Long = 0L
    private var customerId: Long = 0L
    private var shipperId: Long = 0L
    val shipperName = mutableStateOf("")

    /** ✅ Load tin nhắn từ API trước khi connect WebSocket */
    private fun loadRecentMessages(orderId: Long) {
        viewModelScope.launch {
            try {
                val response = chatApi.getMessages(orderId, 20)
                if (response.isSuccessful) {
                    val messageList = response.body()?.messages ?: emptyList()
                    val uiMsgs = messageList.sortedBy { it.id }.map {
                        ChatMessageUi(
                            id = it.id,
                            fromUserId = it.from_user_id,
                            toUserId = it.to_user_id,
                            content = it.content,
                            createdAt = it.created_at,
                            orderId = it.order_id
                        )
                    }
                    messages.clear()
                    messages.addAll(uiMsgs)
                    Log.d("ChatViewModel", "Loaded ${uiMsgs.size} messages from REST API")
                } else {
                    Log.e("ChatViewModel", "Failed to load messages: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading messages: ${e.message}")
            }
        }
    }

    /** ✅ Load thêm tin nhắn cũ (scroll lên) */
    fun loadMore(beforeId: Long) {
        viewModelScope.launch {
            try {
                val response = chatApi.getMessages(currentOrderId, 20, beforeId)
                if (response.isSuccessful) {
                    val more = response.body()?.messages ?: emptyList()
                    val uiMsgs = more.sortedBy { it.id }.map {
                        ChatMessageUi(
                            id = it.id,
                            fromUserId = it.from_user_id,
                            toUserId = it.to_user_id,
                            content = it.content,
                            createdAt = it.created_at,
                            orderId = it.order_id
                        )
                    }
                    messages.addAll(0, uiMsgs)
                    Log.d("ChatViewModel", "Loaded ${uiMsgs.size} older messages")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Load more error: ${e.message}")
            }
        }
    }

    /** Khởi tạo Chat: tải REST → mở WebSocket */
    fun initChat(
        orderId: Long,
        shipperId: Long,
        shipperName: String,
        token: String
    ) {
        this.currentOrderId = orderId
        this.shipperId = shipperId
        this.shipperName.value = shipperName

        viewModelScope.launch {
            loadRecentMessages(orderId)     // 🟢 tải tin nhắn trước
            val accessToken = token.ifEmpty { dataStore.accessToken.firstOrNull() ?: return@launch }
            wsManager = com.example.deliveryapp.utils.WebSocketManager(
                token = accessToken,
                onMessageReceived = { msg -> handleIncomingMessage(msg) },
                onClosed = { isChatEnabled.value = false }
            )
            wsManager?.connect(orderId)
        }
    }

    /** Nhận tin nhắn mới từ WS */
    private fun handleIncomingMessage(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            if (json.optString("type") == "chat_message") {
                val message = ChatMessageUi(
                    fromUserId = json.optLong("from_user_id"),
                    toUserId = json.optLong("to_user_id"),
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

    /** Gửi tin nhắn đến shipper */
    fun sendMessage() {
        if (!isChatEnabled.value || inputText.value.isBlank()) return
        val content = inputText.value
        wsManager?.sendMessage(currentOrderId, shipperId, content)
        val sentMsg = ChatMessageUi(
            fromUserId = customerId,
            toUserId = shipperId,
            content = content,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(java.util.Date()),
            orderId = currentOrderId
        )
        messages.add(sentMsg)
        inputText.value = ""
    }

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