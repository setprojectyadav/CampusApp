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

    val Primary = Color(0xFF5B4FE9)       // Electric indigo — brand
    val PrimaryDark = Color(0xFF3F35C4)
    val PrimarySoft = Color(0xFFEDEBFC)

    val Accent = Color(0xFFFFB627)        // Sunburst — speed / energy
    val AccentSoft = Color(0xFFFFF3DC)

    val Success = Color(0xFF12B886)       // Mint — wallet / money
    val SuccessSoft = Color(0xFFDFF7EE)

    val Danger = Color(0xFFF03E5C)
    val DangerSoft = Color(0xFFFDE7EA)

    val DividerColor = Color(0xFFE7E8F3)

    val PrimaryGradient = Brush.linearGradient(listOf(Color(0xFF6C5CE9), Color(0xFF4B3FD1)))
    val AccentGradient = Brush.linearGradient(listOf(Color(0xFFFFC94A), Color(0xFFFF9A3D)))
    val HeroGradient = Brush.linearGradient(listOf(Color(0xFF241E5C), Color(0xFF4B3FD1)))

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCatalog()

        setContent {
            var searchQuery by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
            
            // Cart State Map
            val cart = remember { mutableStateMapOf<Product, Int>() }
            val priceConfigs = remember { mutableStateMapOf<String, PriceConfig>() }
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
                AlertDialog(
                    onDismissRequest = {
                        showOrderSuccess = false
                        cart.clear()
                    },
                    shape = AppTheme.CardShape,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(AppTheme.SuccessSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅", fontSize = 24.sp)
                        }
                    },
                    title = {
                        Text(
                            text = "Order placed!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = AppTheme.InkPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                "SECURE ORDER CONFIRMATION",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = AppTheme.InkTertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            cart.forEach { (prod, qty) ->
                                Text(
                                    "•  ${prod.productName}  ×$qty",
                                    color = AppTheme.InkSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = AppTheme.DividerColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoLine("Pickup", SecurityUtils.sanitizeInput(pickPoint))
                            InfoLine("Delivery", SecurityUtils.sanitizeInput(dropPoint))
                            InfoLine("Policy", if (overBudgetPolicy == "cancel") "Cancel over-budget item" else "Buy over-budget item")
                            InfoLine("Instructions", if (instructions.isEmpty()) "None" else SecurityUtils.sanitizeInput(instructions))
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppTheme.SuccessSoft, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text("🔒", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Payment processed securely via AutoPay token authorization.",
                                    color = AppTheme.Success,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOrderSuccess = false
                                cart.clear()
                                finish()
                            },
                            shape = AppTheme.ButtonShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary)
                        ) {
                            Text("Awesome, thanks!", fontWeight = FontWeight.Bold)
                        }
                    }
                )
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
            title = { Text("Set Price Reference", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                        val minVal = (priceConfigRange.start / 5).toInt() * 5
                        val maxVal = (priceConfigRange.endInclusive / 5).toInt() * 5
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Est. Min: ₹$minVal", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 13.sp)
                            Text("Est. Max: ₹$maxVal", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 13.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        RangeSlider(
                            value = priceConfigRange,
                            onValueChange = { priceConfigRange = it },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = primaryColor,
                                thumbColor = primaryColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rider will verify real price and update your final bill.",
                            fontSize = 11.sp,
                            color = textSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = prod.code ?: ""
                        if (!priceConfigIsUnknown && priceConfigExact.trim().isNotEmpty()) {
                            priceConfigs[code] = PriceConfig(
                                isUnknown = false,
                                exactPrice = priceConfigExact.trim().toInt()
                            )
                        } else {
                            val minVal = (priceConfigRange.start / 5).toInt() * 5
                            val maxVal = (priceConfigRange.endInclusive / 5).toInt() * 5
                            priceConfigs[code] = PriceConfig(
                                isUnknown = true,
                                rangeMin = minVal,
                                rangeMax = maxVal
                            )
                        }
                        priceConfigProduct = null
                        priceConfigExact = ""
                        priceConfigRange = 0f..300f
                    },
                    shape = AppTheme.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Save Configuration")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val code = prod.code ?: ""
                    if (!priceConfigs.containsKey(code)) {
                        priceConfigs[code] = PriceConfig(isUnknown = true, rangeMin = 0, rangeMax = 200)
                    }
                    priceConfigProduct = null
                    priceConfigExact = ""
                    priceConfigRange = 0f..300f
                }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // Custom Item Request Dialog
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCustomDialog = false
                customName = ""
                customBrand = ""
                customPrice = ""
                customNameError = false
            },
            title = { Text("Request Custom Item", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                        label = { Text("Product Name (e.g. Axe Signature Perfume)") },
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
                        label = { Text("Brand Name (Optional)") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Add to Basket")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCustomDialog = false
                    customName = ""
                    customBrand = ""
                    customPrice = ""
                    customNameError = false
                }) {
                    Text("Cancel", color = textSecondary)
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
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Search for a product above to place an order.",
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCustomDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = AppTheme.ButtonShape
                                ) {
                                    Text("+ Request Custom Item", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Canteen items & snacks delivered to your doorstep",
                                    color = primaryColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
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

                            item {
                                Card(
                                    onClick = { showCustomDialog = true },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    shape = AppTheme.FieldShape,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F5F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = android.R.drawable.ic_input_add),
                                            contentDescription = "Add Custom",
                                            tint = primaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Can't find what you need? Add a Custom Item",
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
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

                // The Drawer container
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Premium Native Bottom Sheet Grab Handle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color(0xFFECE7E1))
                                .clip(CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Review Order Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textPrimary
                            )
                            IconButton(onClick = { showCheckout = false }) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Scrollable Area (Cart details & Forms)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Basket Items:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            cart.forEach { (prod, qty) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
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
                                                "Qty: $qty (Range: ₹${config.rangeMin} - ₹${config.rangeMax})"
                                            } else {
                                                "Qty: $qty x ₹${config.exactPrice}"
                                            }
                                        } else {
                                            "Qty: $qty"
                                        }
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 16.dp))

                            // Delivery location form
                            Text(
                                text = "Delivery Details:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = pickPoint,
                                onValueChange = onPickChange,
                                label = { Text("Pick Up Location (e.g. Campus Store)") },
                                isError = pickError.isNotEmpty(),
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
                                shape = AppTheme.FieldShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFFECE7E1)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Over-Budget Policy Selector
                            Text(
                                text = "If actual price exceeds catalog/estimated price:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = AppTheme.FieldShape,
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), // Off-white clean background card
                                border = BorderStroke(1.dp, Color(0xFFECE7E1))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Option B Checkbox Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPolicyChange("cancel") }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = overBudgetPolicy == "cancel",
                                            onCheckedChange = { if (it) onPolicyChange("cancel") },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Cancel this item only", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = textPrimary)
                                            Text("Rider skips this item but buys the rest of your cart", fontSize = 10.sp, color = textSecondary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Option C Checkbox Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPolicyChange("buffer") }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = overBudgetPolicy == "buffer",
                                            onCheckedChange = { if (it) onPolicyChange("buffer") },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Buy it anyway", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = textPrimary)
                                            Text("Auto-approves price differences to avoid counter delays", fontSize = 10.sp, color = textSecondary)
                                        }
                                    }
                                }
                            }

                            // AutoPay Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Authorize AutoPay", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                                    Text("Toggle switch to approve secure payments", fontSize = 11.sp, color = textSecondary)
                                }
                                Switch(
                                    checked = autopayEnabled,
                                    onCheckedChange = onAutopayChange,
                                    colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                                )
                            }

                            var fixedTotal = 0
                            var estMinTotal = 0
                            var estMaxTotal = 0
                            var hasEstimate = false
                            
                            cart.forEach { (prod, qty) ->
                                val config = priceConfigs[prod.code ?: ""]
                                if (config != null) {
                                    hasEstimate = true
                                    if (config.isUnknown) {
                                        estMinTotal += config.rangeMin * qty
                                        estMaxTotal += config.rangeMax * qty
                                    } else {
                                        val exact = config.exactPrice ?: 0
                                        fixedTotal += exact * qty
                                    }
                                }
                            }
                            
                            if (hasEstimate) {
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                val itemsMinSubtotal = fixedTotal + estMinTotal
                                val itemsMaxSubtotal = fixedTotal + estMaxTotal
                                val totalQuantity = cart.values.sum()

                                // Calculate Dynamic Delivery Fee based on Subtotal & Item Count (Range: ₹20 - ₹30)
                                val deliveryMin = (20 + (if (itemsMinSubtotal < 100) 5 else 0) + (if (totalQuantity > 3) (totalQuantity - 3) * 2 else 0)).coerceIn(20, 30)
                                val deliveryMax = (20 + (if (itemsMaxSubtotal < 100) 5 else 0) + (if (totalQuantity > 3) (totalQuantity - 3) * 2 else 0)).coerceIn(20, 30)
                                
                                val handlingFee = 2
                                val hostelPremium = 10

                                val totalMin = itemsMinSubtotal + deliveryMin + handlingFee + hostelPremium
                                val totalMax = itemsMaxSubtotal + deliveryMax + handlingFee + hostelPremium

                                // Item Subtotal
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Item sub total", fontSize = 13.sp, color = textSecondary)
                                    val subtotalText = if (itemsMinSubtotal == itemsMaxSubtotal) "₹$itemsMinSubtotal" else "₹$itemsMinSubtotal - ₹$itemsMaxSubtotal"
                                    Text(subtotalText, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                }

                                // Delivery Fee
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
                                            color = Color(0xFF5C1D8D),
                                            modifier = Modifier
                                                .background(Color(0xFFF3E8FF), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    val delMinVal = minOf(deliveryMin, deliveryMax)
                                    val delMaxVal = maxOf(deliveryMin, deliveryMax)
                                    val deliveryText = if (delMinVal == delMaxVal) "₹$delMinVal" else "₹$delMinVal - ₹$delMaxVal"
                                    Text(deliveryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                }

                                // Handling Fee
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Handling Fee", fontSize = 13.sp, color = textSecondary)
                                    Text("₹$handlingFee", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = textPrimary)
                                }

                                // Hostel Gate Premium Fee
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

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFECE7E1))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Total Expected Pay
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Expected Pay:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimary)
                                        Text("Includes ₹$hostelPremium hostel gate delivery premium", fontSize = 9.sp, color = textSecondary)
                                    }
                                    val totalText = if (totalMin == totalMax) "₹$totalMin" else "₹$totalMin - ₹$totalMax"
                                    Text(totalText, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = primaryColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onSubmitClick,
                                enabled = autopayEnabled,
                                shape = AppTheme.ButtonShape,
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
            // Brand Logo Circle Badge (Blinkit style: clean flat off-white circle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppTheme.SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = style.iconResId),
                    contentDescription = product.productName,
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
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

