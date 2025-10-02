package com.example.deliveryapp.ui.map

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.deliveryapp.R
import com.example.deliveryapp.utils.Resource
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.launch

private const val TAG = "LocationPickerScreen"

@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun LocationPickerScreen(
    navController: NavController,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    val addressState by viewModel.addressState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedLocation by viewModel.selectedLocation.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val pinIcon = rememberIconImage(resourceId = R.drawable.ic_locationn)

    val defaultCenter = Point.fromLngLat(105.804817, 21.028511)
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(defaultCenter)
            zoom(12.0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Debug info
        selectedLocation?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "DEBUG: Lat=${it.lat}, Lng=${it.lng}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Phần tìm kiếm
        if (showSearch) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchLocation(it)
                        },
                        label = { Text("Tìm địa chỉ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Kết quả tìm kiếm
            when (val res = searchResults) {
                is Resource.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        val list = res.data ?: emptyList()
                        items(list) { result ->
                            ListItem(
                                headlineContent = { Text(result.display_name) },
                                supportingContent = { Text("Lat: ${result.lat}, Lng: ${result.lon}") },
                                modifier = Modifier.clickable {
                                    val lat = result.lat.toDoubleOrNull() ?: return@clickable
                                    val lng = result.lon.toDoubleOrNull() ?: return@clickable

                                    viewModel.selectLocation(lat, lng)

                                    coroutineScope.launch {
                                        mapViewportState.flyTo(
                                            CameraOptions.Builder()
                                                .center(Point.fromLngLat(lng, lat))
                                                .zoom(15.0)
                                                .build()
                                        )
                                    }
                                    showSearch = false
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Error -> {
                    Text(
                        text = "Không tìm thấy kết quả: ${res.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            TextButton(
                onClick = { showSearch = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tìm kiếm địa chỉ")
            }
        }

        // Bản đồ
        MapboxMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            mapViewportState = mapViewportState,
            style = {
                MapboxStandardStyle()
            }
        ) {
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.addOnMapClickListener { point ->
                    val lat = point.latitude()
                    val lng = point.longitude()

                    Log.d(TAG, "Map clicked at: Lat=$lat, Lng=$lng")
                    viewModel.selectLocation(lat, lng)

                    coroutineScope.launch {
                        mapViewportState.flyTo(
                            CameraOptions.Builder()
                                .center(point)
                                .zoom(15.0)
                                .build()
                        )
                    }
                    true
                }
            }

            selectedLocation?.let { location ->
                val point = Point.fromLngLat(location.lng, location.lat)
                PointAnnotation(point = point) {
                    iconImage = pinIcon
                }
            }
        }

        // Phần xác nhận địa chỉ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            when (val state = addressState) {
                is Resource.Loading -> {
                    if (selectedLocation != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Đang tải địa chỉ...")
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Tọa độ: (${String.format("%.6f", selectedLocation!!.lat)}, ${String.format("%.6f", selectedLocation!!.lng)})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is Resource.Success -> {
                    val address = state.data ?: "Vị trí không xác định"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Địa chỉ được chọn:", style = MaterialTheme.typography.titleMedium)
                            Text(address)

                            selectedLocation?.let { location ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Latitude: ${String.format("%.6f", location.lat)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Longitude: ${String.format("%.6f", location.lng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    selectedLocation?.let { location ->
                                        Log.d(TAG, "Button clicked - Sending data")
                                        Log.d(TAG, "Lat: ${location.lat}, Lng: ${location.lng}, Address: $address")

                                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                                            set("selectedLat", location.lat)
                                            set("selectedLng", location.lng)
                                            set("selectedAddress", address)
                                            Log.d(TAG, "Data saved to savedStateHandle")
                                        } ?: Log.e(TAG, "previousBackStackEntry is NULL!")

                                        navController.popBackStack()
                                    } ?: Log.e(TAG, "selectedLocation is NULL!")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedLocation != null
                            ) {
                                Text("Xác nhận vị trí")
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    val errorAddress = state.data ?: "Vị trí không xác định (lỗi API)"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Lỗi lấy địa chỉ: ${state.message}",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            selectedLocation?.let { location ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Latitude: ${String.format("%.6f", location.lat)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Longitude: ${String.format("%.6f", location.lng)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        Log.d(TAG, "Error case - Button clicked")
                                        Log.d(TAG, "Lat: ${location.lat}, Lng: ${location.lng}, Address: $errorAddress")

                                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                                            set("selectedLat", location.lat)
                                            set("selectedLng", location.lng)
                                            set("selectedAddress", errorAddress)
                                            Log.d(TAG, "Data saved to savedStateHandle (error case)")
                                        } ?: Log.e(TAG, "previousBackStackEntry is NULL!")

                                        navController.popBackStack()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Xác nhận tọa độ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}