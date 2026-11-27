package com.hyperlauncher.ai.ui.gestures

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.data.model.AppInfo

@Composable
fun GesturePreferences(bindings: Map<String,String>,apps: List<AppInfo>,prefix: String="gesture_",
                       onSet: (String,String)->Unit,onDismiss: ()->Unit) {
    var selecting by remember {mutableStateOf<String?>(null)}
    var custom by remember {mutableStateOf("")}
    val context=LocalContext.current
    val names=if(prefix=="gesture_") HomeGesture.entries.map {it.name} else listOf("DOUBLE_TAP","UP","DOWN","LEFT","RIGHT")
    AlertDialog(onDismissRequest=onDismiss,title={Text("Gesture assignments")},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
            if(prefix=="gesture_") {
                Text("Home gestures enabled")
                Switch(bindings["gesturesEnabled"]!="false",{onSet("gesturesEnabled",it.toString())})
            }
            names.forEach { name -> TextButton({selecting=name}) {
                Text("$name: ${bindings[prefix+name] ?: if(prefix=="gesture_") GestureMath.defaultAction(HomeGesture.valueOf(name)) else "none"}")
            } }
            Text("Lock, notification shade and recents need the optional read-free gesture Accessibility service. No content is retrieved. Vault text capture is a separate, optional service.")
            TextButton({runCatching {context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}) {Text("Open Android Accessibility Settings")}
        }
    },confirmButton={TextButton(onDismiss){Text("Done")}})
    selecting?.let { name -> AlertDialog(onDismissRequest={selecting=null},title={Text("Assign $name")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
            listOf("none","drawer","search","notifications","screenOff","drawerSearch","recents","widgets","edit").forEach {action ->
                TextButton({onSet(prefix+name,action);selecting=null}) {Text(action)}
            }
            apps.forEach {app -> TextButton({onSet(prefix+name,"app:"+app.packageName);selecting=null}) {Text("Open ${app.label}")} }
            OutlinedTextField(custom,{custom=it},label={Text("Custom intent URI (intent:…)")})
            TextButton({onSet(prefix+name,"intent:$custom");selecting=null},enabled=custom.startsWith("intent:")) {Text("Use custom intent")}
        }},confirmButton={TextButton({selecting=null}){Text("Cancel")}}) }
}
