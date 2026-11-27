package com.hyperlauncher.ai

import android.app.Application
import com.hyperlauncher.ai.di.AppContainer

class HyperLauncherApp : Application(), androidx.work.Configuration.Provider {
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Network lockdown: in the network flavor, clear the process default
        // network so only the locked-down OkHttp client can reach the network
        // (via an explicit Network). In the offline flavor there is no network
        // stack at all. See NetworkModule for the enforcement point.
        container.networkModule.enforceLockdown()
    }
}
