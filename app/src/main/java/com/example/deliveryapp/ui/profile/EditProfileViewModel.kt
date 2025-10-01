package com.example.deliveryapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.ProfileDto
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    // Inject your repository here
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<ProfileDto>>(Resource.Loading())
    val profileState: StateFlow<Resource<ProfileDto>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val updateState: StateFlow<Resource<String>> = _updateState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = Resource.Loading()
            try {
                // Mock data for now - replace with actual API call
                val mockProfile = ProfileDto(
                    id = 1,
                    name = "Nguyễn Văn A",
                    email = "user@example.com",
                    phone = "0123456789",
                    address = "123 Đường ABC, Quận 1, TP.HCM"
                )
                _profileState.value = Resource.Success(mockProfile)
            } catch (e: Exception) {
                _profileState.value = Resource.Error(e.message ?: "Lỗi tải profile")
            }
        }
    }

    fun updateProfile(name: String, phone: String, address: String) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            try {
                // Mock API call - replace with actual implementation
                kotlinx.coroutines.delay(1000) // Simulate network delay

                // Update the profile state with new data
                val currentProfile = (_profileState.value as? Resource.Success)?.data
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        name = name,
                        phone = phone,
                        address = address
                    )
                    _profileState.value = Resource.Success(updatedProfile)
                }

                _updateState.value = Resource.Success("Cập nhật thành công")
            } catch (e: Exception) {
                _updateState.value = Resource.Error(e.message ?: "Lỗi cập nhật profile")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = Resource.Success("")
    }
}
