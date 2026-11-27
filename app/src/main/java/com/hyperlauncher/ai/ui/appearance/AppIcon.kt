package com.hyperlauncher.ai.ui.appearance

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.hyperlauncher.ai.data.model.AppInfo
import com.hyperlauncher.importer.IconPackParser
import com.hyperlauncher.importer.IconPackMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val packCache=android.util.LruCache<String,IconPackMapping>(6)
@Composable
fun AppIcon(app: AppInfo,size: Dp=44.dp) {
    val context=LocalContext.current;val options=LocalAppearance.current;val pack=options["iconPack"].orEmpty()
    var day by remember {mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH))}
    DisposableEffect(context) {
        val receiver=object:BroadcastReceiver(){override fun onReceive(c: Context?,i: Intent?){day=java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)}}
        ContextCompat.registerReceiver(context,receiver,IntentFilter().apply {addAction(Intent.ACTION_DATE_CHANGED);addAction(Intent.ACTION_TIME_CHANGED);addAction(Intent.ACTION_TIMEZONE_CHANGED)},ContextCompat.RECEIVER_EXPORTED)
        onDispose {context.unregisterReceiver(receiver)}
    }
    val icon by produceState<Bitmap?>(null,app.packageName,app.iconPng,pack,day) {value=withContext(Dispatchers.IO) {
        val packed=if(pack.isBlank()) null else runCatching {
            val pm=context.packageManager;val resources=pm.getResourcesForApplication(pack)
            val version=pm.getPackageInfo(pack,0).lastUpdateTime
            val mapping=packCache.get("$pack:$version") ?: run {
                val xmlId=resources.getIdentifier("appfilter","xml",pack)
                val parsed=if(xmlId==0) {
                    val input=resources.assets.open("appfilter.xml").bufferedReader().use {it.readText()}
                    IconPackParser.parse(input)
                } else {
                    val icons=mutableMapOf<String,String>();val calendars=mutableMapOf<String,String>()
                    resources.getXml(xmlId).use {xml ->
                        while(xml.eventType!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                            if(xml.eventType==org.xmlpull.v1.XmlPullParser.START_TAG) {
                                val component=xml.getAttributeValue(null,"component")?.let(IconPackParser::component)
                                if(component!=null && xml.name=="item") {
                                    val id=xml.getAttributeResourceValue(null,"drawable",0)
                                    val name=if(id!=0) "#$id" else xml.getAttributeValue(null,"drawable")
                                    if(name!=null) icons[component]=name
                                }
                                if(component!=null && xml.name=="calendar") xml.getAttributeValue(null,"prefix")?.let {calendars[component]=it}
                            }
                            xml.next()
                        }
                    }
                    IconPackMapping(icons,calendars)
                }
                parsed.also {packCache.put("$pack:$version",it)}
            }
            val component=pm.getLaunchIntentForPackage(app.packageName)?.component?.flattenToString().orEmpty()
            val key=IconPackParser.component(component)
            val drawable=mapping.calendars[key]?.let {it+day} ?:mapping.icons[key] ?:error("No icon mapping")
            val id=drawable.removePrefix("#").toIntOrNull() ?:resources.getIdentifier(drawable,"drawable",pack)
            resources.getDrawable(id,null).toBitmap(96,96)
        }.getOrNull()
        packed ?:app.iconPng?.let {BitmapFactory.decodeByteArray(it,0,it.size)}
    }}
    val shape=when(options["iconShape"]) {"circle"->CircleShape;"squircle"->RoundedCornerShape(35);"rounded"->RoundedCornerShape(24)
        "teardrop"->RoundedCornerShape(topStart=50.dp,topEnd=50.dp,bottomStart=50.dp,bottomEnd=2.dp);else->null}
    val mask=if(shape!=null) Modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant) else Modifier
    Box(Modifier.size(size).then(mask),contentAlignment=Alignment.Center) {
        icon?.let {Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),colorFilter=if(app.isSuspended) ColorFilter.colorMatrix(ColorMatrix().apply {setToSaturation(0f)}) else null)} ?:Text(app.label.take(1))
        if(app.packageName in listOf("com.google.android.calendar","com.android.calendar")) Surface(Modifier.align(Alignment.BottomCenter),color=MaterialTheme.colorScheme.surface.copy(alpha=.9f)) {Text(day.toString(),style=MaterialTheme.typography.labelSmall)}
    }
}
