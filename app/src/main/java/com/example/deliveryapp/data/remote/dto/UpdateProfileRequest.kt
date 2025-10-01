package com.example.deliveryapp.data.remote.dto

data class UpdateProfileRequest(
    val name: String,
    val phone: String?,
    val address: String?
)