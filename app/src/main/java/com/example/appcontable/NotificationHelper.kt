package com.example.appcontable

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "app_contable_notifications"
        const val CHANNEL_NAME = "Notificaciones de Deudas"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Canal para recordatorios y avisos de la app"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendWelcomeNotification() {
        val message = "Te agradecemos por activar las notificaciones, te tendremos al tanto de todo!"
        sendNotification(1001, "¡Bienvenida!", message)
    }

    fun sendPaymentReminder(deudorNombre: String, notificationId: Int) {
        val title = "Día de pago"
        val message = "Hoy $deudorNombre debe pagarte su abono"
        sendNotification(notificationId, title, message)
    }

    // Nueva notificación para aviso de mañana
    fun sendTomorrowReminder(deudorNombre: String, notificationId: Int) {
        val title = "Aviso de pago"
        val message = "Mañana $deudorNombre debe pagarte su abono"
        sendNotification(notificationId, title, message)
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        } catch (e: SecurityException) {
            // Manejado en la UI
        }
    }
}
