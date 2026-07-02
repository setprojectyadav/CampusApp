package com.college.campusapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.college.campusapp.security.EncryptionManager
import com.college.campusapp.security.SecurityUtils
import kotlin.random.Random
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Check
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        googleProfileEmail.value = user.email ?: ""
                        googleProfileName.value = user.displayName ?: ""
                        showGoogleProfileSetup.value = true
                    }
                } else {
                    Toast.makeText(this, "Google Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private val showGoogleProfileSetup = mutableStateOf(false)
    private val isPermissionsStepActive = mutableStateOf(false)
    private val showCongratulationsDialogActive = mutableStateOf(false)

    private val requestRegistrationPermissionsLauncher = registerForActivityResult(
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
            isPermissionsStepActive.value = false
            showCongratulationsDialogActive.value = true
        } else {
            Toast.makeText(this, "Permissions are required to proceed with registration.", Toast.LENGTH_LONG).show()
        }
    }

    private fun triggerRegistrationPermissionsRequest() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestRegistrationPermissionsLauncher.launch(permissions.toTypedArray())
    }
    private val googleProfileName = mutableStateOf("")
    private val googleProfileEmail = mutableStateOf("")

    private val frontPhoto = mutableStateOf<Bitmap?>(null)
    private val backPhoto = mutableStateOf<Bitmap?>(null)
    private val isIdUploadActive = mutableStateOf(false)

    private val cameraFrontLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            frontPhoto.value = bitmap
        } else {
            Toast.makeText(this, "Front photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraBackLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            backPhoto.value = bitmap
        } else {
            Toast.makeText(this, "Back photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isIdUploadActive.value = true
        } else {
            Toast.makeText(
                this,
                "Camera permission is required to upload your student ID card.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // AUTO-LOGIN: Check if "Remember Me" is enabled and user is logged in
        val rememberMe = EncryptionManager.getBoolean(this, "remember_me", false)
        if (auth.currentUser != null) {
            if (rememberMe) {
                navigateToNext()
                return
            } else {
                auth.signOut()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // CYBER SECURITY: Prevent screenshots/screen-recording on credential/OTP screens
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            AuthScreenView(
                frontBitmap = frontPhoto.value,
                backBitmap = backPhoto.value,
                isIdUpload = isIdUploadActive.value,
                isPermissionsStep = isPermissionsStepActive.value,
                showCongratulationsExternal = showCongratulationsDialogActive.value,
                onGrantPermissions = { triggerRegistrationPermissionsRequest() },
                onCaptureFront = { 
                    checkCameraPermission()
                    cameraFrontLauncher.launch(null) 
                },
                onCaptureBack = { 
                    checkCameraPermission()
                    cameraBackLauncher.launch(null) 
                },
                onBackToCredentials = { isIdUploadActive.value = false },
                onVerifyFinished = {
                    isIdUploadActive.value = false
                    isPermissionsStepActive.value = true
                },
                onLogin = { email, password, rememberMe, onSuccess, onFailure ->
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    EncryptionManager.saveString(this, "user_email", email)
                                    EncryptionManager.saveBoolean(this, "is_signed_up", true)
                                    EncryptionManager.saveBoolean(this, "remember_me", rememberMe)
                                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                    navigateToNext()
                                }
                            } else {
                                val errorMsg = task.exception?.message ?: "Authentication failed."
                                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                                onFailure(errorMsg)
                            }
                        }
                },
                onRegister = { name, email, phone, password, onSuccess, onFailure ->
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    EncryptionManager.saveString(this, "user_email", email)
                                    Toast.makeText(this, "Account Created! Please verify your student ID card.", Toast.LENGTH_LONG).show()
                                    isIdUploadActive.value = true
                                    onSuccess()
                                }
                            } else {
                                val errorMsg = task.exception?.message ?: "Account creation failed."
                                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                                onFailure(errorMsg)
                            }
                        }
                },
                onForgotPassword = { email, onSuccess, onFailure ->
                    auth.sendPasswordResetEmail(email.trim())
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                onSuccess()
                            } else {
                                val errorMsg = task.exception?.message ?: "Failed to send password reset email."
                                onFailure(errorMsg)
                            }
                        }
                },
                onGoogleSignIn = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
                onFinished = {
                    // Save registration flags on home click
                    EncryptionManager.saveBoolean(this, "is_signed_up", true)
                    navigateToNext()
                },
                showGoogleProfileSetup = showGoogleProfileSetup.value,
                googleProfileName = googleProfileName.value,
                googleProfileEmail = googleProfileEmail.value,
                onGoogleProfileSaved = { name, phone, rollNo ->
                    EncryptionManager.saveString(this, "user_email", googleProfileEmail.value)
                    EncryptionManager.saveString(this, "user_name", name)
                    EncryptionManager.saveString(this, "user_phone", phone)
                    EncryptionManager.saveString(this, "user_roll_no", rollNo)
                    EncryptionManager.saveBoolean(this, "is_signed_up", true)
                    Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                    showGoogleProfileSetup.value = false
                    navigateToNext()
                }
            )
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                isIdUploadActive.value = true
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateToNext() {
        startActivity(Intent(this, PermissionsActivity::class.java))
        finish()
    }
}

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)

