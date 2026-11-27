package com.hyperlauncher.ai.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/** Separate from opt-in vault capture. Never requests window content or reads events. */
class GestureAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { connected=WeakReference(this) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { connected=null; super.onDestroy() }
    companion object {
        private var connected: WeakReference<GestureAccessibilityService>?=null
        fun perform(action: Int): Boolean = connected?.get()?.performGlobalAction(action) ?: false
    }
}
