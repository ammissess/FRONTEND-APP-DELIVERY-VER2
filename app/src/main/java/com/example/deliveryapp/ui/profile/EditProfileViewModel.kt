package com.example.deliveryapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
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

    fun updateProfile(name: String, phone: String, address: String) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            val request = UpdateProfileRequest(
                name = name,
                phone = phone,
                address = address
            )
            val result = authRepository.updateProfile(request)

            when (result) {
                is Resource.Success -> {
                    // Reload profile sau khi update thành công
                    loadProfile()
                    _updateState.value = Resource.Success("Cập nhật thành công")
                }
                is Resource.Error -> {
                    _updateState.value = Resource.Error(result.message ?: "Lỗi cập nhật profile")
                }
                else -> {}
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = Resource.Success("")
    }
}