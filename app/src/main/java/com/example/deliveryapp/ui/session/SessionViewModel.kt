package com.example.deliveryapp.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.dto.RefreshTokenRequestDto
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.di.NormalAuthApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SessionViewModel"
private const val TOKEN_CHECK_INTERVAL = 60_000L // 1 phút kiểm tra một lần

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
    private val authRepository: AuthRepository,
    @NormalAuthApi private val authApi: AuthApi
) : ViewModel() {

    // StateFlow kiểm tra nếu có token -> đã đăng nhập
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn

    // StateFlow cho trạng thái token
    private val _tokenState = MutableStateFlow<TokenState>(TokenState.Unknown)
    val tokenState: StateFlow<TokenState> = _tokenState

    init {
        // Kiểm tra token ban đầu
        checkLoginStatus()

        // Bắt đầu kiểm tra token định kỳ
        startTokenRefreshChecker()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            dataStore.accessToken.collect { token ->
                Log.d(TAG, "Current token: ${token?.take(10)}")
                _isLoggedIn.value = !token.isNullOrEmpty()

                if (!token.isNullOrEmpty()) {
                    // Kiểm tra tính hợp lệ của token ngay khi có token
                    checkTokenValidity()
                } else {
                    _tokenState.value = TokenState.Invalid
                }
            }
        }
    }

    private fun startTokenRefreshChecker() {
        viewModelScope.launch {
            while (true) {
                delay(TOKEN_CHECK_INTERVAL)

                // Chỉ kiểm tra nếu người dùng đã đăng nhập
                if (_isLoggedIn.value == true) {
                    checkTokenValidity()
                }
            }
        }
    }

    // Phương thức mới để kiểm tra tính hợp lệ của access token
    private suspend fun checkTokenValidity() {
        try {
            // Thử gọi API profile để kiểm tra access token
            val profileResult = authRepository.getProfile()

            if (profileResult is com.example.deliveryapp.utils.Resource.Success) {
                // Access token vẫn hợp lệ
                Log.d(TAG, "Access token is valid")
                _tokenState.value = TokenState.Valid
            } else {
                // Access token có thể đã hết hạn, thử refresh
                Log.d(TAG, "Access token may be expired, trying to refresh")
                refreshTokenIfNeeded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking token validity: ${e.message}", e)
            // Có lỗi khi kiểm tra, thử refresh
            refreshTokenIfNeeded()
        }
    }

    private suspend fun refreshTokenIfNeeded() {
        try {
            // Lấy refresh token hiện tại
            val refreshToken = dataStore.refreshToken.first()
            if (refreshToken.isNullOrEmpty()) {
                Log.d(TAG, "No refresh token available")
                _tokenState.value = TokenState.Invalid
                _isLoggedIn.value = false
                return
            }

            // Thử refresh token
            val result = authRepository.refreshToken(refreshToken)

            if (result is com.example.deliveryapp.utils.Resource.Success) {
                // Token đã được refresh thành công
                Log.d(TAG, "Token refreshed successfully")
                _tokenState.value = TokenState.Valid
                _isLoggedIn.value = true
            } else {
                // Token không hợp lệ hoặc hết hạn
                Log.e(TAG, "Token refresh failed: ${(result as? com.example.deliveryapp.utils.Resource.Error)?.message}")
                _tokenState.value = TokenState.Invalid
                _isLoggedIn.value = false

                // Đăng xuất người dùng
                authRepository.logout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token: ${e.message}", e)
            // Giữ nguyên trạng thái hiện tại, không đăng xuất người dùng
        }
    }

    // Phương thức để kiểm tra token theo yêu cầu (có thể gọi từ UI)
    fun checkToken() {
        viewModelScope.launch {
            checkTokenValidity()
        }
    }

    // Phương thức để đăng xuất
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.value = false
            _tokenState.value = TokenState.Invalid
        }
    }
}

// Enum class để biểu diễn trạng thái token
sealed class TokenState {
    object Unknown : TokenState()
    object Valid : TokenState()
    object Invalid : TokenState()
}