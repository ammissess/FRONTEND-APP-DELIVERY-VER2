package com.example.deliveryapp.ui.order

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.dto.OrderProductDto
import com.example.deliveryapp.data.remote.dto.PlaceOrderRequestDto
import com.example.deliveryapp.data.remote.dto.ProfileDto
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.ui.home.CartItem
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CheckoutViewModel"

data class DeliveryInfo(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<ProfileDto>>(Resource.Loading())
    val profileState: StateFlow<Resource<ProfileDto>> = _profileState

    private val _confirmOrderState = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val confirmOrderState: StateFlow<Resource<String>> = _confirmOrderState

    private val _deliveryInfo = MutableStateFlow(DeliveryInfo())
    val deliveryInfo: StateFlow<DeliveryInfo> = _deliveryInfo

    // StateFlow cho cart
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = authRepository.getProfile()
        }
    }

    // Method để set cart từ navigation
    fun setCart(newCart: List<CartItem>) {
        _cart.value = newCart
        Log.d(TAG, "Cart set with ${newCart.size} items")
    }

    fun updateDeliveryAddress(lat: Double, lng: Double, address: String) {
        Log.d(TAG, "Update delivery: lat=$lat, lng=$lng, address=$address")
        _deliveryInfo.value = DeliveryInfo(lat, lng, address)
    }

    fun confirmOrder(
        cart: List<CartItem>,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            _confirmOrderState.value = Resource.Loading()

            try {
                val deliveryInfo = _deliveryInfo.value

                // Kiểm tra tọa độ
                if (deliveryInfo.latitude == null || deliveryInfo.longitude == null) {
                    _confirmOrderState.value = Resource.Error("Vui lòng chọn địa chỉ giao hàng")
                    return@launch
                }

                // Lấy refresh token
                val refreshToken = dataStore.refreshToken.first()
                if (refreshToken.isNullOrEmpty()) {
                    _confirmOrderState.value = Resource.Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại")
                    return@launch
                }

                // Tạo request đúng format backend yêu cầu
                val products = cart.map {
                    OrderProductDto(
                        product_id = it.product.id,
                        quantity = it.quantity.toLong()
                    )
                }

                val request = PlaceOrderRequestDto(
                    latitude = deliveryInfo.latitude,
                    longitude = deliveryInfo.longitude,
                    products = products
                )

                // Gọi API với refresh token
                Log.d(TAG, "Calling placeOrderWithRefreshToken")
                val result = orderRepository.placeOrderWithRefreshToken(request, refreshToken)

                if (result is Resource.Error) {
                    Log.e(TAG, "Order failed: ${result.message}")

                    // Kiểm tra nếu lỗi liên quan đến token
                    if (result.message?.contains("401") == true ||
                        result.message?.contains("phiên") == true ||
                        result.message?.contains("token") == true) {

                        // Đăng xuất người dùng
                        authRepository.logout()

                        // Thông báo lỗi cụ thể hơn
                        _confirmOrderState.value = Resource.Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại")
                    } else {
                        _confirmOrderState.value = result
                    }
                } else {
                    _confirmOrderState.value = result

                    // Nếu đặt hàng thành công, xóa giỏ hàng
                    if (result is Resource.Success) {
                        Log.d(TAG, "Order placed successfully, clearing cart")
                        _cart.value = emptyList()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in confirmOrder: ${e.message}", e)
                _confirmOrderState.value = Resource.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }
}