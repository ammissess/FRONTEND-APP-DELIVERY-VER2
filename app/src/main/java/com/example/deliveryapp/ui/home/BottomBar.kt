// File: BottomBar.kt
package com.example.deliveryapp.ui.home

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.example.deliveryapp.R

@Composable
fun BottomNavigationBar(
    navController: NavController,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // Tab 0: HomePage
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_homepage), contentDescription = "Home") },
            label = { Text("Home") },
            selected = selectedTab == 0,
            onClick = {
                onTabSelected(0)
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
            }
        )

        // Tab 1: Message
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_message), contentDescription = "Message") },
            label = { Text("Message") },
            selected = selectedTab == 1,
            onClick = {
                onTabSelected(1)
                navController.navigate("messages") {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Tab 2: Order
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_order), contentDescription = "Order") },
            label = { Text("Order") },
            selected = selectedTab == 2,
            onClick = {
                onTabSelected(2)
               // navController.navigate("orders") {
                navController.navigate("order_list") {  // ✅ Route mới
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Tab 3: User
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_user), contentDescription = "User") },
            label = { Text("User") },
            selected = selectedTab == 3,
            onClick = {
                onTabSelected(3)
                navController.navigate("profile") {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
