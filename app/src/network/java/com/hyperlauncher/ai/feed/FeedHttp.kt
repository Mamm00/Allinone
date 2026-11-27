package com.hyperlauncher.ai.feed

import com.hyperlauncher.feed.FeedParser
import javax.net.ssl.HttpsURLConnection
import java.io.ByteArrayOutputStream
import java.net.URL

/** Separate explicit-purpose transport: never carries OpenRouter keys/cookies. */
object FeedHttp {
    fun fetch(url: String): String {
        require(FeedParser.safeUrl(url)) {"Only HTTPS feeds without embedded credentials are allowed"}
        val connection=URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout=10000;connection.readTimeout=10000;connection.instanceFollowRedirects=false
        connection.setRequestProperty("Accept","application/rss+xml, application/atom+xml, application/xml, text/xml")
        try {
            require(connection.responseCode==200) {"Feed returned HTTP ${connection.responseCode}. Redirects are not followed; use the final HTTPS feed URL."}
            return connection.inputStream.use {input ->
                val bytes=ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val count=input.read(buffer);if(count<0) break;require(bytes.size()+count<=FeedParser.MAX_BYTES) {"Feed exceeds 2 MiB"};bytes.write(buffer,0,count)}
                bytes.toString("UTF-8")
            }
        } finally {connection.disconnect()}
    }
}
