package com.example.deliveryapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.ProfileDto
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<ProfileDto>>(Resource.Loading())
    val profileState: StateFlow<Resource<ProfileDto>> = _profileState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = authRepository.getProfile()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}