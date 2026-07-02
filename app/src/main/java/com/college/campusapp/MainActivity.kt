package com.college.campusapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.campusapp.security.EncryptionManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var selectedTab by remember { mutableStateOf(0) }
            var showSettingsDialog by remember { mutableStateOf(false) }

            if (showSettingsDialog) {
                val email = EncryptionManager.getString(this, "user_email") ?: "N/A"
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = {
                        Text(
                            text = "CampusApp Security Center",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column {
                            Text("Active Session Details:\n", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("• Current Student: $email", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Session Storage: EncryptedSharedPreferences (AES-256)", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• SSL Status: HTTPS Only (No Cleartext)", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Code Shield: Proguard Obfuscation Active", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Screenshots: Blocked in Sensitive Contexts", color = Color(0xFF475569), fontSize = 13.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSettingsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1D8D))
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showSettingsDialog = false
                                EncryptionManager.clear(this@MainActivity)
                                val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        ) {
                            Text("Sign Out", color = Color.Red)
                        }
                    }
                )
            }

            MainScreenView(
                selectedTabIndex = selectedTab,
                onTabSelect = { index ->
                    if (index == 1) {
                        showSettingsDialog = true
                    } else {
                        selectedTab = index
                    }
                },
                onCategoryClick = { title ->
                    if (title == "Order a Product") {
                        startActivity(Intent(this, OrderProductActivity::class.java))
                    } else {
                        Toast.makeText(this, "$title is coming soon!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

data class DashboardCategory(
    val title: String,
    val iconRes: Int,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenView(
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        DashboardCategory("Order a Product", R.drawable.ic_order, "Buy food, books, or devices from campus stores"),
        DashboardCategory("Transfer a Product", R.drawable.ic_transfer, "Instantly pass notes or items to peer students"),
        DashboardCategory("Rent a Product", R.drawable.ic_rent, "List items for short-term rent in your hostel"),
        DashboardCategory("People Gathering for Cab", R.drawable.ic_cab, "Coordinate cab pooling going your way"),
        DashboardCategory("People Gathering for Meal", R.drawable.ic_meal, "Find buddies to split food prices and enjoy meals"),
        DashboardCategory("Assignment Completion", R.drawable.ic_assignment, "Find study groups to complete assignments")
    )

    val primaryColor = Color(0xFF5C1D8D)
    val textPrimary = Color(0xFF0F172A)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CampusApp", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Encrypted Connection Enforced", fontSize = 11.sp, color = Color(0xFF22C55E))
                    }
                },
                actions = {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_secure),
                        contentDescription = "Encrypted Connection icon",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(20.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryColor
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(painter = painterResource(id = android.R.drawable.ic_menu_today), contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = Color(0xFFF1F5F9)
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelect(1) },
                    icon = { Icon(painter = painterResource(id = android.R.drawable.ic_menu_preferences), contentDescription = "Settings") },
                    label = { Text("Security") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryColor,
                        selectedTextColor = primaryColor,
                        indicatorColor = Color(0xFFF1F5F9)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Services Grid",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid Layout of Categories
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    CategoryCardView(category = category, onClick = { onCategoryClick(category.title) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCardView(category: DashboardCategory, onClick: () -> Unit) {
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = category.iconRes),
                    contentDescription = category.title,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = category.description,
                fontSize = 10.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 3
            )
        }
    }
}
