package com.example.deliveryapp.ui.map

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.deliveryapp.utils.LocationPermissionHelper
import com.example.deliveryapp.utils.Resource
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.ComposeMapInitOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location

private const val TAG = "LocationPickerDebug"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LocationPickerScreen(
    navController: NavController,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedPoint by remember { mutableStateOf<Point?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(LocationPermissionHelper.hasLocationPermission(context))
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val addressState by viewModel.addressState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // manager cho annotation (marker)
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (!hasLocationPermission) {
            showPermissionDialog = true
        }
    }

    // Camera mặc định: TP. HCM
    val cameraOptions = CameraOptions.Builder()
        .center(Point.fromLngLat(106.6297, 10.8231))
        .zoom(12.0)
        .build()

    // Tạo initOptions 1 lần
    val composeInitOptions = remember {
        ComposeMapInitOptions(
            mapOptions = MapOptions.Builder().build()
        )
    }

    // Check permission on first launch
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            LocationPermissionHelper.requestLocationPermission(permissionLauncher)
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Quyền truy cập vị trí") },
            text = {
                Text("Ứng dụng cần quyền truy cập vị trí để hiển thị vị trí hiện tại của bạn trên bản đồ và cung cấp dịch vụ tốt hơn.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        LocationPermissionHelper.requestLocationPermission(permissionLauncher)
                    }
                ) {
                    Text("Cho phép")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Từ chối")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chọn vị trí") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    showSearchResults = it.isNotEmpty()
                    if (it.isNotEmpty()) {
                        viewModel.searchLocation(it)
                    } else {
                        viewModel.clearSearch()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Tìm kiếm địa điểm...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                showSearchResults = false
                                viewModel.clearSearch()
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (searchQuery.isNotEmpty()) {
                            viewModel.searchLocation(searchQuery)
                        }
                    }
                ),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // MapboxMap
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    composeMapInitOptions = composeInitOptions
                ) {
                    MapEffect { mapView ->
                        // Set camera và load style
                        mapView.mapboxMap.setCamera(cameraOptions)
                        mapView.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)

                        // Enable location component if permission granted
                        if (hasLocationPermission) {
                            mapView.location.updateSettings {
                                enabled = true
                                pulsingEnabled = true
                            }
                        }

                        // Tạo annotation manager nếu chưa có
                        if (pointAnnotationManager == null) {
                            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
                        }

                        // Add click listener
                        mapView.gestures.addOnMapClickListener { point ->
                            selectedPoint = point
                            showSearchResults = false
                            keyboardController?.hide()
                            viewModel.reverseGeocode(point.latitude(), point.longitude())

                            // Xoá marker cũ
                            pointAnnotationManager?.deleteAll()

                            // Thêm marker mới
                            val options = PointAnnotationOptions()
                                .withPoint(point)
                                .withIconSize(1.0)

                            pointAnnotationManager?.create(options)
                            true
                        }
                    }
                }

                // Search Results Overlay
                if (showSearchResults && searchQuery.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        when (val state = searchResults) {
                            is Resource.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSearching) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            is Resource.Success -> {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    items(state.data ?: emptyList()) { location ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    text = location.display_name,
                                                    maxLines = 2
                                                )
                                            },
                                            leadingContent = {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                val point = Point.fromLngLat(
                                                    location.lon.toDouble(),
                                                    location.lat.toDouble()
                                                )
                                                selectedPoint = point
                                                searchQuery = location.display_name
                                                showSearchResults = false
                                                keyboardController?.hide()

                                                // Update map camera
                                                val newCameraOptions = CameraOptions.Builder()
                                                    .center(point)
                                                    .zoom(15.0)
                                                    .build()

                                                // Xoá marker cũ và thêm marker mới
                                                pointAnnotationManager?.deleteAll()
                                                val options = PointAnnotationOptions()
                                                    .withPoint(point)
                                                    .withIconSize(1.0)
                                                pointAnnotationManager?.create(options)

                                                viewModel.reverseGeocode(point.latitude(), point.longitude())
                                            }
                                        )
                                        if (location != (state.data?.lastOrNull())) {
                                            Divider()
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> {
                                Text(
                                    text = state.message ?: "Lỗi tìm kiếm",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Current Location Button
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = {
                            // Get current location logic here
                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            try {
                                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                                location?.let {
                                    val point = Point.fromLngLat(it.longitude, it.latitude)
                                    selectedPoint = point
                                    viewModel.reverseGeocode(it.latitude, it.longitude)

                                    // Update camera and marker
                                    pointAnnotationManager?.deleteAll()
                                    val options = PointAnnotationOptions()
                                        .withPoint(point)
                                        .withIconSize(1.0)
                                    pointAnnotationManager?.create(options)
                                }
                            } catch (e: SecurityException) {
                                // Handle permission error
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .offset(y = (-10).dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = "Current Location",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // UI thông tin và xác nhận khi đã chọn
                if (selectedPoint != null && !showSearchResults) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        when (val state = addressState) {
                            is Resource.Success -> {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = state.data ?: "Đang tải...",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            is Resource.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = state.message ?: "Lỗi lấy địa chỉ",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            is Resource.Loading -> {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đang lấy địa chỉ...")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ✅ SỬA: Luôn lưu lat/lng (ngay cả khi address loading), thêm log debug
                        Button(
                            onClick = {
                                val address = (addressState as? Resource.Success)?.data ?: "Vị trí không xác định"
                                Log.d(TAG, "Confirm location: lat=${selectedPoint!!.latitude()}, lng=${selectedPoint!!.longitude()}, address=$address")

                                // Lưu vào savedStateHandle của parent (CheckoutScreen)
                                navController.previousBackStackEntry?.savedStateHandle?.set("selectedLat", selectedPoint!!.latitude())
                                navController.previousBackStackEntry?.savedStateHandle?.set("selectedLng", selectedPoint!!.longitude())
                                navController.previousBackStackEntry?.savedStateHandle?.set("selectedAddress", address)

                                navController.popBackStack()
                            },
                            enabled = selectedPoint != null,  // ✅ SỬA: Enable nếu có point, không phụ thuộc address
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Xác nhận vị trí")
                        }

                    }
                }
            }
        }
    }
}