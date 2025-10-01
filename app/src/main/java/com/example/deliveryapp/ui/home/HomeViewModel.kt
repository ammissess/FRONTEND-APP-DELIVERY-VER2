package com.example.deliveryapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deliveryapp.data.remote.dto.ProductDto
import com.example.deliveryapp.domain.usecase.GetProductsUseCase
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// CartItem data class
//data class CartItem(val product: ProductDto, val quantity: Int = 1)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProducts: GetProductsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Danh sách sản phẩm
    private val _products = MutableStateFlow<Resource<List<ProductDto>>>(Resource.Loading())
    val products: StateFlow<Resource<List<ProductDto>>> = _products

    // Loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Danh mục
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    // Danh mục được chọn
    private val _selectedCategory = MutableStateFlow("Tất cả")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Lỗi
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Giỏ hàng
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart

    init {
        fetchProducts()
        fetchCategories()
    }

    /** Lấy tất cả sản phẩm */
    fun fetchProducts(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _products.value = getProducts(page)
            } catch (e: Exception) {
                _products.value = Resource.Error(e.message ?: "Lỗi tải sản phẩm")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Fake API categories (anh có thể thay bằng repo thực tế) */
    fun fetchCategories() {
        viewModelScope.launch {
            _categories.value = listOf("Tất cả", "Trái cây", "Đồ uống", "Đồ ăn nhanh")
        }
    }

    /** Lọc sản phẩm theo category */
    fun fetchProductsByCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = if (category == "Tất cả") {
                    getProducts(1)
                } else {
                    val all = getProducts(1)
                    if (all is Resource.Success) {
                        Resource.Success(all.data?.filter {
                            it.name.contains(category, ignoreCase = true)
                        } ?: emptyList())
                    } else {
                        all
                    }
                }
                _products.value = result
            } catch (e: Exception) {
                _products.value = Resource.Error(e.message ?: "Lỗi lọc sản phẩm")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Tìm kiếm sản phẩm theo tên */
    fun searchProducts(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val all = getProducts(1)
                if (all is Resource.Success) {
                    val filtered = all.data?.filter {
                        it.name.contains(query, ignoreCase = true)
                    } ?: emptyList()
                    _products.value = Resource.Success(filtered)
                } else {
                    _products.value = all
                }
            } catch (e: Exception) {
                _products.value = Resource.Error(e.message ?: "Lỗi tìm kiếm")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Giỏ hàng */
    fun addToCart(product: ProductDto) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(product, 1))
        }
        _cart.value = current
    }

    fun increaseQty(product: ProductDto) = addToCart(product)

    fun decreaseQty(product: ProductDto) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
            } else {
                current.removeAt(index)
            }
        }
        _cart.value = current
    }

    fun getCartQuantity(productId: Long): Int {
        return _cart.value.find { it.product.id == productId }?.quantity ?: 0
    }

    fun clearCart() { _cart.value = emptyList() }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}