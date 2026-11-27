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

# Tink / EncryptedSharedPreferences (security-crypto) missing annotations
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.protobuf.**
-dontwarn com.google.api.**
