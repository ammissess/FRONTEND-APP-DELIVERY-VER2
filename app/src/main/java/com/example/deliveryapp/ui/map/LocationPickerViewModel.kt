package com.example.deliveryapp.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.api.GeocodingApi
import com.example.deliveryapp.data.remote.api.SearchLocationResponse
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val geocodingApi: GeocodingApi
) : ViewModel() {

    private val _addressState = MutableStateFlow<Resource<String>>(Resource.Loading())
    val addressState: StateFlow<Resource<String>> = _addressState

    private val _searchResults = MutableStateFlow<Resource<List<SearchLocationResponse>>>(Resource.Loading())
    val searchResults: StateFlow<Resource<List<SearchLocationResponse>>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun reverseGeocode(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                _addressState.value = Resource.Loading()
                val response = geocodingApi.reverseGeocode(lat = lat, lon = lng)
                if (response.isSuccessful) {
                    val address = response.body()?.display_name ?: "Vị trí không xác định"
                    _addressState.value = Resource.Success(address)
                } else {
                    _addressState.value = Resource.Error("Lỗi lấy địa chỉ: ${response.code()}")
                }
            } catch (e: Exception) {
                _addressState.value = Resource.Error("Lỗi mạng: ${e.message}")
            }
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchResults.value = Resource.Loading()
                val response = geocodingApi.searchLocation(query = query)
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()
                    _searchResults.value = Resource.Success(results)
                } else {
                    _searchResults.value = Resource.Error("Lỗi tìm kiếm: ${response.code()}")
                }
            } catch (e: Exception) {
                _searchResults.value = Resource.Error("Lỗi mạng: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = Resource.Success(emptyList())
        _isSearching.value = false
    }
}
