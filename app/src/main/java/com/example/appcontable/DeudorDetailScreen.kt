package com.example.appcontable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DeudorDetailScreen(
    deudor: Deudor?,
    viewModel: DeudoresViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustDebt: (Boolean, Double, Boolean) -> Unit
) {
    val movimientos by viewModel.getMovimientos(deudor?.id ?: "").collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdjustDebtDialog by remember { mutableStateOf(false) }

    var captchaQuestion by remember { mutableStateOf("") }
    var captchaAnswer by remember { mutableStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    val isAnswerCorrect = userAnswer.toIntOrNull() == captchaAnswer

    LaunchedEffect(showDeleteDialog) {
        if (showDeleteDialog) {
            val isAddition = (0..1).random() == 0
            if (isAddition) {
                val num1 = (1..4).random()
                val num2 = (1..(5 - num1)).random()
                captchaAnswer = num1 + num2
                captchaQuestion = "¿Cuánto es $num1 + $num2?"
            } else { // Resta
                val num1 = (2..5).random()
                val num2 = (1..(num1 - 1)).random()
                captchaAnswer = num1 - num2
                captchaQuestion = "¿Cuánto es $num1 - $num2?"
            }
            userAnswer = ""
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Para confirmar, resuelve la siguiente operación:")
                    Text(captchaQuestion, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = userAnswer,
                        onValueChange = { userAnswer = it.filter(Char::isDigit) },
                        label = { Text("Respuesta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { Button(onClick = { onDelete(); showDeleteDialog = false }, enabled = isAnswerCorrect) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showAdjustDebtDialog) {
        AdjustDebtDialog(
            deudaActual = deudor?.deudaActual ?: 0.0,
            tipoPago = deudor?.tipoPago ?: TipoPago.UNICO,
            onDismiss = { showAdjustDebtDialog = false },
            onConfirm = { isAbono, amount, esPagoCompleto ->
                onAdjustDebt(isAbono, amount, esPagoCompleto)
                showAdjustDebtDialog = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = Color.Transparent) {
                Box(modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    Button(
                        onClick = { showAdjustDebtDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("Ajustar Deuda", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (deudor != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 100.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()).padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(deudor.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Row {
                                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = Color.White) }
                                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Eliminar", tint = Color.White) }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tel: ${deudor.telefono}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
                            
                            Column {
                                Text("DEUDA ACTUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                Text(currencyFormat.format(deudor.deudaActual), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("INICIAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text(currencyFormat.format(deudor.deudaInicial), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PAGO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text(deudor.tipoPago.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            
                            val diasRestantes = viewModel.calcularDiasRestantes(deudor.fechaProximoPago)
                            PaymentStatusCard(deudor = deudor, diasRestantes = diasRestantes)
                        }
                    }
                }

                item {
                    Text(
                        text = "Movimientos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                
                if (movimientos.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay movimientos registrados.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    items(movimientos) { movimiento ->
                        MovimientoItem(movimiento, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PaymentStatusCard(deudor: Deudor, diasRestantes: Long) {
    if (deudor.estado == DeudorEstado.PAGADO || deudor.deudaActual <= 0) {
        Surface(
            color = Color(0xFFE8F5E9).copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ESTADO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text("AL DÍA - PAGADO", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
    } else if (deudor.fechaProximoPago.isNotBlank()) {
        Surface(
            color = if (diasRestantes < 0) Color(0xFFFFEBEE).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = if (diasRestantes < 0) "ATRASO" else "PRÓXIMO PAGO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (diasRestantes < 0) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (diasRestantes < 0) "Atrasado por ${Math.abs(diasRestantes)} día(s)" 
                               else "${deudor.fechaProximoPago} (En $diasRestantes días)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (diasRestantes < 0) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MovimientoItem(movimiento: Movimiento, modifier: Modifier = Modifier) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val color = if (movimiento.tipo == TipoMovimiento.ABONO) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val sign = if (movimiento.tipo == TipoMovimiento.ABONO) "-" else "+"

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(dateFormat.format(movimiento.fecha.toDate()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(movimiento.tipo.name.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text("$sign${currencyFormat.format(movimiento.monto)}", color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AdjustDebtDialog(
    deudaActual: Double,
    tipoPago: TipoPago,
    onConfirm: (isAbono: Boolean, amount: Double, esPagoCompleto: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isAbono by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var esPagoCompleto by remember { mutableStateOf(false) }
    val montoNumerico = amount.toDoubleOrNull() ?: 0.0
    val esMontoValido = montoNumerico > 0 && (!isAbono || montoNumerico <= deudaActual)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar Deuda", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { isAbono = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = if (isAbono) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) { Text("Abono") }
                    Button(onClick = { isAbono = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = if (!isAbono) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()) { Text("Aumento") }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit) },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CurrencyVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (isAbono && tipoPago != TipoPago.UNICO) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = esPagoCompleto, onCheckedChange = { esPagoCompleto = it })
                        Text("Es pago completo del período")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(isAbono, montoNumerico, esPagoCompleto) }, enabled = esMontoValido, shape = RoundedCornerShape(12.dp)) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
