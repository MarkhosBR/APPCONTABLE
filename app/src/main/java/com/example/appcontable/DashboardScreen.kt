package com.example.appcontable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DeudoresViewModel = viewModel(),
    onSignOut: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val deudores by viewModel.deudores.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // Estado de las notificaciones guardado en SharedPreferences
    var notificationsEnabled by remember { 
        mutableStateOf(prefs.getBoolean("notifications_enabled", false)) 
    }

    // Lógica para pedir permisos (solo Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            prefs.edit().putBoolean("notifications_enabled", true).apply()
            NotificationHelper(context).sendWelcomeNotification()
        } else {
            notificationsEnabled = false
            prefs.edit().putBoolean("notifications_enabled", false).apply()
            Toast.makeText(context, "Se requiere permiso para las notificaciones", Toast.LENGTH_SHORT).show()
        }
    }

    val dashboardStats by remember(deudores) {
        derivedStateOf {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val totalDeuda = deudores.sumOf { it.deudaActual }
            val deudoresActivos = deudores.count { it.deudaActual > 0 }
            val deudoresPagados = deudores.count { it.deudaActual == 0.0 }
            val deudoresAtrasados = deudores.count { deudor ->
                val fechaPago = try { dateFormat.parse(deudor.fechaProximoPago) } catch (e: Exception) { null }
                deudor.deudaActual > 0 && fechaPago != null && fechaPago.before(hoy)
            }

            listOf(
                DashboardStat("Deuda total", formatCurrency(totalDeuda)),
                DashboardStat("Deudores activos", deudoresActivos.toString()),
                DashboardStat("Deudores pagados", deudoresPagados.toString()),
                DashboardStat("Deudores atrasados", deudoresAtrasados.toString())
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.Transparent) {
                Box(modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())) {
                    Text(
                        text = "Resumen",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                dashboardStats.forEach { stat ->
                    StatCard(stat = stat)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Card de configuración actualizada con lógica de notificaciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Configuración",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        SettingItem(label = "Modo Oscuro", checked = isDarkTheme, onCheckedChange = onThemeChange)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SettingItem(
                            label = "Permitir Notificaciones", 
                            checked = notificationsEnabled, 
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // Al intentar activar: comprobar permiso
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                        if (status == PackageManager.PERMISSION_GRANTED) {
                                            notificationsEnabled = true
                                            prefs.edit().putBoolean("notifications_enabled", true).apply()
                                            NotificationHelper(context).sendWelcomeNotification()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        // Android < 13 no requiere permiso en runtime
                                        notificationsEnabled = true
                                        prefs.edit().putBoolean("notifications_enabled", true).apply()
                                        NotificationHelper(context).sendWelcomeNotification()
                                    }
                                } else {
                                    // Al desactivar simplemente apagamos
                                    notificationsEnabled = false
                                    prefs.edit().putBoolean("notifications_enabled", false).apply()
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatCard(stat: DashboardStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

data class DashboardStat(val label: String, val value: String)

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "CR")).format(value)
}
