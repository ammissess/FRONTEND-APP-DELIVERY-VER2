package com.example.deliveryapp.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.dto.ProfileDto
import com.example.deliveryapp.data.remote.dto.UpdateProfileRequest
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EditProfileVM"

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStore: DataStoreManager  // Inject để lưu lat/lng
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<ProfileDto>>(Resource.Loading())
    val profileState: StateFlow<Resource<ProfileDto>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val updateState: StateFlow<Resource<String>> = _updateState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = Resource.Loading()
            _profileState.value = authRepository.getProfile()
        }
    }

    // SỬA: Thêm param lat/lng, lưu vào DataStore (backend chỉ nhận address string)
    fun updateProfile(name: String, phone: String, address: String, lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            try {
                val request = UpdateProfileRequest(
                    name = name,
                    phone = phone,
                    address = address  // Backend chỉ nhận string address
                )
                Log.d(TAG, "Calling updateProfile with request: $request")
                val result = authRepository.updateProfile(request)

                when (result) {
                    is Resource.Success -> {
                        // Lưu lat/lng vào DataStore nếu có (fallback cho order)
                        if (lat != null && lng != null) {
                            dataStore.saveLocation(lat, lng)
                            Log.d(TAG, "Saved location to DataStore: lat=$lat, lng=$lng")
                        }
                        // Reload profile
                        loadProfile()
                        _updateState.value = Resource.Success("Cập nhật thành công")
                    }
                    is Resource.Error -> {
                        // SỬA: Loại bỏ 'code' vì Resource.Error không có property 'code'
                        Log.e(TAG, "Update failed: ${result.message}")
                        _updateState.value = Resource.Error(result.message ?: "Lỗi cập nhật profile")
                    }
                    else -> {
                        _updateState.value = Resource.Error("Trạng thái không xác định")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update exception: ${e.message}", e)
                _updateState.value = Resource.Error(e.message ?: "Lỗi cập nhật profile")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = Resource.Success("")
    }
}