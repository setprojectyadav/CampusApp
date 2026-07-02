package com.college.campusapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.core.content.ContextCompat
import kotlin.system.exitProcess

class PermissionsActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        var allGranted = true
        for ((_, isGranted) in results) {
            if (!isGranted) {
                allGranted = false
                break
            }
        }

        if (allGranted) {
            proceedToIntro()
        } else {
            showDeniedDialog.value = true
        }
    }

    private val showDeniedDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkAllPermissionsGranted()) {
            proceedToIntro()
            return
        }

        setContent {
            PermissionsScreenView(
                showDeniedDialog = showDeniedDialog.value,
                onGrantClick = { triggerPermissionsRequest() },
                onExitClick = { exitApp() }
            )
        }
    }

    private fun checkAllPermissionsGranted(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return locationGranted && notificationGranted
    }

    private fun triggerPermissionsRequest() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun proceedToIntro() {
        Toast.makeText(this, "Permissions Verified!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, IntroActivity::class.java))
        finish()
    }

    private fun exitApp() {
        finishAffinity()
        exitProcess(0)
    }
}

@Composable
fun PermissionsScreenView(
    showDeniedDialog: Boolean,
    onGrantClick: () -> Unit,
    onExitClick: () -> Unit
) {
    val primaryColor = Color(0xFF5C1D8D)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)

    if (showDeniedDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permissions Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "CampusApp requires location and notification permissions to provide its core features. Since permission was denied, the application will now exit.",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onExitClick,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Exit App")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Permission Icon
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                contentDescription = "Permissions Required Info",
                tint = primaryColor,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Access Permissions",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We need location and notification access to connect you with campus delivery services, cab pools, and alert notifications.",
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card list of requested permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Location Card Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                            contentDescription = "Location icon",
                            tint = primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Location Services", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Used to calculate delivery drop points and match cab pooling sharing near you.", fontSize = 12.sp, color = textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notifications Card Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_popup_reminder),
                            contentDescription = "Notification icon",
                            tint = primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Push Notifications", fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Receive alerts for order status, cab updates, and meal requests.", fontSize = 12.sp, color = textSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
