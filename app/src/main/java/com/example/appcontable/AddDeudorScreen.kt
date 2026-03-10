package com.example.appcontable

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeudorScreen(
    deudorId: String?,
    viewModel: DeudoresViewModel = viewModel(),
    onSave: (Deudor) -> Unit,
    onNavigateBack: () -> Unit
) {
    val deudores by viewModel.deudores.collectAsState()
    val deudorToEdit = deudores.find { it.id == deudorId }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var deudaInicial by remember { mutableStateOf("") }
    var tipoPago by remember { mutableStateOf(TipoPago.UNICO) }
    var fechaProximoPago by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(deudorToEdit) {
        if (deudorToEdit != null) {
            nombre = deudorToEdit.nombre
            telefono = deudorToEdit.telefono
            tipoPago = deudorToEdit.tipoPago
            fechaProximoPago = deudorToEdit.fechaProximoPago
        }
    }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            fechaProximoPago = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        },
        year, month, day
    )

    val isFormValid by remember(nombre, telefono, deudaInicial, fechaProximoPago, deudorId) {
        derivedStateOf {
            val isDeudaInicialValid = if (deudorId == null) {
                deudaInicial.isNotBlank() && (deudaInicial.toDoubleOrNull() ?: 0.0) > 0.0
            } else {
                true
            }
            nombre.isNotBlank() && telefono.isNotBlank() && isDeudaInicialValid && fechaProximoPago.isNotBlank()
        }
    }

    val title = if (deudorId == null) "Nuevo Deudor" else "Editar Deudor"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Número de teléfono") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                if (deudorId == null) {
                    OutlinedTextField(
                        value = deudaInicial,
                        onValueChange = { newValue ->
                            deudaInicial = newValue.filter { char -> char.isDigit() }
                        },
                        label = { Text("Deuda inicial (Colones)") },
                        visualTransformation = CurrencyVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = tipoPago.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TipoPago.values().forEach { item ->
                            DropdownMenuItem(
                                text = { Text(text = item.displayName) },
                                onClick = {
                                    tipoPago = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedTextField(
                        value = fechaProximoPago,
                        onValueChange = {},
                        label = { Text("Día de pago") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isFormValid) {
                            val deudor = if (deudorToEdit != null) {
                                deudorToEdit.copy(
                                    nombre = nombre,
                                    telefono = telefono,
                                    tipoPago = tipoPago,
                                    fechaProximoPago = fechaProximoPago
                                )
                            } else {
                                val deudaInicialValue = deudaInicial.toDoubleOrNull() ?: 0.0
                                Deudor(
                                    nombre = nombre,
                                    telefono = telefono,
                                    deudaInicial = deudaInicialValue,
                                    deudaActual = deudaInicialValue,
                                    tipoPago = tipoPago,
                                    fechaProximoPago = fechaProximoPago
                                )
                            }
                            onSave(deudor)
                        } else {
                            Toast.makeText(context, "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Guardar Cambios")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
