package com.hyperlauncher.ai.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.hyperlauncher.ai.data.local.KeyStoreManager

/**
 * Network flavor: clears the process default network so only the
 * locked-down OkHttp client (via an explicit Network) can reach the network.
 */
class NetworkModule private constructor(val openRouterService: OpenRouterService) {

    fun enforceLockdown() {
        try {
            val cm = contextRef?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.setProcessDefaultNetwork(null)
        } catch (_: Exception) {
            // Best-effort: the DNS + interceptor firewall still applies.
        }
    }

    companion object {
        private var contextRef: Context? = null

        fun create(context: Context, keyStoreManager: KeyStoreManager): NetworkModule {
            contextRef = context.applicationContext
            return NetworkModule(NetworkOpenRouterService())
        }
    }
}
