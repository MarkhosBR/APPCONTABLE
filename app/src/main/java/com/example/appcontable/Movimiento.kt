package com.example.appcontable

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class TipoMovimiento {
    ABONO,
    AUMENTO
}

data class Movimiento(
    @DocumentId val id: String = "",
    val tipo: TipoMovimiento = TipoMovimiento.ABONO,
    val monto: Double = 0.0,
    val fecha: Timestamp = Timestamp.now()
)
