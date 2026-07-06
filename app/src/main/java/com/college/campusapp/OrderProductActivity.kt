package com.college.campusapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.college.campusapp.api.Product
import com.college.campusapp.security.SecurityUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

// ============================================================================
// DESIGN SYSTEM — "Campus Speed"
// A quick-commerce identity built around the app's real job: getting hostel
// snacks delivered fast. Electric indigo carries the brand, sunburst yellow
// carries urgency/speed, mint carries money & success. Product badges keep
// their real-brand colors (Oreo blue, Lay's yellow, Coke red, etc.) — that's
// the one signature flourish worth keeping bold everywhere else is quiet.
// ============================================================================
object AppTheme {
    val Background = Color(0xFFF6F7FC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEEF0F9)
    val SurfaceSunken = Color(0xFFF1F2F9)

    val InkPrimary = Color(0xFF15172E)
    val InkSecondary = Color(0xFF6D7091)
    val InkTertiary = Color(0xFFA6A8C4)

    val Primary = Color(0xFF0EA5E9)       // Premium Light Blue — brand
    val PrimaryDark = Color(0xFF0284C7)   // Deep Sky Blue
    val PrimarySoft = Color(0xFFE0F2FE)   // Soft Sky Blue

    val Accent = Color(0xFFFFB627)        // Sunburst — speed / energy
    val AccentSoft = Color(0xFFFFF3DC)

    val Success = Color(0xFF12B886)       // Mint — wallet / money
    val SuccessSoft = Color(0xFFDFF7EE)

    val Danger = Color(0xFFF03E5C)
    val DangerSoft = Color(0xFFFDE7EA)

    val DividerColor = Color(0xFFE7E8F3)

    val PrimaryGradient = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)))
    val AccentGradient = Brush.linearGradient(listOf(Color(0xFFFFC94A), Color(0xFFFF9A3D)))
    val HeroGradient = Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF0EA5E9)))

    val CardShape = RoundedCornerShape(18.dp)
    val ChipShape = RoundedCornerShape(50)
    val FieldShape = RoundedCornerShape(14.dp)
    val ButtonShape = RoundedCornerShape(50)
}

class OrderProductActivity : ComponentActivity() {

    private val httpClient by lazy { okhttp3.OkHttpClient() }
    private var cachedCatalog: List<Product> = emptyList()

