package com.hyperlauncher.ai.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hyperlauncher.ai.HyperLauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Only package/count data is kept in memory; no notification text, history or network. */
object NotificationBadges {
    private val mutableCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = mutableCounts
    fun replace(counts: Map<String, Int>) { mutableCounts.value = counts }
}

data class BadgeEntry(val packageName: String, val number: Int, val isGroupSummary: Boolean)

fun countBadges(entries: List<BadgeEntry>): Map<String, Int> = entries
    .filterNot { it.isGroupSummary }.groupBy { it.packageName }
    .mapValues { (_, items) -> items.sumOf { it.number.coerceIn(1, 999) }.coerceAtMost(999) }

class LauncherNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var enabled = false
    private var connected = false

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            (application as HyperLauncherApp).container.settingsRepository.drawerPreferences.collect {
                enabled = it.badges
                refresh()
            }
        }
    }

    override fun onListenerConnected() { connected = true; refresh() }
    override fun onListenerDisconnected() { connected = false; NotificationBadges.replace(emptyMap()) }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { refresh() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { refresh() }

    private fun refresh() {
        if (!enabled || !connected) { NotificationBadges.replace(emptyMap()); return }
        val entries = runCatching { activeNotifications.toList() }.getOrDefault(emptyList())
        NotificationBadges.replace(countBadges(entries.map {
            BadgeEntry(it.packageName, it.notification.number,
                it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        }))
    }

    override fun onDestroy() {
        scope.cancel()
        NotificationBadges.replace(emptyMap())
        super.onDestroy()
    }
}
