package com.example.deliveryapp.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("format") format: String = "json",
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int = 18,
        @Query("addressdetails") addressdetails: Int = 1
    ): Response<ReverseGeocodeResponse>

    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("addressdetails") addressdetails: Int = 1
    ): Response<List<SearchLocationResponse>>
}

data class ReverseGeocodeResponse(
    val place_id: Long,
    val display_name: String,
    val address: AddressDetails?
)

data class SearchLocationResponse(
    val place_id: Long,
    val display_name: String,
    val lat: String,
    val lon: String,
    val address: AddressDetails?
)

data class AddressDetails(
    val road: String?,
    val neighbourhood: String?,
    val suburb: String?,
    val city: String?,
    val state: String?,
    val country: String?
)
