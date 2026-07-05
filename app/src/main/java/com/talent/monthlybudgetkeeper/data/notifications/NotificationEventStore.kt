package com.talent.monthlybudgetkeeper.data.notifications

import android.content.Context
import android.content.SharedPreferences

object NotificationEventStore {
    private const val PREFS_NAME = "notification_event_store"
    private const val KEY_PREFIX = "event"

    fun shouldNotify(
        context: Context,
        userId: String,
        eventKey: String
    ): Boolean {
        val storageKey = storageKey(userId, eventKey)
        val prefs = prefs(context)
        val alreadyTracked = prefs.contains(storageKey)
        if (!alreadyTracked) {
            prefs.edit().putLong(storageKey, System.currentTimeMillis()).apply()
        }
        return !alreadyTracked
    }

    fun removeInactiveEventKeys(
        context: Context,
        userId: String,
        activeEventKeys: Set<String>
    ): List<String> {
        val prefix = userPrefix(userId)
        val inactiveKeys = prefs(context).all.keys
            .filter { it.startsWith(prefix) }
            .mapNotNull { storedKey ->
                val eventKey = storedKey.removePrefix(prefix)
                eventKey.takeUnless(activeEventKeys::contains)
            }

        if (inactiveKeys.isNotEmpty()) {
            prefs(context).edit().apply {
                inactiveKeys.forEach { remove(storageKey(userId, it)) }
            }.apply()
        }
        return inactiveKeys
    }

    fun clearUserEvents(
        context: Context,
        userId: String
    ): List<String> {
        return removeInactiveEventKeys(
            context = context,
            userId = userId,
            activeEventKeys = emptySet()
        )
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun storageKey(userId: String, eventKey: String): String {
        return "${userPrefix(userId)}$eventKey"
    }

    private fun userPrefix(userId: String): String {
        return "${KEY_PREFIX}_${userId}_"
    }
}
