package com.example.appcontable

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", false)

        // Si el usuario tiene las notificaciones apagadas, no hacemos nada
        if (!notificationsEnabled) return Result.success()

        // Esperar a que Firebase se estabilice (importante al abrir la app)
        var userId: String? = null
        repeat(5) { 
            userId = Firebase.auth.currentUser?.uid
            if (userId != null) return@repeat
            delay(1500) 
        }

        if (userId == null) return Result.success()

        val db = Firebase.firestore
        val notificationHelper = NotificationHelper(context)

        return try {
            val snapshot = db.collection("deudores")
                .whereEqualTo("userId", userId)
                .whereEqualTo("estado", "ACTIVO")
                .get()
                .await()

            val deudores = snapshot.toObjects<Deudor>()

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val hoyStr = dateFormat.format(Calendar.getInstance().time)
            val mañana = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val mañanaStr = dateFormat.format(mañana.time)

            deudores.forEachIndexed { index, deudor ->
                val fechaDeudor = deudor.fechaProximoPago.trim()
                
                if (fechaDeudor == hoyStr) {
                    notificationHelper.sendPaymentReminder(deudor.nombre, 2000 + index)
                } else if (fechaDeudor == mañanaStr) {
                    notificationHelper.sendTomorrowReminder(deudor.nombre, 3000 + index)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error: ${e.message}")
            Result.retry()
        }
    }
}
