package com.example.deliveryapp.data.remote.dto

data class MessageDto(
    val id: Long,
    val order_id: Long,
    val from_user_id: Long,
    val to_user_id: Long,
    val content: String,
    val is_read: Boolean,
    val created_at: String
)

data class MessagesResponse(
    val messages: List<MessageDto>
)