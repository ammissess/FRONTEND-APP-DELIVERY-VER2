package com.example.deliveryapp.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val dataStore: DataStoreManager
) : ViewModel() {

    // ✅ StateFlow kiểm tra nếu có token -> đã đăng nhập
    val isLoggedIn: StateFlow<Boolean> = dataStore.accessToken
        .map { !it.isNullOrEmpty() } // true nếu token tồn tại
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}