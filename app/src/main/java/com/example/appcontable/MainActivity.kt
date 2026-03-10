package com.example.appcontable

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.appcontable.ui.theme.AppContableTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkStored = prefs.getBoolean("isDarkTheme", false)

        enableEdgeToEdge()

        // Programar el chequeo de pagos
        scheduleDailyPaymentReminder(this)

        setContent {
            val auth = remember { Firebase.auth }
            val db = remember { Firebase.firestore }
            
            var isDarkTheme by remember { mutableStateOf(isDarkStored) }
            var currentUser by remember { mutableStateOf(auth.currentUser) }

            LaunchedEffect(currentUser) {
                currentUser?.let { user ->
                    try {
                        val doc = db.collection("users").document(user.uid).get().await()
                        if (doc.exists()) {
                            val cloudTheme = doc.getBoolean("isDarkTheme") ?: isDarkTheme
                            if (cloudTheme != isDarkTheme) {
                                isDarkTheme = cloudTheme
                                prefs.edit().putBoolean("isDarkTheme", cloudTheme).apply()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error sync tema", e)
                    }
                }
            }

            AppContableTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme, 
                    onThemeChange = { newTheme ->
                        isDarkTheme = newTheme
                        prefs.edit().putBoolean("isDarkTheme", newTheme).apply()
                        auth.currentUser?.let { user ->
                            db.collection("users").document(user.uid)
                                .set(mapOf("isDarkTheme" to newTheme))
                        }
                    }
                )
            }
        }
    }

    private fun scheduleDailyPaymentReminder(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. Tarea Periódica (Cada 24 horas para el futuro)
        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "PaymentReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )

        // 2. Tarea Inmediata (Solo para pruebas/al abrir la app)
        // Esto ejecutará el chequeo JUSTO AHORA para que veas si funciona
        val immediateWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
    }
}

@Composable
fun AppNavigation(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val auth: FirebaseAuth = remember { Firebase.auth }
    val db = remember { Firebase.firestore }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val gso = remember {
        val serverClientId = context.getString(R.string.default_web_client_id)
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken!!
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            coroutineScope.launch {
                try {
                    auth.signInWithCredential(credential).await()
                    val user = auth.currentUser
                    user?.let {
                        val doc = db.collection("users").document(it.uid).get().await()
                        val prefTheme = doc.getBoolean("isDarkTheme") ?: isDarkTheme
                        onThemeChange(prefTheme)
                    }
                    navController.navigate("deudores") {
                        popUpTo("login") { inclusive = true }
                    }
                } catch (e: Exception) {
                    Log.e("AppNavigation", "Firebase auth failed", e)
                }
            }
        } catch (e: ApiException) {
            Log.e("AppNavigation", "Google sign in failed, code: ${e.statusCode}", e)
        }
    }

    val startDestination = remember { if (auth.currentUser == null) "login" else "deudores" }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                modifier = Modifier.fillMaxSize(),
                onLoginClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) }
            )
        }
        composable("deudores") {
            val viewModel: DeudoresViewModel = viewModel()
            DeudoresScreen(
                viewModel = viewModel,
                onAddDeudor = { navController.navigate("addDeudor") },
                onDeudorClick = { deudorId -> navController.navigate("deudorDetail/$deudorId") },
                onDashboardClick = { navController.navigate("dashboard") }
            )
        }
        composable(
            route = "addDeudor?deudorId={deudorId}",
            arguments = listOf(navArgument("deudorId") { nullable = true })
        ) {
            val viewModel: DeudoresViewModel = viewModel()
            val deudorId = it.arguments?.getString("deudorId")
            AddDeudorScreen(
                deudorId = deudorId,
                viewModel = viewModel,
                onSave = { deudor ->
                    if (deudor.id.isBlank()) {
                        val deudorConUserId = deudor.copy(userId = auth.currentUser?.uid ?: "")
                        coroutineScope.launch { 
                            try {
                                db.collection("deudores").add(deudorConUserId).await()
                            } catch (e: Exception) {
                                Log.e("AppNavigation", "Error al crear deudor", e)
                            }
                        }
                    } else {
                        viewModel.updateDeudor(deudor)
                    }
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "deudorDetail/{deudorId}",
            arguments = listOf(navArgument("deudorId") { type = NavType.StringType })
        ) {
            val viewModel: DeudoresViewModel = viewModel()
            val deudorId = it.arguments?.getString("deudorId") ?: ""
            val deudor by viewModel.deudores.collectAsState()
                .let { state -> derivedStateOf { state.value.find { d -> d.id == deudorId } } }

            DeudorDetailScreen(
                deudor = deudor,
                viewModel = viewModel,
                onEdit = { navController.navigate("addDeudor?deudorId=$deudorId") },
                onDelete = {
                    viewModel.deleteDeudor(deudorId)
                    navController.popBackStack()
                },
                onAdjustDebt = { isAbono, amount, esPagoCompleto ->
                    deudor?.let {
                        viewModel.addMovimiento(it, isAbono, amount, esPagoCompleto)
                    }
                }
            )
        }
        composable("dashboard") {
            val viewModel: DeudoresViewModel = viewModel()
            DashboardScreen(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onSignOut = {
                    coroutineScope.launch {
                        auth.signOut()
                        googleSignInClient.signOut().await()
                        navController.navigate("login") { popUpTo(0) }
                    }
                }
            )
        }
    }
}
