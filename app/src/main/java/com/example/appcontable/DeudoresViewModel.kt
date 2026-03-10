package com.example.appcontable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeudoresViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val userId = Firebase.auth.currentUser?.uid

    val deudores: StateFlow<List<Deudor>> = 
        db.collection("deudores")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Deudor>() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val totalDeudaActual: StateFlow<Double> = deudores.map { list ->
        list.filter { it.estado == DeudorEstado.ACTIVO }.sumOf { it.deudaActual }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deudoresConAtraso: StateFlow<Int> = deudores.map { list ->
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        list.count { deudor ->
            val proximoPagoDate = try { dateFormat.parse(deudor.fechaProximoPago) } catch (e: Exception) { null }
            deudor.estado == DeudorEstado.ACTIVO && deudor.tipoPago != TipoPago.UNICO && proximoPagoDate != null && proximoPagoDate.before(hoy)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun getMovimientos(deudorId: String): Flow<List<Movimiento>> {
        if (deudorId.isBlank()) {
            return emptyFlow()
        }
        return db.collection("deudores").document(deudorId)
            .collection("movimientos")
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Movimiento>() }
    }

    fun addMovimiento(deudor: Deudor, esAbono: Boolean, monto: Double, esPagoCompleto: Boolean) {
        viewModelScope.launch {
            val deudorRef = db.collection("deudores").document(deudor.id)
            
            val movimiento = Movimiento(
                tipo = if (esAbono) TipoMovimiento.ABONO else TipoMovimiento.AUMENTO,
                monto = monto,
                fecha = Timestamp.now()
            )
            deudorRef.collection("movimientos").add(movimiento)

            val factor = if (esAbono) -1 else 1
            val nuevaDeuda = deudor.deudaActual + (monto * factor)
            deudorRef.update("deudaActual", nuevaDeuda)

            if (nuevaDeuda <= 0 && deudor.estado == DeudorEstado.ACTIVO) {
                deudorRef.update("estado", DeudorEstado.PAGADO)
            } else if (nuevaDeuda > 0 && deudor.estado == DeudorEstado.PAGADO) {
                 deudorRef.update("estado", DeudorEstado.ACTIVO)
            }

            if (esPagoCompleto) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                
                // CORRECCIÓN: Si es pago completo, calculamos desde HOY para resetear el ciclo
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }

                when (deudor.tipoPago) {
                    TipoPago.SEMANAL -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                    TipoPago.BISEMANAL -> calendar.add(Calendar.DAY_OF_YEAR, 14)
                    TipoPago.QUINCENAL -> calendar.add(Calendar.DAY_OF_YEAR, 15)
                    TipoPago.MENSUAL -> calendar.add(Calendar.MONTH, 1)
                    TipoPago.UNICO -> { /* No hace nada */ }
                }
                val nuevaFecha = dateFormat.format(calendar.time)
                deudorRef.update("fechaProximoPago", nuevaFecha)
            }
        }
    }

    // Función para calcular los días restantes hasta el próximo pago
    fun calcularDiasRestantes(fechaProximoPago: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            val proximoPago = dateFormat.parse(fechaProximoPago) ?: return 0
            val diff = proximoPago.time - hoy.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            0
        }
    }

    fun deleteDeudor(deudorId: String) {
        viewModelScope.launch {
            db.collection("deudores").document(deudorId).delete()
        }
    }

    fun updateDeudor(deudor: Deudor) {
        viewModelScope.launch {
            db.collection("deudores").document(deudor.id).set(deudor)
        }
    }
}
