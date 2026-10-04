package com.college.campusapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class OrderTrackingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OrderRepository.load(this)
        
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""
        val otp = intent.getStringExtra("OTP") ?: ""
        
        setContent {
            OrderTrackingPage(
                orderId = orderId,
                otp = otp,
                onClose = { finish() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
