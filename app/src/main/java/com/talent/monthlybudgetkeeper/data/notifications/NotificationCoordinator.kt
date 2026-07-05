package com.talent.monthlybudgetkeeper.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.talent.monthlybudgetkeeper.MainActivity
import com.talent.monthlybudgetkeeper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun ensureChannels() {
        val manager = context.getSystemService<NotificationManager>() ?: return
        NotificationChannels.all.forEach { spec ->
            manager.createNotificationChannel(
                NotificationChannel(
                    spec.id,
                    spec.name,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = spec.description
                }
            )
        }
    }

    fun canSendNotifications(): Boolean {
        return NotificationPermissionManager.canSendNotifications(context)
    }

    fun sendReminder(
        channelId: String,
        id: Int,
        title: String,
        message: String
    ) {
        if (!canSendNotifications()) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent(id))
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun sendTestNotification() {
        sendReminder(
            channelId = NotificationChannels.BUDGET_ALERTS,
            id = TEST_NOTIFICATION_ID,
            title = "Monthly Budget Keeper test",
            message = "Notifications are working on this device."
        )
    }

    fun cancelNotification(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun cancelAllReminderNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun createOpenAppPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val TEST_NOTIFICATION_ID = 10_000_001
    }
}
