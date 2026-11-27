package com.hyperlauncher.ai.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hyperlauncher.ai.HyperLauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Whitelist-only accessibility capture. Master toggle defaults OFF.
 * Only whitelisted packages are captured, and only when the user has
 * enabled capture in Settings AND granted Accessibility access.
 * Captured text is written to the encrypted vault (if unlocked).
 */
class HyperAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            return
        }
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return // never capture self
        scope.launch {
            try {
                val app = application as HyperLauncherApp
                val settings = app.container.settingsRepository
                val enabled = settings.accessibilityCaptureEnabled.first()
                if (!enabled) return@launch
                val whitelist = settings.whitelistedPackages.first()
                if (!whitelist.contains(packageName)) return@launch
                if (!app.container.vaultRepository.isUnlocked()) return@launch
                val text = extractText(event) ?: return@launch
                if (text.isBlank()) return@launch
                app.container.vaultRepository.addNote(
                    title = "Captured from $packageName",
                    body = text.take(4000),
                    sourcePackage = packageName
                )
            } catch (_: Exception) {
                // Never crash the system accessibility service.
            }
        }
    }

    override fun onInterrupt() {
        // No-op: nothing to interrupt.
    }

    private fun extractText(event: AccessibilityEvent): String? {
        val sb = StringBuilder()
        event.text?.forEach { sb.append(it).append(" ") }
        if (sb.isNotBlank()) return sb.toString().trim()
        val root = rootInActiveWindow ?: return null
        try {
            collectText(root, sb)
        } finally {
            root.recycle()
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        try {
            val text = node.text
            if (text != null && text.isNotBlank()) {
                sb.append(text).append(" ")
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                collectText(child, sb)
                child.recycle()
            }
        } catch (_: Exception) {
            // Ignore malformed nodes.
        }
    }
}
