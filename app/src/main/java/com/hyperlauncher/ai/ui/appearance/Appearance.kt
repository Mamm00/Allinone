package com.hyperlauncher.ai.ui.appearance

import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalAppearance=staticCompositionLocalOf<Map<String,String>> {emptyMap()}
val LocalClockFont=staticCompositionLocalOf {FontFamily.Default}
@Composable
fun appearanceFont(options: Map<String,String>,clock: Boolean=false): FontFamily {
    val context=LocalContext.current
    val key=if(clock) "clockFont" else "font"
    val name=options[key] ?:"system"
    val version=options["${key}Version"].orEmpty()
    val family by produceState(FontFamily.Default,name,version) {
        value=withContext(Dispatchers.IO) {
            when(name) {"serif"->FontFamily.Serif;"monospace"->FontFamily.Monospace;"sans"->FontFamily.SansSerif
                "downloaded"->runCatching {FontFamily(android.graphics.Typeface.createFromFile(java.io.File(context.filesDir,"$key.ttf")))}.getOrDefault(FontFamily.Default)
                else->FontFamily.Default}
        }
    }
    return family
}
fun TextStyle.withAppearance(family: FontFamily,options: Map<String,String>)=copy(fontFamily=family,
    fontWeight=FontWeight((options["fontWeight"]?.toIntOrNull() ?:400).coerceIn(100,900)))
