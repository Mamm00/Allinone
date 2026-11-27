package com.hyperlauncher.ai.util

import android.os.Debug
import android.os.SystemClock

/**
 * Lightweight perf helpers for the <350ms cold-start and 60fps gates.
 * No wake-locks, no polling — measurements are taken on demand only.
 */
object PerformanceMonitor {

    fun measureColdStart(startTimeMs: Long): Long {
        return SystemClock.elapsedRealtime() - startTimeMs
    }

    fun nativeHeapKb(): Long {
        return try {
            Debug.getNativeHeapAllocatedSize() / 1024
        } catch (e: Exception) {
            -1
        }
    }

    fun totalHeapKb(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            (runtime.totalMemory() - runtime.freeMemory()) / 1024
        } catch (e: Exception) {
            -1
        }
    }
}