val PoppinsTypography = Typography(
    displayLarge = TextStyle(fontFamily = PoppinsFontFamily),
    displayMedium = TextStyle(fontFamily = PoppinsFontFamily),
    displaySmall = TextStyle(fontFamily = PoppinsFontFamily),
    headlineLarge = TextStyle(fontFamily = PoppinsFontFamily),
    headlineMedium = TextStyle(fontFamily = PoppinsFontFamily),
    headlineSmall = TextStyle(fontFamily = PoppinsFontFamily),
    titleLarge = TextStyle(fontFamily = PoppinsFontFamily),
    titleMedium = TextStyle(fontFamily = PoppinsFontFamily),
    titleSmall = TextStyle(fontFamily = PoppinsFontFamily),
    bodyLarge = TextStyle(fontFamily = PoppinsFontFamily),
    bodyMedium = TextStyle(fontFamily = PoppinsFontFamily),
    bodySmall = TextStyle(fontFamily = PoppinsFontFamily),
    labelLarge = TextStyle(fontFamily = PoppinsFontFamily),
    labelMedium = TextStyle(fontFamily = PoppinsFontFamily),
    labelSmall = TextStyle(fontFamily = PoppinsFontFamily)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenView(
    frontBitmap: Bitmap?,
    backBitmap: Bitmap?,
    isIdUpload: Boolean,
    isPermissionsStep: Boolean,
    showCongratulationsExternal: Boolean,
    onGrantPermissions: () -> Unit,
    onCaptureFront: () -> Unit,
    onCaptureBack: () -> Unit,
    onBackToCredentials: () -> Unit,
    onVerifyFinished: () -> Unit,
    onLogin: (String, String, Boolean, () -> Unit, (String) -> Unit) -> Unit,
    onRegister: (String, String, String, String, () -> Unit, (String) -> Unit) -> Unit,
    onForgotPassword: (String, () -> Unit, (String) -> Unit) -> Unit,
    onGoogleSignIn: () -> Unit,
    onFinished: () -> Unit,
    showGoogleProfileSetup: Boolean,
    googleProfileName: String,
    googleProfileEmail: String,
    onGoogleProfileSaved: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 for Login, 1 for Sign Up
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    // Login form states
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginRememberMe by remember { mutableStateOf(false) }
    var loginEmailError by remember { mutableStateOf("") }
    var loginPasswordError by remember { mutableStateOf("") }

    // Register form states
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPhone by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerRememberMe by remember { mutableStateOf(false) }
    var registerNameError by remember { mutableStateOf("") }
    var registerEmailError by remember { mutableStateOf("") }
    var registerPhoneError by remember { mutableStateOf("") }
    var registerPasswordError by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var simulatedOtp by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordStep by remember { mutableStateOf(0) } // 0: Email, 1: OTP, 2: New Pass, 3: Success
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordEmailError by remember { mutableStateOf("") }
    
    var forgotPasswordOtp by remember { mutableStateOf("") }
    var forgotPasswordOtpError by remember { mutableStateOf("") }
    var simulatedForgotPasswordOtp by remember { mutableStateOf("") }
    
    var forgotPasswordNew by remember { mutableStateOf("") }
    var forgotPasswordNewError by remember { mutableStateOf("") }
    var forgotPasswordConfirm by remember { mutableStateOf("") }
    var forgotPasswordConfirmError by remember { mutableStateOf("") }
    var forgotPasswordNewVisible by remember { mutableStateOf(false) }
    var forgotPasswordConfirmVisible by remember { mutableStateOf(false) }


    // DRIBBBLE BRAND THEME COLORS
    val activePillColor = Color(0xFF0EA5E9) // Premium Light Blue
    val inactivePillBg = Color(0xFFF1F5F9) // Light gray bg
    val inputBgColor = Color(0xFFF8FAFC) // Slate input bg
    val textPrimary = Color(0xFF0F172A) // Black-slate
    val textSecondary = Color(0xFF64748B) // Slate gray text

    MaterialTheme(typography = PoppinsTypography) {
        if (showGoogleProfileSetup) {
            var localName by remember { mutableStateOf(googleProfileName) }
            var localPhone by remember { mutableStateOf("") }
            var localRollNo by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf("") }
            var phoneError by remember { mutableStateOf("") }
            var rollNoError by remember { mutableStateOf("") }
            
            LaunchedEffect(googleProfileName) {
                localName = googleProfileName
            }

            Dialog(
                onDismissRequest = {} // Force setup completion
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(activePillColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo_campus),
                                contentDescription = "Profile Logo",
                                tint = activePillColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Complete Profile",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Please enter your official student details to complete registration.",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 1. Name input
                        OutlinedTextField(
                            value = localName,
                            onValueChange = { localName = it; nameError = "" },
                            placeholder = { Text("Official Full Name", fontSize = 14.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            isError = nameError.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = activePillColor,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color(0xFFEF4444),
                                errorContainerColor = inputBgColor
                            )
                        )
                        if (nameError.isNotEmpty()) {
                            Text(text = nameError, color = Color.Red, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // 2. Mobile input
                        OutlinedTextField(
                            value = localPhone,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }
                                if (digitsOnly.length <= 10) {
                                    localPhone = digitsOnly
                                }
                                phoneError = ""
                            },
                            placeholder = { Text("Mobile Number", fontSize = 14.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = activePillColor,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color(0xFFEF4444),
                                errorContainerColor = inputBgColor
                            )
                        )
                        if (phoneError.isNotEmpty()) {
                            Text(text = phoneError, color = Color.Red, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // 3. Roll number input
                        OutlinedTextField(
                            value = localRollNo,
                            onValueChange = { localRollNo = it; rollNoError = "" },
                            placeholder = { Text("Student ID / Roll Number", fontSize = 14.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            isError = rollNoError.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = activePillColor,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color(0xFFEF4444),
                                errorContainerColor = inputBgColor
                            )
                        )
                        if (rollNoError.isNotEmpty()) {
                            Text(text = rollNoError, color = Color.Red, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 2.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                var isFormValid = true
                                if (localName.trim().isEmpty()) {
                                    nameError = "Name is required"
                                    isFormValid = false
                                }
                                if (!SecurityUtils.isValidPhone(localPhone)) {
                                    phoneError = "Please enter a valid 10-digit number"
                                    isFormValid = false
                                }
                                if (localRollNo.trim().isEmpty()) {
                                    rollNoError = "Roll Number is required"
                                    isFormValid = false
                                }
                                
                                if (isFormValid) {
                                    onGoogleProfileSaved(localName, localPhone, localRollNo)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activePillColor),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        if (showForgotPasswordDialog) {
            Dialog(
                onDismissRequest = { 
                    showForgotPasswordDialog = false 
                    forgotPasswordStep = 0
                    forgotPasswordEmail = ""
                    forgotPasswordEmailError = ""
                    forgotPasswordNew = ""
                    forgotPasswordNewError = ""
                    forgotPasswordConfirm = ""
                    forgotPasswordConfirmError = ""
                }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (forgotPasswordStep == 0) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(activePillColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock Icon",
                                    tint = activePillColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            Text(
                                text = "Reset Password",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = textPrimary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Please enter your registered email address and choose a secure new password.",
                                color = textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // 1. Email Input
                            OutlinedTextField(
                                value = forgotPasswordEmail,
                                onValueChange = { 
                                    forgotPasswordEmail = it 
                                    forgotPasswordEmailError = ""
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email Icon",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                placeholder = { Text("Email Address", fontSize = 14.sp, color = textSecondary) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, color = textPrimary, fontFamily = FontFamily.Default),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrect = false),
                                isError = forgotPasswordEmailError.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = activePillColor,
                                    unfocusedBorderColor = Color.Transparent,
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorContainerColor = inputBgColor,
                                    disabledBorderColor = Color.Transparent
                                )
                            )
                            
                            if (forgotPasswordEmailError.isNotEmpty()) {
                                Text(
                                    text = forgotPasswordEmailError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .padding(start = 4.dp, top = 2.dp)
                                        .align(Alignment.Start)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // 2. New Password Input
                            OutlinedTextField(
                                value = forgotPasswordNew,
                                onValueChange = { 
                                    forgotPasswordNew = it 
                                    forgotPasswordNewError = ""
                                },
                                placeholder = { Text("New Password", fontSize = 15.sp, color = textSecondary) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                                visualTransformation = if (forgotPasswordNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                isError = forgotPasswordNewError.isNotEmpty(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = activePillColor,
                                    unfocusedBorderColor = Color.Transparent,
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorContainerColor = inputBgColor,
                                    disabledBorderColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    val iconRes = if (forgotPasswordNewVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                    IconButton(
                                        onClick = { forgotPasswordNewVisible = !forgotPasswordNewVisible },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = "Toggle Password Visibility",
                                            tint = textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                            
                            if (forgotPasswordNewError.isNotEmpty()) {
                                Text(
                                    text = forgotPasswordNewError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .padding(start = 4.dp, top = 2.dp)
                                        .align(Alignment.Start)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // 3. Confirm Password Input
                            OutlinedTextField(
                                value = forgotPasswordConfirm,
                                onValueChange = { 
                                    forgotPasswordConfirm = it 
                                    forgotPasswordConfirmError = ""
                                },
                                placeholder = { Text("Confirm New Password", fontSize = 15.sp, color = textSecondary) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                                visualTransformation = if (forgotPasswordConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                isError = forgotPasswordConfirmError.isNotEmpty(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = activePillColor,
                                    unfocusedBorderColor = Color.Transparent,
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorContainerColor = inputBgColor,
                                    disabledBorderColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    val iconRes = if (forgotPasswordConfirmVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                    IconButton(
                                        onClick = { forgotPasswordConfirmVisible = !forgotPasswordConfirmVisible },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = "Toggle Password Visibility",
                                            tint = textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                            
                            if (forgotPasswordConfirmError.isNotEmpty()) {
                                Text(
                                    text = forgotPasswordConfirmError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .padding(start = 4.dp, top = 2.dp)
                                        .align(Alignment.Start)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            Button(
                                onClick = {
                                    var isFormValid = true
                                    
                                    if (!SecurityUtils.isValidEmail(forgotPasswordEmail)) {
                                        forgotPasswordEmailError = "Please enter a valid email address"
                                        isFormValid = false
                                    }
                                    
                                    if (forgotPasswordNew.isEmpty()) {
                                        forgotPasswordNewError = "Password is required"
                                        isFormValid = false
                                    } else if (!SecurityUtils.isStrongPassword(forgotPasswordNew)) {
                                        forgotPasswordNewError = "Password too weak"
                                        isFormValid = false
                                    }
                                    
                                    if (forgotPasswordConfirm != forgotPasswordNew) {
                                        forgotPasswordConfirmError = "Passwords do not match"
                                        isFormValid = false
                                    }
                                    
                                    if (isFormValid) {
                                        forgotPasswordStep = 1
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activePillColor),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Update Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = "Go Back",
                                color = textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable {
                                        showForgotPasswordDialog = false
                                        forgotPasswordEmail = ""
                                        forgotPasswordEmailError = ""
                                        forgotPasswordNew = ""
                                        forgotPasswordNewError = ""
                                        forgotPasswordConfirm = ""
                                        forgotPasswordConfirmError = ""
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        } else {
                            // STEP 1: SUCCESS DIALOG CARD
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success Check",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            Text(
                                text = "Password Updated!",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF10B981)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Your password has been reset successfully. You can now use your new password to log in to CampusApp.",
                                color = textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            Button(
                                onClick = {
                                    showForgotPasswordDialog = false
                                    forgotPasswordStep = 0
                                    forgotPasswordEmail = ""
                                    forgotPasswordNew = ""
                                    forgotPasswordConfirm = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = textPrimary),
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Okay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }



    // Congratulations Success Card Dialog (Screen 3 of mockup)
    if (showCongratulationsExternal) {
        Dialog(
            onDismissRequest = {}
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF34D399)) // Vibrant green card
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Success check circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.checkbox_on_background),
                            contentDescription = "Success checkmark",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Congratulations!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Success is the result of hard work and perseverance.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Custom yellow Home Page action button inside the green card
                    Button(
                        onClick = {
                            onFinished()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activePillColor)
                    ) {
                        Text("Home Page", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F9FF), // Very soft sky blue
                        Color(0xFFE0F2FE), // Pale sky blue
                        Color(0xFFF8FAFC)  // Soft slate-white
                    )
                )
            )
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val topSpacerHeight = if (selectedTab == 0) 90.dp else 10.dp
            Spacer(modifier = Modifier.height(topSpacerHeight))

            // Premium graduation cap logo header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logo_campus),
                    contentDescription = "CampusApp Logo",
                    tint = activePillColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CampusApp",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = textPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Your digital gateway to college life",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))


            if (isPermissionsStep) {
                // Permissions Card (displayed during registration, after account creation, before ID photo capture)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(activePillColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                                contentDescription = "Permissions Icon",
                                tint = activePillColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Permissions Required",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "To verify your identity and send campus updates, please grant the following permissions:",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Permission item 1: Location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                                contentDescription = "Location Icon",
                                tint = textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Location Services", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                                Text("Used to verify you are currently on the college campus.", color = textSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Permission item 2: Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_popup_sync),
                                contentDescription = "Notification Icon",
                                tint = textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Push Notifications", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                                Text("Receive alerts about campus events, classes, and verification updates.", color = textSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onGrantPermissions,
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activePillColor)
                        ) {
                            Text(
                                text = "Grant Permissions",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else if (isIdUpload) {
                // ID Card Capture Screen (Card 2)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Student ID Verification",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            IconButton(
                                onClick = { onBackToCredentials() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(inactivePillBg)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_revert),
                                    contentDescription = "Back to Form",
                                    tint = textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Please capture clear photos of your Student ID card (Front & Back) to verify your campus identity.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                            textAlign = TextAlign.Center
                        )

                        // 1. Front of ID Card Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(inputBgColor)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                                .clickable { onCaptureFront() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (frontBitmap != null) {
                                Image(
                                    bitmap = frontBitmap.asImageBitmap(),
                                    contentDescription = "Front of ID",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                                        contentDescription = "Camera Icon",
                                        tint = activePillColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Capture ID Card (Front)", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Back of ID Card Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(inputBgColor)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                                .clickable { onCaptureBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (backBitmap != null) {
                                Image(
                                    bitmap = backBitmap.asImageBitmap(),
                                    contentDescription = "Back of ID",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                                        contentDescription = "Camera Icon",
                                        tint = activePillColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Capture ID Card (Back)", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Verify & Continue Button
                        val canVerify = frontBitmap != null && backBitmap != null
                        Button(
                            onClick = {
                                if (canVerify) {
                                    onVerifyFinished()
                                }
                            },
                            enabled = canVerify,
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activePillColor,
                                disabledContainerColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = "Verify & Continue",
                                color = if (canVerify) Color.White else textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Elevated Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Card Screen Title & Subtitle with back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedTab == 0) "Welcome Back" else "Create Account",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )

                            if (selectedTab == 1) {
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        selectedTab = 0
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(inactivePillBg)
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_revert),
                                        contentDescription = "Back to Login",
                                        tint = textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (selectedTab == 0) "Step Into the Future of Campus Life" else "Sign up to join your campus community",
                            fontSize = 13.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        // Input Fields Section
                        Column(modifier = Modifier.fillMaxWidth()) {
                    
                    // Full Name (Sign Up only)
                    AnimatedVisibility(visible = selectedTab == 1) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Full Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = registerName,
                                onValueChange = { registerName = it; registerNameError = "" },
                                placeholder = { Text("Enter your name", fontSize = 16.sp, color = textSecondary) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                isError = registerNameError.isNotEmpty(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorContainerColor = inputBgColor,
                                    disabledBorderColor = Color.Transparent
                                )
                            )
                            if (registerNameError.isNotEmpty()) {
                                Text(
                                    text = registerNameError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }

                    // Email Address
                    Text(
                        text = "Email Address",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { 
                                loginEmail = it
                                loginEmailError = ""
                            },
                            placeholder = { Text("Enter your email", fontSize = 16.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrect = false),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            )
                        )
                        if (loginEmailError.isNotEmpty()) {
                            Text(
                                text = loginEmailError,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    } else {
                        OutlinedTextField(
                            value = registerEmail,
                            onValueChange = { 
                                registerEmail = it
                                registerEmailError = ""
                            },
                            placeholder = { Text("Enter your email", fontSize = 16.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrect = false),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = registerEmailError.isNotEmpty(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color(0xFFEF4444),
                                errorContainerColor = inputBgColor,
                                disabledBorderColor = Color.Transparent
                            )
                        )
                        if (registerEmailError.isNotEmpty()) {
                            Text(
                                text = registerEmailError,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    }

                    // Phone Number (Sign Up only to save screen real estate on Login)
                    AnimatedVisibility(visible = selectedTab == 1) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Mobile Number (+91)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            OutlinedTextField(
                                value = registerPhone,
                                onValueChange = { input ->
                                    val digitsOnly = input.filter { it.isDigit() }
                                    if (digitsOnly.length <= 10) {
                                        registerPhone = digitsOnly
                                    }
                                    registerPhoneError = ""
                                },
                                placeholder = { Text("Enter mobile number", fontSize = 16.sp, color = textSecondary) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, autoCorrect = false),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                isError = registerPhoneError.isNotEmpty(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBgColor,
                                    unfocusedContainerColor = inputBgColor,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    errorBorderColor = Color(0xFFEF4444),
                                    errorContainerColor = inputBgColor,
                                    disabledBorderColor = Color.Transparent
                                )
                            )
                            if (registerPhoneError.isNotEmpty()) {
                                Text(
                                    text = registerPhoneError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                        }
                    }

                    // Password
                    Text(
                        text = "Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { 
                                loginPassword = it
                                loginPasswordError = ""
                            },
                            placeholder = { Text("Password", fontSize = 16.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                val iconRes = if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = "Toggle Password Visibility",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        if (loginPasswordError.isNotEmpty()) {
                            Text(
                                text = loginPasswordError,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = registerPassword,
                            onValueChange = { 
                                registerPassword = it
                                registerPasswordError = ""
                            },
                            placeholder = { Text("Password", fontSize = 16.sp, color = textSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = textPrimary, fontFamily = FontFamily.Default),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = registerPasswordError.isNotEmpty(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color(0xFFEF4444),
                                errorContainerColor = inputBgColor,
                                disabledBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                val iconRes = if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = "Toggle Password Visibility",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        if (registerPasswordError.isNotEmpty()) {
                            Text(
                                text = registerPasswordError,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }

                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Extras (Remember Me & conditionally Forgot Password)
                    if (selectedTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = loginRememberMe,
                                    onCheckedChange = { loginRememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = textPrimary)
                                )
                                Text(text = "Remember Me", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "Forgot Password?",
                                color = textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    showForgotPasswordDialog = true
                                }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = registerRememberMe,
                                onCheckedChange = { registerRememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = textPrimary)
                            )
                            Text(text = "Remember Me", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main Action Button (Neon Yellow-Green with Black text)
                    Button(
                        onClick = {
                            var isValid = true
                            if (selectedTab == 0) {
                                loginEmailError = ""
                                loginPasswordError = ""

                                if (SecurityUtils.containsInjectionPatterns(loginEmail) ||
                                    SecurityUtils.containsInjectionPatterns(loginPassword)
                                ) {
                                    Toast.makeText(context, "Security Threat: Injection detected!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                if (!SecurityUtils.isValidEmail(loginEmail)) {
                                    loginEmailError = "Please enter a valid email address"
                                    isValid = false
                                }

                                if (loginPassword.isEmpty()) {
                                    loginPasswordError = "Password is required"
                                    isValid = false
                                }

                                if (isValid) {
                                    onLogin(
                                        loginEmail,
                                        loginPassword,
                                        loginRememberMe,
                                        {
                                            focusManager.clearFocus()
                                        },
                                        { error ->
                                            // Error is handled via Toast by AuthActivity
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Please correct the highlighted fields", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                registerNameError = ""
                                registerEmailError = ""
                                registerPhoneError = ""
                                registerPasswordError = ""

                                if (SecurityUtils.containsInjectionPatterns(registerName) ||
                                    SecurityUtils.containsInjectionPatterns(registerEmail) ||
                                    SecurityUtils.containsInjectionPatterns(registerPhone) ||
                                    SecurityUtils.containsInjectionPatterns(registerPassword)
                                ) {
                                    Toast.makeText(context, "Security Threat: Injection detected!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                if (registerName.trim().isEmpty()) {
                                    registerNameError = "Name cannot be empty"
                                    isValid = false
                                }

                                if (!SecurityUtils.isValidEmail(registerEmail)) {
                                    registerEmailError = "Please enter a valid email address"
                                    isValid = false
                                }

                                if (!SecurityUtils.isValidPhone(registerPhone)) {
                                    registerPhoneError = "Please enter a valid 10-digit number"
                                    isValid = false
                                }

                                if (registerPassword.isEmpty()) {
                                    registerPasswordError = "Password is required"
                                    isValid = false
                                } else if (!SecurityUtils.isStrongPassword(registerPassword)) {
                                    registerPasswordError = "Password too weak"
                                    isValid = false
                                }

                                if (isValid) {
                                    onRegister(
                                        registerName,
                                        registerEmail,
                                        registerPhone,
                                        registerPassword,
                                        {
                                            // Registration succeeded, Activity sets isIdUploadActive = true
                                        },
                                        { error ->
                                            // Error displayed via Toast in Activity callback
                                        }
                                    )
                                } else {
                                }
                            }
                        },
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activePillColor)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Login" else "Register",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Social login divider (outside Card)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E8F0)))
                        Text(text = "   Or continue with   ", color = textSecondary, fontSize = 12.sp)
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E8F0)))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Google login button (outside Card)
                    Card(
                        onClick = {
                            onGoogleSignIn()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Google", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                        }
                    }
                }

                // Bottom redirects (outside Card)
                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Don't have an account? ", color = textSecondary, fontSize = 13.sp)
                        Text(
                            text = "Sign up",
                            color = activePillColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { 
                                focusManager.clearFocus()
                                selectedTab = 1 
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Already have an account? ", color = textSecondary, fontSize = 13.sp)
                        Text(
                            text = "Log in",
                            color = activePillColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { 
                                focusManager.clearFocus()
                                selectedTab = 0 
                            }
                        )
                    }
                }
            }
        }

            }
        }


    }
}
}
