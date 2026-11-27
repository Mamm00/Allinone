package com.hyperlauncher.ai.ui.workspace

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.BuildConfig
import com.hyperlauncher.ai.R
import com.hyperlauncher.ai.ui.settings.matchesPreference
import kotlin.math.roundToInt

@Composable
private fun PreferenceGroup(query: String,@StringRes label: Int,content: @Composable ()->Unit) {
    val title=stringResource(label)
    if(matchesPreference(title,query)) {HorizontalDivider();Text(title,style=MaterialTheme.typography.titleSmall);content()}
}
@Composable
fun WorkspacePreferencesDialog(layout: WorkspaceLayout,onSave: (WorkspaceLayout)->Unit,
    onBackup: ()->Unit,onFeed: ()->Unit,onWallpaper: ()->Unit,onGestures: ()->Unit,
    onFolder: ()->Unit,onAddDock: ()->Unit,onAppearance: ()->Unit,onDismiss: ()->Unit) {
    var query by remember {mutableStateOf("")}
    fun number(key: String,default: Float,min: Float,max: Float)=layout.options[key]?.toFloatOrNull()?.takeIf {it.isFinite()}?.coerceIn(min,max) ?:default
    AlertDialog(onDismissRequest=onDismiss,title={Text("Layout for this display/orientation")},text={
        Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query,{query=it},singleLine=true,label={Text(stringResource(R.string.search_preferences))})
            PreferenceGroup(query,R.string.pref_appearance) {TextButton(onAppearance) {Text("Fonts, weight, icons and immersive home")}}
            if(BuildConfig.ENABLE_IMPORT) PreferenceGroup(query,R.string.pref_backup) {TextButton(onBackup) {Text("Open import / backup")}}
            if(BuildConfig.ENABLE_FEED) PreferenceGroup(query,R.string.pref_feed) {
                Switch(layout.options["feedEnabled"]=="true",{onSave(layout.option("feedEnabled",it.toString()))})
                TextButton(onFeed) {Text("Open Your Feed")}
            }
            if(BuildConfig.ENABLE_GLASS) PreferenceGroup(query,R.string.pref_glass) {
                listOf("material","opaque","frosted","liquid").forEach {theme->TextButton({onSave(layout.option("theme",theme))}) {Text(theme)}}
                TextButton(onWallpaper) {Text("Choose/apply wallpaper for glass sampling")}
                Text("Android can restrict wallpaper pixels. Applying an image here supplies a local snapshot; no image is uploaded.")
                Text("Blur radius")
                Slider(number("blur",16f,0f,50f),{onSave(layout.option("blur",it.toString()))},valueRange=0f..50f)
                Text("Effect intensity")
                Slider(number("glassIntensity",.4f,0f,1f),{onSave(layout.option("glassIntensity",it.toString()))})
                Text("White frost intensity")
                Slider(number("overlay",.2f,0f,.85f),{onSave(layout.option("overlay",it.toString()))},valueRange=0f.. .85f)
            }
            PreferenceGroup(query,R.string.pref_gestures) {
                TextButton(onGestures) {Text("Gesture assignments")}
                TextButton(onFolder,enabled=layout.dockApps.size>=2) {Text("Create folder from dock apps")}
            }
            PreferenceGroup(query,R.string.pref_dock) {
                Text("Slots: ${layout.options["dockSlots"] ?: "5"}")
                Slider(number("dockSlots",5f,4f,6f),{onSave(layout.option("dockSlots",it.roundToInt().toString()))},valueRange=4f..6f,steps=1)
                Text("Search pill")
                Switch(layout.options["dockSearch"]!="false",{onSave(layout.option("dockSearch",it.toString()))})
                TextButton(onAddDock) {Text("Add dock app/page")}
                TextButton({onSave(layout.option("dock",""))}) {Text("Clear dock")}
            }
            PreferenceGroup(query,R.string.pref_grid) {
                Text("Gridless placement")
                Switch(layout.gridless,{onSave(layout.copy(gridless=it))})
                Text("Columns: ${layout.columns}")
                Slider(layout.columns.toFloat(),{onSave(layout.copy(columns=it.roundToInt()))},valueRange=2f..8f,steps=5)
                Text("Rows: ${layout.rows}")
                Slider(layout.rows.toFloat(),{onSave(layout.copy(rows=it.roundToInt()))},valueRange=2f..12f,steps=9)
                Text("Pages: ${layout.pages}")
                Slider(layout.pages.toFloat(),{onSave(layout.copy(pages=it.roundToInt()))},valueRange=1f..7f,steps=5)
                Text("Reducing pages hides their contents without deleting them. Home pages loop circularly.")
            }
        }
    },confirmButton={TextButton(onDismiss){Text("Done")}})
}
