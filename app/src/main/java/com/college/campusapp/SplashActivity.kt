package com.college.campusapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.campusapp.security.EncryptionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SplashScreenView(
                onAnimationFinished = {
                    val isSignedUp = EncryptionManager.getBoolean(this, "is_signed_up", false)
                    val rememberMe = EncryptionManager.getBoolean(this, "remember_me", false)

                    // Persistent Login check
                    val targetActivity = if (isSignedUp && rememberMe) {
                        MainActivity::class.java
                    } else {
                        AuthActivity::class.java
                    }

                    startActivity(Intent(this, targetActivity))
                    finish()
                }
            )
        }
    }
}

@Composable
fun SplashScreenView(onAnimationFinished: () -> Unit) {
    val scaleAnim = remember { Animatable(0.5f) }
    val rotateAnim = remember { Animatable(-30f) }

    LaunchedEffect(key1 = true) {
        // Run entry animations in parallel
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1500)
            )
        }
        launch {
            rotateAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1500)
            )
        }
        // Total delay representing loading
        delay(2500)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)), // Slate-50 background (light background)
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_campus),
                contentDescription = "CampusApp Logo",
                modifier = Modifier
                    .size(130.dp)
                    .scale(scaleAnim.value)
                    .rotate(rotateAnim.value),
                colorFilter = ColorFilter.tint(Color(0xFF5C1D8D)) // Custom brand primary color
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CampusApp",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D1060),
                modifier = Modifier.scale(scaleAnim.value)
            )

            Text(
                text = "Your Campus Assistant",
                fontSize = 14.sp,
                color = Color(0xFF475569),
                modifier = Modifier.scale(scaleAnim.value)
            )
        }

        // Bottom horizontal progress bar indicator
        LinearProgressIndicator(
            color = Color(0xFFE1127A), // Brand accent color
            trackColor = Color(0xFFF1F5F9),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(width = 200.dp, height = 4.dp)
                .background(Color.Transparent)
        )
    }
}
