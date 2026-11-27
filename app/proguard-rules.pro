# Keep Room entities and DAOs
-keep class com.hyperlauncher.ai.data.local.entity.** { *; }
-keep class com.hyperlauncher.ai.data.local.dao.** { *; }
-keep class com.hyperlauncher.ai.data.local.AppDatabase { *; }
# Keep SQLCipher
-keep class net.sqlcipher.** { *; }
# Keep accessibility service
-keep class com.hyperlauncher.ai.service.** { *; }
# Keep OkHttp (network flavor)
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
