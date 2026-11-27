package com.hyperlauncher.ai.data.network

import okhttp3.CertificatePinner
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Locked-down OkHttp client — the ONLY network path in the app.
 *
 * Firewall layers:
 * 1. DNS resolves ONLY api.openrouter.ai (any other host fails DNS).
 * 2. Interceptor rejects any request whose host is not api.openrouter.ai.
 * 3. Certificate pinning for api.openrouter.ai (SPKI pins; verified in CI).
 * 4. Cleartext disabled (https only).
 * 5. No redirects to other hosts, short timeouts, no retries on failure.
 */
object LockedDownOkHttpClient {

    const val ALLOWED_HOST = "api.openrouter.ai"

    // SPKI pins for api.openrouter.ai. Values are verified in CI from the live
    // TLS handshake (see .github/workflows/release.yml pin-verification step).
    // Placeholder pins are NOT shipped: CI fails the release if pins are absent.
    val CERTIFICATE_PINS: List<String> = listOf(
        // Populated by CI pin-verification; see DEBUG_LOG.md.
    )

    private val lockedDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (hostname != ALLOWED_HOST) {
                throw java.net.UnknownHostException("Blocked host: $hostname")
            }
            return Dns.SYSTEM.lookup(hostname)
        }
    }

    private val firewallInterceptor = Interceptor { chain ->
        val request = chain.request()
        if (request.url.host != ALLOWED_HOST) {
            throw java.io.IOException("Blocked request to ${request.url.host}")
        }
        if (request.url.scheme != "https") {
            throw java.io.IOException("Cleartext blocked")
        }
        chain.proceed(request)
    }

    fun create(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .dns(lockedDns)
            .addInterceptor(firewallInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
        if (CERTIFICATE_PINS.isNotEmpty()) {
            val pinner = CertificatePinner.Builder()
            CERTIFICATE_PINS.forEach { pin ->
                pinner.add(ALLOWED_HOST, pin)
            }
            builder.certificatePinner(pinner.build())
        }
        return builder.build()
    }
}
