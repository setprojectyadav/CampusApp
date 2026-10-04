package com.college.campusapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleGatewayScreen(
    onSelectStudent: () -> Unit,
    onSelectRider: () -> Unit,
    onSelectAdmin: () -> Unit,
    onBack: () -> Unit
) {
    val primaryColor = AppTheme.Primary
    val successColor = AppTheme.Success
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App bar back button & title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("◀", fontSize = 16.sp, color = textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Console Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(36.dp))
        
        Text(
            text = "Choose Your Role",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            color = textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select Student Console to order/rent items, Rider Console to deliver, or Admin Console to review requests",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(36.dp))
        
        // Student Console Card
        Card(
            onClick = onSelectStudent,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛒", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Student Console",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Browse campus store products, rent instruments, pay securely, and track orders.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Rider Console Card
        Card(
            onClick = onSelectRider,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, successColor.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(successColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚴", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rider Console",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Review pending orders, claim delivery jobs, and earn payouts in your app wallet.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Admin Console Card
        Card(
            onClick = onSelectAdmin,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF64748B).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Admin Console",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Approve hosted rental items, monitor platform transactions, and manage campus listings.",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ConsoleGatewayScreenPreview() {
    ConsoleGatewayScreen(
        onSelectStudent = {},
        onSelectRider = {},
        onSelectAdmin = {},
        onBack = {}
    )
}
