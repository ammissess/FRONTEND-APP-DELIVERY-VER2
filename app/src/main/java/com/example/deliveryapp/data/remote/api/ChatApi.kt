package com.example.deliveryapp.data.remote.api

import com.example.deliveryapp.data.remote.dto.MessageDto
import com.example.deliveryapp.data.remote.dto.MessagesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("messages/{orderId}")
    suspend fun getMessages(
        @Path("orderId") orderId: Long,
        @Query("limit") limit: Int = 20,
        @Query("before") before: Long? = null
    ): Response<MessagesResponse>
}