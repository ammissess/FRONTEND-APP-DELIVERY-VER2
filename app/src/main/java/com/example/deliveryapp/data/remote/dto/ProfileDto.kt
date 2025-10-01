package com.example.deliveryapp.data.remote.dto

data class ProfileDto(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val address: String? = null
)
