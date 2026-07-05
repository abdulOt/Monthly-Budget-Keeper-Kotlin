package com.talent.monthlybudgetkeeper.data.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class NotificationPermissionStatus {
    GRANTED,
    REQUESTABLE,
    RATIONALE_NEEDED,
    BLOCKED,
    SYSTEM_DISABLED
}

object NotificationPermissionManager {
    private const val PREFS_NAME = "notification_permission_state"
    private const val KEY_PROMPT_SHOWN = "prompt_shown"

    fun requiresRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= 33

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return !requiresRuntimePermission() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun areSystemNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canSendNotifications(context: Context): Boolean {
        return hasPostNotificationsPermission(context) && areSystemNotificationsEnabled(context)
    }

    fun markPromptShown(context: Context, userId: String?) {
        prefs(context).edit().putBoolean(promptShownKey(userId), true).apply()
    }

    fun permissionStatus(
        context: Context,
        activity: Activity?,
        userId: String?
    ): NotificationPermissionStatus {
        if (requiresRuntimePermission() && !hasPostNotificationsPermission(context)) {
            if (activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                return NotificationPermissionStatus.RATIONALE_NEEDED
            }
            return if (prefs(context).getBoolean(promptShownKey(userId), false)) {
                NotificationPermissionStatus.BLOCKED
            } else {
                NotificationPermissionStatus.REQUESTABLE
            }
        }
        if (!areSystemNotificationsEnabled(context)) {
            return NotificationPermissionStatus.SYSTEM_DISABLED
        }
        return NotificationPermissionStatus.GRANTED
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun clearPromptState(context: Context, userId: String?) {
        prefs(context).edit().remove(promptShownKey(userId)).apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun promptShownKey(userId: String?): String {
        return if (userId.isNullOrBlank()) {
            KEY_PROMPT_SHOWN
        } else {
            "${KEY_PROMPT_SHOWN}_$userId"
        }
    }
}
