package com.example.appcontable

import com.google.firebase.firestore.DocumentId

enum class TipoPago(val displayName: String) {
    UNICO("Único"),
    SEMANAL("Semanal"),
    BISEMANAL("Bisemanal"),
    QUINCENAL("Quincenal"),
    MENSUAL("Mensual")
}

enum class DeudorEstado {
    ACTIVO,
    PAGADO
}

data class Deudor(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val deudaInicial: Double = 0.0,
    val deudaActual: Double = 0.0,
    val tipoPago: TipoPago = TipoPago.UNICO,
    val fechaProximoPago: String = "",
    val estado: DeudorEstado = DeudorEstado.ACTIVO
)
