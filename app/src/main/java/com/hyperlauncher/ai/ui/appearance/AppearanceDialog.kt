package com.hyperlauncher.ai.ui.appearance

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.hyperlauncher.ai.HyperLauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun AppearanceDialog(onDismiss: ()->Unit) {
    val context=LocalContext.current
    val repository=(context.applicationContext as HyperLauncherApp).container.settingsRepository
    val options by repository.appearance.collectAsState(emptyMap())
    val scope=rememberCoroutineScope()
    fun set(key: String,value: String) {scope.launch {repository.setAppearance(key,value)}}
    var googleName by remember {mutableStateOf("Roboto")};var message by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    val packs by produceState<List<Pair<String,String>>>(emptyList()) {value=withContext(Dispatchers.IO) {
        listOf("org.adw.launcher.THEMES","com.novalauncher.THEME","com.anddoes.launcher.THEME").flatMap {action ->
            context.packageManager.queryIntentActivities(Intent(action),0).map {it.activityInfo.packageName to it.loadLabel(context.packageManager).toString()}
        }.distinctBy {it.first}
    }}
    suspend fun download(clock: Boolean) {
        busy=true
        runCatching {withContext(Dispatchers.IO) {
            require(googleName.matches(Regex("[A-Za-z0-9 -]{1,60}"))) {"Enter a font family name"}
            val info=context.packageManager.getPackageInfo("com.google.android.gms",PackageManager.GET_SIGNING_CERTIFICATES)
            val certs=info.signingInfo?.apkContentsSigners?.map {it.toByteArray()} ?:error("Google Play font provider unavailable")
            val weight=(options["fontWeight"]?.toIntOrNull() ?:400).coerceIn(100,900)
            val request=FontRequest("com.google.android.gms.fonts","com.google.android.gms","name=$googleName&weight=$weight",listOf(certs))
            val result=FontsContractCompat.fetchFonts(context,null,request)
            require(result.statusCode==0) {"Font provider rejected this request"}
            val font=result.fonts?.firstOrNull {it.resultCode==0} ?:error("Font not found")
            val key=if(clock) "clockFont" else "font"
            val temporary=java.io.File(context.filesDir,"$key.tmp")
            context.contentResolver.openInputStream(font.uri)!!.use {input ->temporary.outputStream().use {output ->
                val buffer=ByteArray(8192);var total=0
                while(true) {val n=input.read(buffer);if(n<0) break;total+=n;require(total<=8*1024*1024);output.write(buffer,0,n)}
            }}
            android.graphics.Typeface.createFromFile(temporary)
            check(temporary.renameTo(java.io.File(context.filesDir,"$key.ttf")))
            repository.setAppearance(key,"downloaded");repository.setAppearance("${key}Version",System.currentTimeMillis().toString())
        }}.onSuccess {message="Font downloaded and cached locally."}.onFailure {message=it.message ?:"Font download failed"}
        busy=false
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Appearance")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState())) {
        Text("System font")
        listOf("system","sans","serif","monospace").forEach {font ->Row {TextButton({set("font",font)}){Text("UI: $font")};TextButton({set("clockFont",font)}){Text("Clock: $font")}}}
        Text("Font weight: ${options["fontWeight"] ?: "400"}")
        Slider(options["fontWeight"]?.toFloatOrNull()?.coerceIn(100f,900f) ?:400f,{set("fontWeight",(it/100).roundToInt().times(100).toString())},valueRange=100f..900f,steps=7)
        OutlinedTextField(googleName,{googleName=it},label={Text("Google Fonts family")})
        Text("Download explicitly requests the font through Google Play Services. It may contact Google. No download occurs while typing or on startup.",style=MaterialTheme.typography.bodySmall)
        Row {TextButton({scope.launch {download(false)}},enabled=!busy){Text("Download UI font")};TextButton({scope.launch {download(true)}},enabled=!busy){Text("Clock font")}}
        Text("Uniform icon mask (none preserves the system adaptive icon)")
        listOf("none","circle","squircle","rounded","teardrop").forEach {shape ->TextButton({set("iconShape",shape)}){Text(shape)}}
        TextButton({set("iconPack","")}){Text("System icons")}
        packs.forEach {(pkg,label)->TextButton({set("iconPack",pkg)}){Text(label)}}
        Text("Immersive home: hide system bars; swipe an edge to reveal them")
        Switch(options["immersive"]=="true",{set("immersive",it.toString())})
        message?.let {Text(it)}
    }},confirmButton={TextButton(onDismiss){Text("Done")}})
}
