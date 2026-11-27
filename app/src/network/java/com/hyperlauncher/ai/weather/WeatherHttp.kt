package com.hyperlauncher.ai.weather

import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Fixed-purpose request. No device location permission, cookies, API keys or redirects. */
object WeatherHttp {
    fun fetch(latitude: Double,longitude: Double): String {
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
        val url="https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&timezone=auto"
        val connection=URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout=10000;connection.readTimeout=10000;connection.instanceFollowRedirects=false
        try {
            require(connection.responseCode==200) {"Weather server returned HTTP ${connection.responseCode}"}
            return connection.inputStream.use {input ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096)
                while(true) {val n=input.read(buffer);if(n<0) break;require(output.size()+n<=65536) {"Weather response is too large"};output.write(buffer,0,n)}
                output.toString("UTF-8")
            }
        } finally {connection.disconnect()}
    }
}
