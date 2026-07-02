package com.college.campusapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.campusapp.security.EncryptionManager
import kotlinx.coroutines.launch

class IntroActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntroScreenView(
                onIntroCompleted = {
                    EncryptionManager.saveBoolean(this, "is_intro_completed", true)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

data class IntroSlide(
    val iconRes: Int,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreenView(onIntroCompleted: () -> Unit) {
    val slidesList = listOf(
        IntroSlide(
            R.drawable.ic_order,
            "Order Products Instantly",
            "Search and order products from verified campus stores. Keep track of quantities and get items delivered to your exact campus spot."
        ),
        IntroSlide(
            R.drawable.ic_transfer,
            "Transfer & Rent Items",
            "Easily transfer books, notes, or gadgets to other students. List your items to rent out securely within your dorm community."
        ),
        IntroSlide(
            R.drawable.ic_cab,
            "Cab & Meal Gatherings",
            "Split cab fares with campus peers going your way, or find dining buddies to share meals and save daily expenses together."
        ),
        IntroSlide(
            R.drawable.ic_assignment,
            "Assignment Completion",
            "Form peer groups to complete assignments on time. Keep track of tasks, ask questions, and study collaboratively in secure rooms."
        )
    )

    val pagerState = rememberPagerState(pageCount = { slidesList.size })
    val coroutineScope = rememberCoroutineScope()

    val primaryColor = Color(0xFF5C1D8D)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Slidable Content using HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = slidesList[page]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.size(160.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = slide.iconRes),
                                contentDescription = slide.title,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = slide.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = slide.description,
                        fontSize = 14.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Bottom Navigation Indicators and Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Interactive animated dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(16.dp)
                ) {
                    repeat(slidesList.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = tween(durationMillis = 300),
                            label = "dotWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) primaryColor else Color(0xFFCBD5E1),
                            animationSpec = tween(durationMillis = 300),
                            label = "dotColor"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Standard action button
                Button(
                    onClick = {
                        val current = pagerState.currentPage
                        if (current < slidesList.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(current + 1)
                            }
                        } else {
                            onIntroCompleted()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(
                        text = if (pagerState.currentPage == slidesList.size - 1) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
