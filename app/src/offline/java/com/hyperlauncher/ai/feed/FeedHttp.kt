package com.hyperlauncher.ai.feed
object FeedHttp {
    fun fetch(url: String): String = throw UnsupportedOperationException("Offline build cannot fetch feeds. Use the network build for explicit refreshes.")
}
