package com.example.deliveryapp.utils

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.pow

private const val TAG = "WebSocketManager"
private const val MAX_RETRY = 3
private const val BASE_DELAY_MS = 1000L

class WebSocketManager(
    private val token: String,
    private val onMessageReceived: (String) -> Unit,
    private val onClosed: () -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)  // ✅ Thêm ping để keep-alive
        .build()

    private var retryCount = 0

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WS Connected")
            retryCount = 0  // Reset retry
            this@WebSocketManager.webSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received: $text")
            onMessageReceived(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WS Closing: $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WS Closed: code=$code, reason=$reason")
            onClosed()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WS Failure: ${t.message}", t)
            if (retryCount < MAX_RETRY && response?.code != 404) {  // Không retry nếu 404 (sai config)
                retryCount++
                val delay = BASE_DELAY_MS * 2.0.pow(retryCount - 1).toLong()
                Log.d(TAG, "Retry #$retryCount in ${delay}ms")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // Giả sử bạn lưu orderId ở đâu đó, ví dụ global hoặc param
                    connect(0L)  // Hoặc truyền orderId từ outside
                }, delay)
            } else {
                onClosed()
            }
        }
    }

    fun connect(orderId: Long) {  // ✅ Sử dụng orderId trong URL nếu cần
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/api/v1/ws?token=$token&orderId=$orderId")  // ✅ Thêm /api/v1 và orderId
            .build()
        val ws = client.newWebSocket(request, listener)
    }

//    fun sendMessage(orderId: Long, toUserId: Long, content: String) {
//        val message = JSONObject().apply {
//            put("type", "chat_message")
//            put("order_id", orderId)
//            put("to_user_id", toUserId)
//            put("content", content)
//            put("created_at", System.currentTimeMillis())  // ✅ Thêm timestamp nếu backend cần
//        }
//        webSocket?.send(message.toString()) ?: run {
//            Log.w(TAG, "WebSocket not connected, cannot send")
//        }
//    }

    fun sendMessage(orderId: Long, toUserId: Long, content: String) {
        val message = JSONObject().apply {
            put("type", "chat_message")
            put("order_id", orderId)
            put("to_user_id", toUserId)
            put("content", content)
            put("created_at", System.currentTimeMillis())
        }
        webSocket?.send(message.toString())
    }



    fun close() {
        webSocket?.close(1000, "Order completed")
        webSocket = null
        client.dispatcher.executorService.shutdown()  // ✅ Cleanup để tránh leak
    }
}