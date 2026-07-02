package com.college.campusapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.college.campusapp.api.FoodFactsApi
import com.college.campusapp.api.Product
import com.college.campusapp.api.SearchResponse
import com.college.campusapp.security.SecurityUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderProductActivity : ComponentActivity() {

    private val api: FoodFactsApi by lazy { FoodFactsApi.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var searchQuery by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var selectedProduct by remember { mutableStateOf<Product?>(null) }
            var quantity by remember { mutableStateOf(1) }
            var pickPoint by remember { mutableStateOf("") }
            var dropPoint by remember { mutableStateOf("") }
            var instructions by remember { mutableStateOf("") }
            var autopayEnabled by remember { mutableStateOf(false) }
            var showOrderSuccess by remember { mutableStateOf(false) }

            // Errors
            var pickError by remember { mutableStateOf("") }
            var dropError by remember { mutableStateOf("") }

            val context = LocalContext.current

            if (showOrderSuccess && selectedProduct != null) {
                AlertDialog(
                    onDismissRequest = { showOrderSuccess = false },
                    title = {
                        Text(
                            text = "Order Placed Successfully!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column {
                            Text("Secure order confirmation:\n", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("• Item: ${selectedProduct?.productName ?: "Product"}", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Quantity: $quantity", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Pickup: ${SecurityUtils.sanitizeInput(pickPoint)}", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Delivery: ${SecurityUtils.sanitizeInput(dropPoint)}", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("• Instructions: ${if (instructions.isEmpty()) "None" else SecurityUtils.sanitizeInput(instructions)}", color = Color(0xFF475569), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Payment processed securely via AutoPay token authorization.", color = Color(0xFF22C55E), fontSize = 12.sp, fontStyle = FontStyle.Italic)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOrderSuccess = false
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1D8D))
                        ) {
                            Text("Awesome")
                        }
                    }
                )
            }

            OrderProductScreenView(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                isLoading = isLoading,
                selectedProduct = selectedProduct,
                quantity = quantity,
                onQuantityChange = { quantity = it },
                pickPoint = pickPoint,
                onPickChange = { pickPoint = it },
                dropPoint = dropPoint,
                onDropChange = { dropPoint = it },
                instructions = instructions,
                onInstructionsChange = { instructions = it },
                autopayEnabled = autopayEnabled,
                onAutopayChange = { autopayEnabled = it },
                pickError = pickError,
                dropError = dropError,
                onBackClick = { finish() },
                onSearchClick = {
                    if (searchQuery.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter a product name to search", Toast.LENGTH_SHORT).show()
                    } else if (SecurityUtils.containsInjectionPatterns(searchQuery)) {
                        Toast.makeText(context, "Security Threat: Injection detected!", Toast.LENGTH_LONG).show()
                    } else {
                        isLoading = true
                        selectedProduct = null
                        val sanitized = SecurityUtils.sanitizeInput(searchQuery.trim())

                        api.searchProducts(sanitized).enqueue(object : Callback<SearchResponse> {
                            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                                isLoading = false
                                val products = response.body()?.products
                                if (response.isSuccessful && !products.isNullOrEmpty()) {
                                    val product = products.firstOrNull { !it.imageUrl.isNullOrEmpty() } ?: products.first()
                                    selectedProduct = product
                                    quantity = 1
                                } else {
                                    loadMockProduct(sanitized) { mock -> selectedProduct = mock; quantity = 1 }
                                }
                            }

                            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                                isLoading = false
                                loadMockProduct(sanitized) { mock -> selectedProduct = mock; quantity = 1 }
                            }
                        })
                    }
                },
                onSubmitClick = {
                    var valid = true
                    pickError = ""
                    dropError = ""

                    if (SecurityUtils.containsInjectionPatterns(pickPoint) ||
                        SecurityUtils.containsInjectionPatterns(dropPoint) ||
                        SecurityUtils.containsInjectionPatterns(instructions)
                    ) {
                        Toast.makeText(context, "Security Threat: Injection detected!", Toast.LENGTH_LONG).show()
                        valid = false
                    }

                    if (pickPoint.trim().isEmpty()) {
                        pickError = "Pick up point is required"
                        valid = false
                    }
                    if (dropPoint.trim().isEmpty()) {
                        dropError = "Drop point is required"
                        valid = false
                    }

                    if (valid) {
                        showOrderSuccess = true
                    }
                }
            )
        }
    }

    private fun loadMockProduct(query: String, onMockLoaded: (Product) -> Unit) {
        val mockName = query.replaceFirstChar { it.uppercase() }
        val mockImageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop"
        val mockProduct = Product(
            code = "1234567890",
            productName = mockName,
            brands = "Campus Quality Checked",
            imageUrl = mockImageUrl
        )
        Toast.makeText(this, "Using secure mock search data (Offline Fallback)", Toast.LENGTH_SHORT).show()
        onMockLoaded(mockProduct)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderProductScreenView(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    selectedProduct: Product?,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    pickPoint: String,
    onPickChange: (String) -> Unit,
    dropPoint: String,
    onDropChange: (String) -> Unit,
    instructions: String,
    onInstructionsChange: (String) -> Unit,
    autopayEnabled: Boolean,
    onAutopayChange: (Boolean) -> Unit,
    pickError: String,
    dropError: String,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val primaryColor = Color(0xFF5C1D8D)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order a Product", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryColor,
                    navigationIconContentColor = primaryColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search food or products (e.g. cookies)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSearchClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            }

            if (!isLoading && selectedProduct == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search for a product above to place an order.",
                        color = textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Animated visibility for Product Details Card
            AnimatedVisibility(visible = !isLoading && selectedProduct != null) {
                if (selectedProduct != null) {
                    Column {
                        // Product Card Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF1F5F9))
                                ) {
                                    AsyncImage(
                                        model = selectedProduct.imageUrl,
                                        contentDescription = selectedProduct.productName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = selectedProduct.productName ?: "Product",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textPrimary
                                )

                                Text(
                                    text = "Brand: ${selectedProduct.brands ?: "Generic"}",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Quantity adjustment stepper
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Select Quantity", fontWeight = FontWeight.Bold, color = textPrimary)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                            modifier = Modifier.size(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }

                                        Text(
                                            text = quantity.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = textPrimary
                                        )

                                        Button(
                                            onClick = { if (quantity < 99) onQuantityChange(quantity + 1) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                            modifier = Modifier.size(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Checkout Card View Form
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Delivery Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textPrimary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = pickPoint,
                                    onValueChange = onPickChange,
                                    label = { Text("Pick Up Location (e.g. Campus Store)") },
                                    isError = pickError.isNotEmpty(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        focusedLabelColor = primaryColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (pickError.isNotEmpty()) {
                                    Text(
                                        text = pickError,
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                OutlinedTextField(
                                    value = dropPoint,
                                    onValueChange = onDropChange,
                                    label = { Text("Drop Off Spot (e.g. Block C Room 204)") },
                                    isError = dropError.isNotEmpty(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        focusedLabelColor = primaryColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (dropError.isNotEmpty()) {
                                    Text(
                                        text = dropError,
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                OutlinedTextField(
                                    value = instructions,
                                    onValueChange = onInstructionsChange,
                                    label = { Text("Delivery Instructions (Optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        focusedLabelColor = primaryColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // AutoPay Switch Gated Option
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Authorize AutoPay", fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text("Toggle switch to approve secure payments", fontSize = 11.sp, color = textSecondary)
                                    }

                                    Switch(
                                        checked = autopayEnabled,
                                        onCheckedChange = onAutopayChange,
                                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = onSubmitClick,
                                    enabled = autopayEnabled,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text("Place Secure Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