    companion object {
        private const val GOOGLE_SHEETS_CSV_URL = "" // PUBLISH GOOGLE SHEET AS CSV AND PASTE HERE
        
        // Global static in-memory references to preserve cart contents across back presses
        val globalCart = mutableStateMapOf<Product, Int>()
        val globalPriceConfigs = mutableStateMapOf<String, PriceConfig>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCatalog()

        setContent {
            val primaryColor = AppTheme.Primary
            val textPrimary = AppTheme.InkPrimary
            val textSecondary = AppTheme.InkSecondary
            
            var searchQuery by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
            
            // Cart State Map (Reference global map to persist across back/forward navigation)
            val cart = remember { globalCart }
            val priceConfigs = remember { globalPriceConfigs }
            var overBudgetPolicy by remember { mutableStateOf("cancel") } // "cancel" or "buffer"
            var walletBalance by remember { mutableStateOf(0) }
            
            var pickPoint by remember { mutableStateOf("") }
            var dropPoint by remember { mutableStateOf("") }
            var instructions by remember { mutableStateOf("") }
            var autopayEnabled by remember { mutableStateOf(false) }
            var showOrderSuccess by remember { mutableStateOf(false) }
            var showUnknownWarning by remember { mutableStateOf(false) }

            // Errors
            var pickError by remember { mutableStateOf("") }
            var dropError by remember { mutableStateOf("") }

            val context = LocalContext.current

            if (showOrderSuccess && cart.isNotEmpty()) {
                // Calculate prices for the receipt
                var receiptFixedTotal = 0
                var receiptEstMinTotal = 0
                var receiptEstMaxTotal = 0
                
                cart.forEach { (prod, qty) ->
                    val config = priceConfigs[prod.code ?: ""]
                    if (config != null) {
                        if (config.isUnknown) {
                            receiptEstMinTotal += config.rangeMin * qty
                            receiptEstMaxTotal += config.rangeMax * qty
                        } else {
                            val exact = config.exactPrice ?: 0
                            receiptFixedTotal += exact * qty
                        }
                    }
                }

                val rMinSubtotal = receiptFixedTotal + receiptEstMinTotal
                val rMaxSubtotal = receiptFixedTotal + receiptEstMaxTotal
                val rTotalQty = cart.values.sum()

                val rDelMin = (20 + (if (rMinSubtotal < 100) 5 else 0) + (if (rTotalQty > 3) (rTotalQty - 3) * 2 else 0)).coerceIn(20, 30)
                val rDelMax = (20 + (if (rMaxSubtotal < 100) 5 else 0) + (if (rTotalQty > 3) (rTotalQty - 3) * 2 else 0)).coerceIn(20, 30)
                
                val rHandling = 2
                val rPremium = 10

                val rTotalMin = rMinSubtotal + rDelMin + rHandling + rPremium
                val rTotalMax = rMaxSubtotal + rDelMax + rHandling + rPremium

                Dialog(
                    onDismissRequest = {
                        showOrderSuccess = false
                        cart.clear()
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp), // Expand card wider on screen
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp), // Generous spacing for a spacious feel
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Minimalist Tick Symbol in a Circle
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFECFDF5), CircleShape) // Soft success green fill
                                    .border(1.5.dp, Color(0xFF10B981), CircleShape), // Success green border
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Your Order Successful",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp,
                                color = AppTheme.InkPrimary,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // The Minimalist Invoice Card (combines Address & Payments)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, AppTheme.DividerColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // 1. Delivery Info Section
                                    Text(
                                        text = "DELIVERY DETAILS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = AppTheme.InkTertiary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "From: $pickPoint",
                                        fontSize = 12.sp,
                                        color = AppTheme.InkPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "To: $dropPoint",
                                        fontSize = 12.sp,
                                        color = AppTheme.InkPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = AppTheme.DividerColor)
                                    
                                    // 2. Items List Section
                                    Text(
                                        text = "ITEMS ORDERED",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = AppTheme.InkTertiary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 100.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        cart.forEach { (prod, qty) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.productName ?: "Product",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = textPrimary
                                                    )
                                                    Text(
                                                        text = "Qty: $qty",
                                                        fontSize = 10.sp,
                                                        color = textSecondary
                                                    )
                                                }
                                                val config = priceConfigs[prod.code ?: ""]
                                                val priceText = if (config != null) {
                                                    if (config.isUnknown) {
                                                        "₹${config.rangeMin * qty} - ₹${config.rangeMax * qty}"
                                                    } else {
                                                        "₹${(config.exactPrice ?: 0) * qty}"
                                                    }
                                                } else {
                                                    "₹0"
                                                }
                                                Text(
                                                    text = priceText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = textPrimary
                                                )
                                            }
                                        }
                                    }
                                    
                                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = AppTheme.DividerColor)
                                    
                                    // 3. Payment Details Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subtotal", fontSize = 11.sp, color = textSecondary)
                                        val subtotalText = if (rMinSubtotal == rMaxSubtotal) "₹$rMinSubtotal" else "₹$rMinSubtotal - ₹$rMaxSubtotal"
                                        Text(subtotalText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Delivery & Fees", fontSize = 11.sp, color = textSecondary)
                                        val feesMin = rDelMin + rHandling + rPremium
                                        val feesMax = rDelMax + rHandling + rPremium
                                        val feesText = if (feesMin == feesMax) "₹$feesMin" else "₹$feesMin - ₹$feesMax"
                                        Text(feesText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                                    }
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = AppTheme.DividerColor)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total Expected Pay", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                        val totalText = if (rTotalMin == rTotalMax) "₹$rTotalMin" else "₹$rTotalMin - ₹$rTotalMax"
                                        Text(totalText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = primaryColor)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    showOrderSuccess = false
                                    cart.clear()
                                },
                                shape = AppTheme.ButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Continue Shopping", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (showUnknownWarning) {
                AlertDialog(
                    onDismissRequest = { showUnknownWarning = false },
                    shape = AppTheme.CardShape,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AppTheme.AccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚠️", fontSize = 22.sp)
                        }
                    },
                    title = {
                        Text(
                            text = "Add a fixed-price item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = AppTheme.InkPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = "At least one item in your basket needs a known price before you can place this order.",
                            color = AppTheme.InkSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showUnknownWarning = false },
                            shape = AppTheme.ButtonShape,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary)
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            OrderProductScreenView(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                isLoading = isLoading,
                searchResults = searchResults,
                cart = cart,
                priceConfigs = priceConfigs,
                overBudgetPolicy = overBudgetPolicy,
                onPolicyChange = { overBudgetPolicy = it },
                walletBalance = walletBalance,
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
                    val preprocessed = searchQuery.trim().replace(Regex("\\s+"), " ")
                    if (preprocessed.isEmpty()) {
                        Toast.makeText(context, "Please enter a product name to search", Toast.LENGTH_SHORT).show()
                    } else if (SecurityUtils.containsInjectionPatterns(preprocessed)) {
                        Toast.makeText(context, "Security Threat: Injection detected!", Toast.LENGTH_LONG).show()
                    } else {
                        isLoading = true
                        searchResults = emptyList()
                        val sanitized = SecurityUtils.sanitizeInput(preprocessed)
                        val normalized = normalizeQuery(sanitized)

                        // Load catalog if empty
                        if (cachedCatalog.isEmpty()) {
                            try {
                                val assetStream = assets.open("catalog.csv")
                                cachedCatalog = parseCsv(assetStream)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Search in-memory catalog
                        val results = cachedCatalog.filter {
                            it.productName?.lowercase()?.contains(normalized.lowercase()) == true ||
                            it.brands?.lowercase()?.contains(normalized.lowercase()) == true
                        }

                        // Split-word search fallback
                        val finalResults = if (results.isNotEmpty()) {
                            results
                        } else {
                            val words = normalized.split(" ").filter { it.length > 2 }
                            if (words.isNotEmpty()) {
                                val firstWord = words.first()
                                cachedCatalog.filter {
                                    it.productName?.lowercase()?.contains(firstWord.lowercase()) == true ||
                                    it.brands?.lowercase()?.contains(firstWord.lowercase()) == true
                                }
                            } else {
                                emptyList()
                            }
                        }

                        isLoading = false
                        searchResults = finalResults

                        if (finalResults.isEmpty()) {
                            Toast.makeText(context, "No matching products found", Toast.LENGTH_SHORT).show()
                        }
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
                    if (valid && pickPoint.trim().equals(dropPoint.trim(), ignoreCase = true)) {
                        pickError = "Pickup and Drop spot cannot be the same"
                        dropError = "Pickup and Drop spot cannot be the same"
                        valid = false
                    }

                    if (valid) {
                        val allUnknown = cart.keys.all { prod ->
                            val config = priceConfigs[prod.code ?: ""]
                            config == null || config.isUnknown
                        }
                        if (allUnknown) {
                            showUnknownWarning = true
                        } else {
                            showOrderSuccess = true
                        }
                    }
                }
            )
        }
    }

    private fun loadCatalog() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (GOOGLE_SHEETS_CSV_URL.isNotEmpty()) {
                    val request = okhttp3.Request.Builder()
                        .url(GOOGLE_SHEETS_CSV_URL)
                        .header("User-Agent", "CampusApp/1.0 (Android; contact@campusapp.com)")
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bodyStream = response.body?.byteStream()
                        if (bodyStream != null) {
                            cachedCatalog = parseCsv(bodyStream)
                            return@launch
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            try {
                val assetStream = assets.open("catalog.csv")
                cachedCatalog = parseCsv(assetStream)
                android.util.Log.d("CampusApp", "Loaded ${cachedCatalog.size} products from CSV.")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseCsv(inputStream: InputStream): List<Product> {
        val list = mutableListOf<Product>()
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = splitCsvLine(line ?: "")
                if (tokens.size >= 4) {
                    list.add(Product(
                        code = tokens[0].trim(),
                        productName = tokens[1].trim(),
                        brands = tokens[2].trim(),
                        imageUrl = tokens[3].trim()
                    ))
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = java.lang.StringBuilder()
        var inQuotes = false
        for (ch in line.toCharArray()) {
            if (inQuotes) {
                if (ch == '\"') {
                    inQuotes = false
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = java.lang.StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
        }
        result.add(curVal.toString())
        return result
    }

    private fun normalizeQuery(query: String): String {
        val lowercase = query.lowercase()
        return when {
            lowercase.contains("cookie") -> {
                query.replace("cookies", "biscuits", ignoreCase = true)
                    .replace("cookie", "biscuit", ignoreCase = true)
            }
            lowercase.contains("cold drink") -> {
                query.replace("cold drink", "drink", ignoreCase = true)
            }
            lowercase.contains("soft drink") -> {
                query.replace("soft drink", "drink", ignoreCase = true)
            }
            lowercase == "biscuit" -> "biscuits"
            else -> query
        }
    }
}

data class CategoryStyle(
    val startColor: Color,
    val endColor: Color,
    val iconResId: Int
)

fun getCategoryStyle(productCode: String?): CategoryStyle {
    val code = productCode?.lowercase() ?: ""
    return when {
        // --- BISCUITS & COOKIES (Different backgrounds per cookie brand!) ---
        code.contains("oreo_original") -> CategoryStyle(
            startColor = Color(0xFF0D47A1), // Oreo Dark Blue
            endColor = Color(0xFF1E88E5),   // Oreo Royal Blue
            iconResId = R.drawable.ic_oreo
        )
        code.contains("oreo_strawberry") -> CategoryStyle(
            startColor = Color(0xFFD81B60), // Strawberry Pink
            endColor = Color(0xFFF48FB1),   // Soft Pink
            iconResId = R.drawable.ic_oreo
        )
        code.contains("bourbon") -> CategoryStyle(
            startColor = Color(0xFF3E2723), // Deep Chocolate Brown
            endColor = Color(0xFF5D4037),   // Milk Chocolate
            iconResId = R.drawable.ic_bourbon
        )
        code.contains("goodday") -> CategoryStyle(
            startColor = Color(0xFFFF8F00), // Butter Amber
            endColor = Color(0xFFFFC107),   // Cookie Gold
            iconResId = R.drawable.ic_biscuit
        )
        code.contains("parleg") -> CategoryStyle(
            startColor = Color(0xFF7CB342), // Parle-G Leaf Green
            endColor = Color(0xFF9CCC65),   // Soft Green
            iconResId = R.drawable.ic_biscuit
        )
        code.contains("hideandseek") -> CategoryStyle(
            startColor = Color(0xFF263238), // Dark Slate
            endColor = Color(0xFF455A64),   // Chocolate Chip Grey
            iconResId = R.drawable.ic_biscuit
        )
        code.contains("marie") -> CategoryStyle(
            startColor = Color(0xFFE65100), // Golden Orange
            endColor = Color(0xFFFFB74D),   // Light Gold Marie
            iconResId = R.drawable.ic_biscuit
        )
        code.contains("darkfantasy") -> CategoryStyle(
            startColor = Color(0xFF1A1A1A), // Luxury Dark Black
            endColor = Color(0xFF3A3A3A),   // Dark Grey
            iconResId = R.drawable.ic_biscuit
        )
        code.contains("jimjam") -> CategoryStyle(
            startColor = Color(0xFFC2185B), // Jam Red
            endColor = Color(0xFFE91E63),   // Strawberry Red
            iconResId = R.drawable.ic_biscuit
        )
        code.contains("monaco") || code.contains("krackjack") -> CategoryStyle(
            startColor = Color(0xFFF57C00), // Salty Gold
            endColor = Color(0xFFFFB74D),   // Light Yellow
            iconResId = R.drawable.ic_biscuit
        )

        // --- CHIPS & SNACKS (Different backgrounds per chips brand!) ---
        code.contains("lays_salted") -> CategoryStyle(
            startColor = Color(0xFFFBC02D), // Lays Yellow
            endColor = Color(0xFFFFF176),   // Bright Yellow
            iconResId = R.drawable.ic_lays
        )
        code.contains("lays_masala") -> CategoryStyle(
            startColor = Color(0xFF1A237E), // Magic Masala Dark Blue
            endColor = Color(0xFF3F51B5),   // Royal Blue
            iconResId = R.drawable.ic_lays
        )
        code.contains("lays_onion") -> CategoryStyle(
            startColor = Color(0xFF00796B), // Cream & Onion Green
            endColor = Color(0xFF26A69A),   // Soft Mint Green
            iconResId = R.drawable.ic_lays
        )
        code.contains("kurkure") -> CategoryStyle(
            startColor = Color(0xFFBF360C), // Kurkure Spicy Red-Orange
            endColor = Color(0xFFFF5722),   // Flame Orange
            iconResId = R.drawable.ic_chips
        )
        code.contains("pringles_salted") -> CategoryStyle(
            startColor = Color(0xFFB71C1C), // Pringles Red
            endColor = Color(0xFFE53935),   // Bright Red
            iconResId = R.drawable.ic_chips
        )
        code.contains("pringles_sourcream") -> CategoryStyle(
            startColor = Color(0xFF2E7D32), // Pringles Green
            endColor = Color(0xFF4CAF50),   // Green
            iconResId = R.drawable.ic_chips
        )
        code.contains("doritos_cheese") -> CategoryStyle(
            startColor = Color(0xFFFF6D00), // Nacho Orange
            endColor = Color(0xFFFFAB40),   // Cheese Yellow
            iconResId = R.drawable.ic_chips
        )
        code.contains("doritos_chilli") -> CategoryStyle(
            startColor = Color(0xFF880E4F), // Sweet Chilli Maroon
            endColor = Color(0xFFAD1457),   // Soft Maroon
            iconResId = R.drawable.ic_chips
        )

        // --- DRINKS & SODA (Different backgrounds per drink!) ---
        code.contains("coke") -> CategoryStyle(
            startColor = Color(0xFFB71C1C), // Coca-Cola Red
            endColor = Color(0xFFE53935),   // Cola Red
            iconResId = R.drawable.ic_coke
        )
        code.contains("thumsup") -> CategoryStyle(
            startColor = Color(0xFF0D1B2A), // Thums Up Dark Night Blue
            endColor = Color(0xFF1B263B),   // Dark Blue
            iconResId = R.drawable.ic_soda
        )
        code.contains("sprite") -> CategoryStyle(
            startColor = Color(0xFF00C853), // Sprite Green
            endColor = Color(0xFF64DD17),   // Lemon Yellow-Green
            iconResId = R.drawable.ic_soda
        )
        code.contains("fanta") -> CategoryStyle(
            startColor = Color(0xFFFF6D00), // Fanta Orange
            endColor = Color(0xFFFF9100),   // Orange
            iconResId = R.drawable.ic_soda
        )
        code.contains("redbull") -> CategoryStyle(
            startColor = Color(0xFF0D47A1), // Red Bull Blue
            endColor = Color(0xFFFFD54F),   // Energy Gold Yellow
            iconResId = R.drawable.ic_soda
        )
        code.contains("sting") -> CategoryStyle(
            startColor = Color(0xFFC2185B), // Sting Pink-Red
            endColor = Color(0xFFE91E63),   // Bright Pink
            iconResId = R.drawable.ic_soda
        )
        code.contains("monster") -> CategoryStyle(
            startColor = Color(0xFF1A1A1A), // Monster Black
            endColor = Color(0xFF76FF03),   // Lime Green
            iconResId = R.drawable.ic_soda
        )

        // --- DETAILED BRAND MAPPINGS ---
        code.contains("maggi") -> CategoryStyle(
            startColor = Color(0xFFFFD600), // Maggi Yellow
            endColor = Color(0xFFDD2C00),   // Maggi Red
            iconResId = R.drawable.ic_maggi
        )
        code.contains("cadbury") || code.contains("dairy milk") || code.contains("kitkat") || code.contains("snickers") || code.contains("5 star") || code.contains("perk") || code.contains("fuse") || code.contains("gems") -> CategoryStyle(
            startColor = Color(0xFF311B92), // Cadbury Purple
            endColor = Color(0xFF5E35B1),
            iconResId = R.drawable.ic_cadbury
        )
        code.contains("amul") -> CategoryStyle(
            startColor = Color(0xFF0D47A1), // Amul Blue
            endColor = Color(0xFF1976D2),
            iconResId = R.drawable.ic_amul
        )
        code.contains("colgate") -> CategoryStyle(
            startColor = Color(0xFFD50000), // Colgate Red
            endColor = Color(0xFFE53935),
            iconResId = R.drawable.ic_colgate
        )
        code.contains("dettol") -> CategoryStyle(
            startColor = Color(0xFF004D40), // Dettol Green
            endColor = Color(0xFF00796B),
            iconResId = R.drawable.ic_dettol
        )

        // --- GENERAL FALLBACKS BY CATEGORY ---
        code.startsWith("biscuit") -> CategoryStyle(
            startColor = Color(0xFF78350F),
            endColor = Color(0xFFB45309),
            iconResId = R.drawable.ic_biscuit
        )
        code.startsWith("chips") || code.startsWith("namkeen") || code.startsWith("popcorn") -> CategoryStyle(
            startColor = Color(0xFFB45309),
            endColor = Color(0xFFF59E0B),
            iconResId = R.drawable.ic_chips
        )
        code.startsWith("noodles") || code.startsWith("soup") -> CategoryStyle(
            startColor = Color(0xFFDC2626),
            endColor = Color(0xFFF59E0B),
            iconResId = R.drawable.ic_meal
        )
        code.startsWith("choc") -> CategoryStyle(
            startColor = Color(0xFF4C1D95),
            endColor = Color(0xFF7C3AED),
            iconResId = R.drawable.ic_biscuit
        )
        code.startsWith("drink") || code.startsWith("juice") || code.startsWith("coffee") -> CategoryStyle(
            startColor = Color(0xFF1D4ED8),
            endColor = Color(0xFF3B82F6),
            iconResId = R.drawable.ic_soda
        )
        code.startsWith("fresh") || code.startsWith("canteen") -> CategoryStyle(
            startColor = Color(0xFFEA580C),
            endColor = Color(0xFFF97316),
            iconResId = R.drawable.ic_meal
        )
        code.startsWith("stat") || code.startsWith("util") || code.startsWith("elec") -> CategoryStyle(
            startColor = Color(0xFF0F766E),
            endColor = Color(0xFF14B8A6),
            iconResId = R.drawable.ic_assignment
        )
        code.startsWith("hygiene") || code.startsWith("cleaner") -> CategoryStyle(
            startColor = Color(0xFF0891B2),
            endColor = Color(0xFF06B6D4),
            iconResId = R.drawable.ic_soap
        )
        code.startsWith("med") -> CategoryStyle(
            startColor = Color(0xFFDC2626),
            endColor = Color(0xFFEF4444),
            iconResId = R.drawable.ic_medical
        )
        else -> CategoryStyle(
            startColor = Color(0xFF475569),
            endColor = Color(0xFF64748B),
            iconResId = R.drawable.ic_order
        )
    }
}

fun getBrandAbbreviation(product: Product): String {
    val name = product.productName?.lowercase() ?: ""
    val brand = product.brands?.lowercase() ?: ""
    return when {
        name.contains("oreo") -> "OREO"
        name.contains("bourbon") -> "BOURBON"
        name.contains("goodday") || name.contains("good day") -> "GOOD DAY"
        name.contains("parle-g") || name.contains("parleg") || name.contains("parle g") -> "PARLE-G"
        name.contains("hide & seek") || name.contains("hideandseek") -> "HIDE & SEEK"
        name.contains("marie") -> "MARIE"
        name.contains("dark fantasy") || name.contains("darkfantasy") -> "DARK FANTASY"
        name.contains("lays") -> "LAY'S"
        name.contains("kurkure") -> "KURKURE"
        name.contains("pringles") -> "PRINGLES"
        name.contains("coke") || name.contains("coca-cola") -> "COKE"
        name.contains("thums up") || name.contains("thumsup") -> "THUMS UP"
        name.contains("sprite") -> "SPRITE"
        name.contains("fanta") -> "FANTA"
        name.contains("red bull") || name.contains("redbull") -> "RED BULL"
        name.contains("sting") -> "STING"
        name.contains("monster") -> "MONSTER"
        name.contains("maggi") -> "MAGGI"
        name.contains("yippee") -> "YIPPEE"
        name.contains("chings") || name.contains("ching's") -> "CHING'S"
        name.contains("knorr") -> "KNORR"
        name.contains("dettol") -> "DETTOL"
        name.contains("dove") -> "DOVE"
        name.contains("himalaya") -> "HIMALAYA"
        name.contains("colgate") -> "COLGATE"
        name.contains("clinic plus") || name.contains("clinicplus") -> "CLINIC PLUS"
        name.contains("nivea") -> "NIVEA"
        name.contains("axe") -> "AXE"
        name.contains("vaseline") -> "VASELINE"
        name.contains("dolo") -> "DOLO"
        name.contains("eno") -> "ENO"
        name.contains("band-aid") || name.contains("bandaid") -> "BAND-AID"
        name.contains("volini") -> "VOLINI"
        name.contains("vicks") -> "VICKS"
        name.contains("bournvita") -> "BOURNVITA"
        name.contains("nutella") -> "NUTELLA"
        name.contains("peanut butter") -> "PEANUT BUTTER"
        name.contains("samosa") -> "SAMOSA"
        name.contains("chai") -> "CHAI"
        brand.isNotEmpty() -> brand.split("/").first().trim().uppercase()
        else -> "CAMPUS"
    }
}

fun getProductPrice(product: Product): Int {
    val code = product.code?.lowercase() ?: ""
    return when {
        code.startsWith("biscuit") -> 30
        code.startsWith("chips") -> 20
        code.startsWith("namkeen") -> 45
        code.startsWith("popcorn") -> 60
        code.startsWith("noodles") -> 15
        code.startsWith("soup") -> 55
        code.startsWith("choc") -> 40
        code.startsWith("drink") -> 40
        code.startsWith("juice") -> 50
        code.startsWith("coffee") -> 60
        code.startsWith("fresh") || code.startsWith("canteen") -> 50
        code.startsWith("stat") -> 40
        code.startsWith("util") -> 199
        code.startsWith("elec") -> 299
        code.startsWith("hygiene") -> 75
        code.startsWith("cleaner") -> 60
        code.startsWith("med") -> 35
        code.startsWith("supplement") -> 250
        code.startsWith("spread") -> 199
        code.startsWith("staple") -> 45
        code.startsWith("sauce") -> 50
        else -> 30
    }
}

fun getProductPriceDisplay(product: Product, config: PriceConfig?): String {
    if (config == null) return ""
    return if (config.isUnknown) {
        "Range: ₹${config.rangeMin} - ₹${config.rangeMax}"
    } else {
        "₹${config.exactPrice ?: 0}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderProductScreenView(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    searchResults: List<Product>,
    cart: MutableMap<Product, Int>,
    priceConfigs: MutableMap<String, PriceConfig>,
    overBudgetPolicy: String,
    onPolicyChange: (String) -> Unit,
    walletBalance: Int,
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
    var showCheckout by remember { mutableStateOf(false) }

    // Custom Item Request State
    var showCustomDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customBrand by remember { mutableStateOf("") }
    var customPrice by remember { mutableStateOf("") }
    var customNameError by remember { mutableStateOf(false) }

    // Price Configuration State
    var priceConfigProduct by remember { mutableStateOf<Product?>(null) }
    var priceConfigIsUnknown by remember { mutableStateOf(true) }
    var priceConfigExact by remember { mutableStateOf("") }
    var priceConfigRange by remember { mutableStateOf(0f..300f) }

    val primaryColor = AppTheme.Primary
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary

    // Price Configuration Dialog
    if (priceConfigProduct != null) {
        val prod = priceConfigProduct!!
        AlertDialog(
            onDismissRequest = { 
                val code = prod.code ?: ""
                if (!priceConfigs.containsKey(code)) {
                    priceConfigs[code] = PriceConfig(isUnknown = true, rangeMin = 0, rangeMax = 200)
                }
                priceConfigProduct = null 
            },
            shape = AppTheme.CardShape,
            title = {
                Text(
                    text = "Set Price Reference",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        text = "Estimate price for ${prod.productName} to help your delivery rider:",
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F5F0), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { priceConfigIsUnknown = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!priceConfigIsUnknown) primaryColor else Color.Transparent,
                                contentColor = if (!priceConfigIsUnknown) Color.White else textSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("I Know Price", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { priceConfigIsUnknown = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (priceConfigIsUnknown) primaryColor else Color.Transparent,
                                contentColor = if (priceConfigIsUnknown) Color.White else textSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Price Unknown", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!priceConfigIsUnknown) {
                        OutlinedTextField(
                            value = priceConfigExact,
                            onValueChange = { priceConfigExact = it.filter { char -> char.isDigit() } },
                            label = { Text("Enter Exact Price (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = AppTheme.FieldShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFECE7E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val maxVal = (priceConfigRange.endInclusive / 5).toInt() * 5
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated Price Limit:", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 13.sp)
                            Text("₹$maxVal", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Minus stepper button
                                Card(
                                    onClick = {
                                        val newVal = (priceConfigRange.endInclusive - 10f).coerceAtLeast(0f)
                                        priceConfigRange = 0f..newVal
                                    },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, AppTheme.DividerColor),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("-", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                    }
                                }

                                // Single-thumb Slider
                                Slider(
                                    value = priceConfigRange.endInclusive,
                                    onValueChange = { priceConfigRange = 0f..it },
                                    valueRange = 0f..1000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = primaryColor,
                                        inactiveTrackColor = primaryColor.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                // Plus stepper button
                                Card(
                                    onClick = {
                                        val newVal = (priceConfigRange.endInclusive + 10f).coerceAtMost(1000f)
                                        priceConfigRange = 0f..newVal
                                    },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, AppTheme.DividerColor),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("+", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
Spacer(modifier = Modifier.height(12.dp))
                        
                        // Premium Wallet Info Banner inside Dialog
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5)), // Soft mint border
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // Soft mint background
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Any surplus money from estimates will be instantly refunded to your Wallet once the rider buys the item.",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF065F46) // Dark green text
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left: Cancel Button (Outlined)
                    OutlinedButton(
                        onClick = {
                            val code = prod.code ?: ""
                            if (!priceConfigs.containsKey(code)) {
                                priceConfigs[code] = PriceConfig(isUnknown = true, rangeMin = 0, rangeMax = 200)
                            }
                            priceConfigProduct = null
                            priceConfigExact = ""
                            priceConfigRange = 0f..300f
                        },
                        shape = AppTheme.ButtonShape,
                        border = BorderStroke(1.dp, AppTheme.DividerColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Right: Save Button (Solid Blue)
                    Button(
                        onClick = {
                            val code = prod.code ?: ""
                            if (!priceConfigIsUnknown && priceConfigExact.trim().isNotEmpty()) {
                                priceConfigs[code] = PriceConfig(
                                    isUnknown = false,
                                    exactPrice = priceConfigExact.trim().toInt()
                                )
                            } else {
                                val minValVal = (priceConfigRange.start / 5).toInt() * 5
                                val maxValVal = (priceConfigRange.endInclusive / 5).toInt() * 5
                                priceConfigs[code] = PriceConfig(
                                    isUnknown = true,
                                    rangeMin = minValVal,
                                    rangeMax = maxValVal
                                )
                            }
                            priceConfigProduct = null
                            priceConfigExact = ""
                            priceConfigRange = 0f..300f
                        },
                        shape = AppTheme.ButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        )
    }

    // Custom Item Request Dialog (Centered Title, AppTheme Card shape, Balanced Side-by-Side buttons)
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCustomDialog = false
                customName = ""
                customBrand = ""
                customPrice = ""
                customNameError = false
            },
            shape = AppTheme.CardShape,
            title = { 
                Text(
                    text = "Request Custom Item", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            text = {
                Column {
                    Text(
                        text = "Need something not in the catalog? Tell your delivery rider what to purchase.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { 
                            customName = it
                            customNameError = false 
                        },
                        label = { Text("Product Name") },
                        placeholder = { Text("e.g. Axe Signature Perfume") },
                        isError = customNameError,
                        singleLine = true,
                        shape = AppTheme.FieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFECE7E1),
                            errorBorderColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (customNameError) {
                        Text("Product name is required", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = customBrand,
                        onValueChange = { customBrand = it },
                        label = { Text("Brand (Optional)") },
                        placeholder = { Text("e.g. Axe or Colgate") },
                        singleLine = true,
                        shape = AppTheme.FieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFECE7E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left: Cancel Button (Outlined, matches Design System)
                    OutlinedButton(
                        onClick = {
                            showCustomDialog = false
                            customName = ""
                            customBrand = ""
                            customPrice = ""
                            customNameError = false
                        },
                        shape = AppTheme.ButtonShape,
                        border = BorderStroke(1.dp, AppTheme.DividerColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Right: Add to Basket Button (Filled, Premium Light Blue)
                    Button(
                        onClick = {
                            if (customName.trim().isEmpty()) {
                                customNameError = true
                            } else {
                                val newProduct = Product(
                                    code = "custom_${System.currentTimeMillis()}",
                                    productName = customName.trim(),
                                    brands = if (customBrand.trim().isEmpty()) "Custom Request" else customBrand.trim(),
                                    imageUrl = ""
                                )
                                cart[newProduct] = 1
                                showCustomDialog = false
                                customName = ""
                                customBrand = ""
                                
                                // IMMEDIATELY TRIGGER THE PRICE CONFIG DIALOG FOR THIS NEW CUSTOM PRODUCT!
                                priceConfigProduct = newProduct
                                priceConfigIsUnknown = true
                                priceConfigExact = ""
                                priceConfigRange = 0f..300f
                            }
                        },
                        shape = AppTheme.ButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Add to Basket", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Products", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    WalletBadge(balance = walletBalance)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.Background,
                    titleContentColor = textPrimary,
                    navigationIconContentColor = primaryColor
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.Background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Welcome header section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Campus Store",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = textPrimary
                    )
                    Text(
                        text = "Get items delivered directly to your spot",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                                // Category Quick-Filter Chips (Option A - Gen Z Style)
                val categories = remember {
                    listOf(
                        "All 🛍️" to "",
                        "Snack Cravings 🤤" to "chips",
                        "Study Grind 📚" to "biscuit",
                        "Hostel Essentials 📦" to "soap",
                        "SOS Meds 🤒" to "med",
                        "Caffeine Fix ☕" to "drink"
                    )
                }
                var selectedCategory by remember { mutableStateOf(0) }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    itemsIndexed(categories) { idx, (label, query) ->
                        val isSelected = selectedCategory == idx
                        Card(
                            onClick = {
                                selectedCategory = idx
                                onQueryChange(query)
                                onSearchClick()
                            },
                            shape = AppTheme.ChipShape,
                            border = BorderStroke(1.dp, if (isSelected) primaryColor else AppTheme.DividerColor),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) AppTheme.PrimarySoft else AppTheme.Surface
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) primaryColor else textSecondary
                                )
                            }
                        }
                    }
                }

                val searchSuggestions = remember {
                    listOf(
                        "chips...",
                        "cookies...",
                        "sodas...",
                        "biscuits...",
                        "chocolates...",
                        "juices...",
                        "snacks...",
                        "ice cream...",
                        "perfumes...",
                        "maggi..."
                    )
                }
                var suggestionIndex by remember { mutableStateOf(0) }
                LaunchedEffect(searchQuery) {
                    if (searchQuery.isEmpty()) {
                        while (true) {
                            kotlinx.coroutines.delay(1000L)
                            suggestionIndex = (suggestionIndex + 1) % searchSuggestions.size
                        }
                    }
                }

                // Search Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Search for ",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                                AnimatedContent(
                                    targetState = searchSuggestions[suggestionIndex],
                                    transitionSpec = {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> -height } + fadeOut()
                                        )
                                    },
                                    label = "PlaceholderAnimation"
                                ) { productText ->
                                    Text(
                                        text = productText,
                                        color = Color(0xFF64748B), // Slightly darker for better visibility and contrast
                                        fontWeight = FontWeight.SemiBold, // Slightly bold for professional look
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = AppTheme.FieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AppTheme.Surface,
                            unfocusedContainerColor = AppTheme.Surface,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = AppTheme.DividerColor
                        ),
                        modifier = Modifier.weight(2f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onSearchClick,
                        shape = AppTheme.ButtonShape,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else {
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(bottom = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Center Content: Search Prompt & Request Button
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = { showCustomDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = AppTheme.ButtonShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .shadow(3.dp, AppTheme.ButtonShape)
                                        .background(AppTheme.PrimaryGradient, AppTheme.ButtonShape)
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_input_add),
                                        contentDescription = "Add",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Request Custom Item",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            // Bottom Aligned Content: Tagline in bigger, visible font
                            Text(
                                text = "Canteen items & snacks delivered to your doorstep 🛵",
                                color = AppTheme.InkSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp, // adjusted to prevent text wrapping on standard screens
                                maxLines = 1,      // force tagline and emoji to stay on the exact same line
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Select Products & Quantities:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textPrimary
                                    )
                                    TextButton(onClick = { showCustomDialog = true }) {
                                        Text("+ Add Custom Item", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 13.sp)
                                    }
                                }
                            }

                            items(searchResults) { product ->
                                val qty = cart[product] ?: 0
                                val config = priceConfigs[product.code ?: ""]
                                ProductRowItem(
                                    product = product,
                                    quantity = qty,
                                    priceDisplay = getProductPriceDisplay(product, config),
                                    onQuantityChange = { newQty ->
                                        if (newQty > 0) {
                                            cart[product] = newQty
                                            if (qty == 0) {
                                                // Trigger price reference dialog on initial ADD
                                                priceConfigProduct = product
                                                priceConfigIsUnknown = true
                                                priceConfigExact = ""
                                                priceConfigRange = 0f..300f
                                            }
                                        } else {
                                            cart.remove(product)
                                            priceConfigs.remove(product.code ?: "")
                                        }
                                    },
                                    onPriceLabelClick = {
                                        priceConfigProduct = product
                                        val existing = priceConfigs[product.code ?: ""]
                                        if (existing != null) {
                                            priceConfigIsUnknown = existing.isUnknown
                                            priceConfigExact = existing.exactPrice?.toString() ?: ""
                                            priceConfigRange = existing.rangeMin.toFloat()..existing.rangeMax.toFloat()
                                        } else {
                                            priceConfigIsUnknown = true
                                            priceConfigExact = ""
                                            priceConfigRange = 0f..300f
                                        }
                                    }
                                )
                            }


                        }
                    }
                }
            }

            // 1. Floating Cart Bar at the bottom
            val totalItems = cart.values.sum()

            if (totalItems > 0 && !showCheckout) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEC191919)), // Dark slate glassmorphism
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$totalItems Items in Basket",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { showCheckout = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // Emerald Green
                            shape = AppTheme.ButtonShape
                        ) {
                            Text("Proceed", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // 2. Slide-up/Drawer Checkout Sheet
            if (showCheckout) {
                // Semi-transparent dim background overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x7F000000))
                        .clickable { showCheckout = false }
                )

                // The Drawer container (Premium Overhauled Slide-up Sheet)
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.Background), // Soft slate background
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        // Premium Grab Handle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color(0xFFE2E8F0))
                                .clip(CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Review Order Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = AppTheme.InkPrimary
                            )
                            Card(
                                onClick = { showCheckout = false },
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, AppTheme.DividerColor),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.InkSecondary)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(top = 8.dp), color = AppTheme.DividerColor)

                        // Scrollable Area
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            // CARD 1: Basket Items List
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = AppTheme.CardShape,
                                border = BorderStroke(1.dp, AppTheme.DividerColor),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Basket Items",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppTheme.InkPrimary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    cart.forEach { (prod, qty) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = prod.productName ?: "Product",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = textPrimary
                                                )
                                                val config = priceConfigs[prod.code ?: ""]
                                                val desc = if (config != null) {
                                                    if (config.isUnknown) {
                                                        "Range: ₹${config.rangeMin} - ₹${config.rangeMax}"
                                                    } else {
                                                        "Price: ₹${config.exactPrice}"
                                                    }
                                                } else {
                                                    "Custom Item"
                                                }
                                                Text(
                                                    text = desc,
                                                    fontSize = 11.sp,
                                                    color = textSecondary
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Quantity Stepper Selector inside checkout basket list
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .border(1.dp, primaryColor, RoundedCornerShape(18.dp))
                                                    .background(Color.White, RoundedCornerShape(18.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                // Minus Button (Decrements or removes when it reaches 0)
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            if (qty > 1) {
                                                                cart[prod] = qty - 1
                                                            } else {
                                                                cart.remove(prod)
                                                                priceConfigs.remove(prod.code ?: "")
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "-",
                                                        color = primaryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                                
                                                // Quantity Text
                                                Text(
                                                    text = qty.toString(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = textPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                                
                                                // Plus Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            cart[prod] = qty + 1
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+",
                                                        color = primaryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 2: Delivery Details Form
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = AppTheme.CardShape,
                                border = BorderStroke(1.dp, AppTheme.DividerColor),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Delivery Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppTheme.InkPrimary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    OutlinedTextField(
                                        value = pickPoint,
                                        onValueChange = onPickChange,
                                        label = { Text("Pick Up Location") },
                                        placeholder = { Text("e.g. Campus Store", color = AppTheme.InkTertiary) },
                                        isError = pickError.isNotEmpty(),
                                        singleLine = true,
                                        shape = AppTheme.FieldShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = AppTheme.DividerColor,
                                            errorBorderColor = Color(0xFFEF4444)
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
                                        label = { Text("Drop Off Spot") },
                                        placeholder = { Text("e.g. Block C Room 204", color = AppTheme.InkTertiary) },
                                        isError = dropError.isNotEmpty(),
                                        singleLine = true,
                                        shape = AppTheme.FieldShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = AppTheme.DividerColor,
                                            errorBorderColor = Color(0xFFEF4444)
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
                                        label = { Text("Delivery Instructions") },
                                        placeholder = { Text("e.g. Call before arrival, leave at door (optional)", color = AppTheme.InkTertiary) },
                                        singleLine = true,
                                        shape = AppTheme.FieldShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color(0xFFF8FAFC),
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = AppTheme.DividerColor
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // CARD 3: Preferences & AutoPay Settings
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = AppTheme.CardShape,
                                border = BorderStroke(1.dp, AppTheme.DividerColor),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "If price exceeds catalog / estimate:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = AppTheme.InkPrimary,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                    
                                    // Segmented Policy Selector
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Policy Option 1
                                        val cancelSelected = overBudgetPolicy == "cancel"
                                        Card(
                                            onClick = { onPolicyChange("cancel") },
                                            shape = AppTheme.FieldShape,
                                            border = BorderStroke(1.dp, if (cancelSelected) primaryColor else AppTheme.DividerColor),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (cancelSelected) AppTheme.PrimarySoft else Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                RadioButton(
                                                    selected = cancelSelected,
                                                    onClick = { onPolicyChange("cancel") },
                                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Cancel this item only", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = textPrimary)
                                                    Text("Rider skips this item but buys the rest of your cart", fontSize = 10.sp, color = textSecondary)
                                                }
                                            }
                                        }

                                        // Policy Option 2
                                        val bufferSelected = overBudgetPolicy == "buffer"
                                        Card(
                                            onClick = { onPolicyChange("buffer") },
                                            shape = AppTheme.FieldShape,
                                            border = BorderStroke(1.dp, if (bufferSelected) primaryColor else AppTheme.DividerColor),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (bufferSelected) AppTheme.PrimarySoft else Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                RadioButton(
                                                    selected = bufferSelected,
                                                    onClick = { onPolicyChange("buffer") },
                                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("Buy it anyway", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = textPrimary)
                                                    Text("Auto-approves price differences to avoid counter delays", fontSize = 10.sp, color = textSecondary)
                                                }
                                            }
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = AppTheme.DividerColor)

                                    // AutoPay Switch Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Authorize AutoPay 🛡️", 
                                                fontWeight = FontWeight.Bold, 
                                                color = textPrimary, 
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Toggle switch to approve secure payments", 
                                                fontSize = 11.sp, 
                                                color = textSecondary
                                            )
                                        }
                                        Switch(
                                            checked = autopayEnabled,
                                            onCheckedChange = onAutopayChange,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = primaryColor
                                            )
                                        )
                                    }
                                }
                            }

                            // CARD 4: Bill Summary Breakdown
                            var fixedTotal = 0
                            var estMinTotal = 0
                            var estMaxTotal = 0
                            var hasEstimate = false
                            var hasUnknownPrice = false
                            
                            cart.forEach { (prod, qty) ->
                                val config = priceConfigs[prod.code ?: ""]
                                if (config != null) {
                                    hasEstimate = true
                                    if (config.isUnknown) {
                                        hasUnknownPrice = true
                                        estMinTotal += config.rangeMin * qty
                                        estMaxTotal += config.rangeMax * qty
                                    } else {
                                        val exact = config.exactPrice ?: 0
                                        fixedTotal += exact * qty
                                    }
                                }
                            }

                            if (hasEstimate) {
                                val itemsMinSubtotal = fixedTotal + estMinTotal
                                val itemsMaxSubtotal = fixedTotal + estMaxTotal
                                val totalQuantity = cart.values.sum()

                                val deliveryMin = (20 + (if (itemsMinSubtotal < 100) 5 else 0) + (if (totalQuantity > 3) (totalQuantity - 3) * 2 else 0)).coerceIn(20, 30)
                                val deliveryMax = (20 + (if (itemsMaxSubtotal < 100) 5 else 0) + (if (totalQuantity > 3) (totalQuantity - 3) * 2 else 0)).coerceIn(20, 30)
                                
                                val handlingFee = 2
                                val hostelPremium = 10

                                val totalMin = itemsMinSubtotal + deliveryMin + handlingFee + hostelPremium
                                val totalMax = itemsMaxSubtotal + deliveryMax + handlingFee + hostelPremium

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                    shape = AppTheme.CardShape,
                                    border = BorderStroke(1.dp, AppTheme.DividerColor),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Bill Details",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AppTheme.InkPrimary,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        // Item Subtotal Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Item sub total", fontSize = 13.sp, color = textSecondary)
                                            val subtotalText = if (itemsMinSubtotal == itemsMaxSubtotal) "₹$itemsMinSubtotal" else "₹$itemsMinSubtotal - ₹$itemsMaxSubtotal"
                                            Text(subtotalText, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                        }

                                        // Delivery Fee Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Delivery Fee", fontSize = 13.sp, color = textSecondary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Dynamic",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0369A1),
                                                    modifier = Modifier
                                                        .background(AppTheme.PrimarySoft, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            val delMinVal = minOf(deliveryMin, deliveryMax)
                                            val delMaxVal = maxOf(deliveryMin, deliveryMax)
                                            val deliveryText = if (delMinVal == delMaxVal) "₹$delMinVal" else "₹$delMinVal - ₹$delMaxVal"
                                            Text(deliveryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                        }

                                        // Handling Fee Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Handling Fee", fontSize = 13.sp, color = textSecondary)
                                            Text("₹$handlingFee", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                        }

                                        // Hostel Premium Fee Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Hostel gate premium fee", fontSize = 13.sp, color = textSecondary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Hostel Gate",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier
                                                        .background(Color(0xFFD1FAE5), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text("₹$hostelPremium", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                        }

                                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = AppTheme.DividerColor)

                                        // Total Pay Row
        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Total Expected Pay:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimary)
                                                Text("Includes ₹$hostelPremium hostel gate premium", fontSize = 9.sp, color = textSecondary)
                                            }
                                            val totalText = if (totalMin == totalMax) "₹$totalMin" else "₹$totalMin - ₹$totalMax"
                                            Text(totalText, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = primaryColor)
                                        }
                                        
                                        if (hasUnknownPrice) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Divider(color = AppTheme.DividerColor)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Wallet Note in Bill details card
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                                    .border(1.5.dp, Color(0xFFD1FAE5), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("ℹ️", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Since your order contains estimated prices, any leftover change will be automatically refunded back to your App Wallet.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Checkout Confirm Swipe Button
                            SwipeToPlaceOrderButton(
                                enabled = autopayEnabled,
                                onSubmit = onSubmitClick,
                                primaryColor = primaryColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
        }
    }
}
}

@Composable
fun ProductRowItem(
    product: Product,
    quantity: Int,
    priceDisplay: String,
    onQuantityChange: (Int) -> Unit,
    onPriceLabelClick: () -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = AppTheme.Primary

    val style = getCategoryStyle(product.code)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = AppTheme.CardShape,
        border = BorderStroke(1.dp, AppTheme.DividerColor),
        colors = CardDefaults.cardColors(containerColor = AppTheme.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Product Packet Mockup Card (Dynamic Flavor and Soap Type Packet illustration)
            val fullName = (product.productName ?: "").uppercase()
            val firstWord = (product.productName ?: "Item").split(" ").firstOrNull()?.uppercase() ?: "ITEM"
            val displayBrand = if (firstWord.length in 3..6) firstWord else getBrandAbbreviation(product).uppercase()
            
            // Determine packet color based on product name and flavor/type combination
            val brandColor = when {
                // 1. OREO Flavors
                fullName.contains("OREO") && fullName.contains("STRAWBERRY") -> Color(0xFFEC4899) // Strawberry Oreo Pink
                fullName.contains("OREO") && (fullName.contains("CHOCO") || fullName.contains("CHOCOLATE")) -> Color(0xFF5C381E) // Chocolate Oreo Brown
                fullName.contains("OREO") -> Color(0xFF0D47A1) // Classic Oreo Blue
                
                // 2. LAYS Flavors (Indian colors match packages)
                fullName.contains("LAYS") && (fullName.contains("SALTED") || fullName.contains("CLASSIC")) -> Color(0xFFFBC02D) // Classic Salted Yellow
                fullName.contains("LAYS") && fullName.contains("MASALA") -> Color(0xFF0288D1) // Magic Masala Blue
                fullName.contains("LAYS") && fullName.contains("CREAM") -> Color(0xFF2E7D32) // Cream & Onion Green
                fullName.contains("LAYS") -> Color(0xFFFBC02D)
                
                // 3. KURKURE Flavors
                fullName.contains("KURKURE") && fullName.contains("CHUTNEY") -> Color(0xFF4CAF50) // Green Chutney
                fullName.contains("KURKURE") && fullName.contains("MASALA") -> Color(0xFFE65100) // Masala Munch Orange
                fullName.contains("KURKURE") -> Color(0xFFE65100)
                
                // 4. BINGO Flavors
                fullName.contains("BINGO") && (fullName.contains("TOMATO") || fullName.contains("YUMITOS")) -> Color(0xFFD32F2F) // Tomato Red
                fullName.contains("BINGO") && fullName.contains("ACHAARI") -> Color(0xFFF57C00) // Achaari Masti Orange
                fullName.contains("BINGO") -> Color(0xFFD32F2F)
                
                // 5. AMUL KOOL Flavors
                fullName.contains("AMUL KOOL") && fullName.contains("KESAR") -> Color(0xFFFFB300) // Kesar Saffron Yellow
                fullName.contains("AMUL KOOL") && fullName.contains("BADAM") -> Color(0xFFFFF9C4) // Badam Cream White
                
                // 6. REAL JUICE Flavors
                fullName.contains("REAL") && fullName.contains("LITCHI") -> Color(0xFFFFF0F5) // Litchi Light Pink
                fullName.contains("REAL") && fullName.contains("GUAVA") -> Color(0xFFE91E63) // Guava Deep Pink
                
                // 7. ENO Flavors
                fullName.contains("ENO") && fullName.contains("LEMON") -> Color(0xFFFFEE58) // Lemon Eno Yellow
                
                // 8. BREAD Types
                fullName.contains("BREAD") && fullName.contains("BROWN") -> Color(0xFF795548) // Brown Bread
                fullName.contains("BREAD") && fullName.contains("WHITE") -> Color(0xFFFFFBEB) // White Bread
                
                // 9. Soaps & Personal Care Brands
                fullName.contains("DOVE") -> Color(0xFFF1F5F9) // Dove White
                fullName.contains("DETTOL") -> Color(0xFF16A34A) // Dettol Green
                fullName.contains("LIFEBUOY") -> Color(0xFFDC2626) // Lifebuoy Red
                fullName.contains("SOAP") || fullName.contains("PEARS") || fullName.contains("NIVEA") || fullName.contains("SHAMPOO") || fullName.contains("FACEWASH") -> Color(0xFF0D9488) // Personal Care Teal
                
                // 10. General Food/Drink Flavors fallback
                fullName.contains("STRAWBERRY") || fullName.contains("ROSE") || fullName.contains("LITCHI") || fullName.contains("BERRY") || fullName.contains("PINK") -> Color(0xFFEC4899) // Pink
                fullName.contains("CHOCO") || fullName.contains("CHOCOLATE") || fullName.contains("COCOA") || fullName.contains("COFFEE") || fullName.contains("BROWNIE") -> Color(0xFF5C381E) // Cocoa Brown
                fullName.contains("ORANGE") || fullName.contains("MANGO") || fullName.contains("PINEAPPLE") || fullName.contains("YELLOW") || fullName.contains("LEMON") -> Color(0xFFEAB308) // Sunny Yellow/Orange
                fullName.contains("MINT") || fullName.contains("LIME") || fullName.contains("GREEN TEA") || fullName.contains("MATCHA") || fullName.contains("ALOE") -> Color(0xFF10B981) // Mint Green
                fullName.contains("VANILLA") || fullName.contains("MILK") || fullName.contains("COCONUT") || fullName.contains("CREAM") -> Color(0xFFFFFBEB) // Cream White
                
                // 11. General Brands
                firstWord.contains("COLA") || firstWord.contains("COKE") || firstWord.contains("PEPSI") || firstWord.contains("DRINK") -> Color(0xFFC8102E) // Cola Red
                firstWord.contains("PENCIL") || firstWord.contains("BOOK") || firstWord.contains("NOTE") || firstWord.contains("PEN") -> Color(0xFF475569) // Slate grey
                else -> primaryColor // Default Indigo
            }
            
            // Symmetrical text color based on background luminance/cleanliness
            val packetTextColor = if (brandColor == Color(0xFFFFFBEB) || brandColor == Color(0xFFF1F5F9) || brandColor == Color(0xFFEAB308)) {
                AppTheme.InkPrimary // Dark ink text for light backgrounds
            } else {
                Color.White // White text for dark backgrounds
            }
            
            val crimpColor = if (brandColor == Color(0xFFFFFBEB) || brandColor == Color(0xFFF1F5F9) || brandColor == Color(0xFFEAB308)) {
                Color.Black.copy(alpha = 0.08f) // Subtle dark crimp line for light backgrounds
            } else {
                Color.White.copy(alpha = 0.25f) // White crimp line for dark backgrounds
            }

            Card(
                modifier = Modifier
                    .width(44.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = brandColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top sealing crimp line of package
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(crimpColor)
                    )
                    
                    // Brand text center stage
                    Text(
                        text = displayBrand,
                        color = packetTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    
                    // Bottom sealing crimp line of package
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(crimpColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product Details Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.productName ?: "Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textPrimary,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = quantity > 0) { onPriceLabelClick() }
                ) {
                    Text(
                        text = if (priceDisplay.isNotEmpty()) "${product.brands ?: "Generic"} • $priceDisplay" else product.brands ?: "Generic",
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1
                    )
                    if (quantity > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_edit),
                            contentDescription = "Edit Price Reference",
                            tint = primaryColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Inline Quantity Stepper / ADD Button
            if (quantity == 0) {
                Button(
                    onClick = { onQuantityChange(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("ADD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AppTheme.Surface, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, AppTheme.DividerColor), RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange(quantity - 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("-", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        text = quantity.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { onQuantityChange(quantity + 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("+", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

data class PriceConfig(
    val isUnknown: Boolean,
    val exactPrice: Int? = null,
    val rangeMin: Int = 0,
    val rangeMax: Int = 1000
)

@Composable
fun WalletBadge(balance: Int) {
    Card(
        shape = AppTheme.ChipShape,
        colors = CardDefaults.cardColors(containerColor = AppTheme.SuccessSoft),
        border = BorderStroke(1.dp, AppTheme.Success.copy(alpha = 0.4f)),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("💵", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Wallet: ₹$balance",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AppTheme.Success
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$label  ",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.InkTertiary
        )
        Text(text = value, fontSize = 12.sp, color = AppTheme.InkSecondary)
    }
}

@Composable
private fun PulsingDeliveryBadge() {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(AppTheme.AccentSoft, AppTheme.ChipShape)
            .border(BorderStroke(1.dp, AppTheme.Accent.copy(alpha = 0.5f)), AppTheme.ChipShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AppTheme.Accent.copy(alpha = pulse))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "10 min delivery promise",
            color = AppTheme.Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SwipeToPlaceOrderButton(
    enabled: Boolean,
    onSubmit: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) primaryColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
            .border(1.dp, if (enabled) primaryColor.copy(alpha = 0.3f) else Color(0xFFE2E8F0), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val thumbSizePx = with(density) { 48.dp.toPx() }
        val paddingPx = with(density) { 8.dp.toPx() }
        val maxDragPx = (widthPx - thumbSizePx - paddingPx).coerceAtLeast(0f)
        
        var dragOffset by remember { mutableStateOf(0f) }
        val animatedOffset by animateFloatAsState(
            targetValue = dragOffset,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
        
        // Reset drag offset when disabled
        LaunchedEffect(enabled) {
            if (!enabled) {
                dragOffset = 0f
            }
        }
        
        // Track Text Hint
        Text(
            text = if (enabled) "Swipe to Place Order ➔" else "Authorize AutoPay to Order",
            color = if (enabled) primaryColor else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Drag Thumb
        val thumbOffsetDp = with(density) { animatedOffset.toDp() }
        
        Box(
            modifier = Modifier
                .padding(4.dp)
                .offset(x = thumbOffsetDp)
                .size(48.dp)
                .clip(CircleShape)
                .background(if (enabled) primaryColor else Color(0xFFCBD5E1))
                .pointerInput(enabled) {
                    if (enabled) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset >= maxDragPx * 0.85f) {
                                    dragOffset = maxDragPx
                                    onSubmit()
                                    // Smooth reset after trigger
                                    dragOffset = 0f
                                } else {
                                    dragOffset = 0f
                                }
                            },
                            onDragCancel = {
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, maxDragPx)
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "➔",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

