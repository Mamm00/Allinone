package com.hyperlauncher.ai.ui.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun HiddenAppsDialog(model: LauncherViewModel,state: LauncherUiState,onDismiss: ()->Unit) {
    val configured by model.hiddenPinConfigured.collectAsState(false)
    var unlocked by remember {mutableStateOf(false)}
    var pin by remember {mutableStateOf("")};var confirm by remember {mutableStateOf("")}
    var error by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope();val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,event ->if(event==Lifecycle.Event.ON_STOP) {unlocked=false;pin="";confirm=""}}
        lifecycle.addObserver(observer);onDispose {lifecycle.removeObserver(observer)}
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Hidden apps")},text={
        Column {
            Text("This hides apps in this launcher, not Android Settings or other launchers. It is not an app lock or an encrypted vault.",style=MaterialTheme.typography.bodySmall)
            if(!unlocked) {
                OutlinedTextField(pin,{pin=it.filter {c->c in '0'..'9'}.take(12)},singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text(if(configured) "PIN" else "Create PIN (6–12 digits)")})
                if(!configured) OutlinedTextField(confirm,{confirm=it.filter {c->c in '0'..'9'}.take(12)},singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text("Confirm PIN")})
                TextButton({scope.launch {busy=true;error=null;runCatching {
                    if(!configured) {require(pin==confirm){"PINs do not match"};model.createPin(pin);unlocked=true}
                    else if(model.unlockHidden(pin)) unlocked=true else error="Wrong PIN"
                }.onFailure {error=it.message};pin="";confirm="";busy=false}},enabled=!busy && pin.length>=6) {Text(if(configured) "Unlock" else "Create")}
            } else LazyColumn(Modifier.heightIn(max=350.dp)) {
                item {Text("Hidden apps")}
                items(state.hiddenApps,key={"hidden_"+it.packageName}) {app ->
                    Row {TextButton({model.onAppLaunched(app.packageName);onDismiss()}) {Text(app.label)};TextButton({model.hideApp(app.packageName,false)}) {Text("Unhide")}}
                }
                item {Text("Hide another app")}
                items(state.allApps,key={"visible_"+it.packageName}) {app ->TextButton({model.hideApp(app.packageName,true)}) {Text("Hide ${app.label}")}}
            }
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }
    },confirmButton={TextButton(onDismiss){Text("Close and lock")}})
}
