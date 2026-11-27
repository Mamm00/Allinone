package com.hyperlauncher.ai.ui.workspace

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hyperlauncher.glass.GlassView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val glassCache=object : LruCache<String,Bitmap>(12*1024*1024) {
    override fun sizeOf(key: String,value: Bitmap)=value.byteCount
}

/** Background is sampled at committed widget bounds, not continuously during drag. */
@Composable
fun GlassBackdrop(item: Placement,options: Map<String,String>) {
    val context=LocalContext.current
    val manager=remember {android.app.WallpaperManager.getInstance(context)}
    var wallpaperId by remember {mutableIntStateOf(manager.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM))}
    DisposableEffect(manager) {
        val listener=android.app.WallpaperManager.OnColorsChangedListener {_,_-> wallpaperId=manager.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM)}
        manager.addOnColorsChangedListener(listener,android.os.Handler(android.os.Looper.getMainLooper()))
        onDispose {manager.removeOnColorsChangedListener(listener)}
    }
    val version=options["wallpaperVersion"].orEmpty()
    val crop by produceState<Bitmap?>(null,item.x,item.y,item.width,item.height,version,wallpaperId) {
        value=withContext(Dispatchers.IO) {
            if(version!=wallpaperId.toString()) return@withContext null
            val key="$version:${item.x}:${item.y}:${item.width}:${item.height}"
            glassCache.get(key) ?: runCatching {
                val file=java.io.File(context.filesDir,"workspace-wallpaper.jpg")
                val source=BitmapFactory.decodeFile(file.path) ?: return@runCatching null
                val x=(source.width*item.x).toInt().coerceIn(0,source.width-1)
                val y=(source.height*item.y).toInt().coerceIn(0,source.height-1)
                val w=(source.width*item.width).toInt().coerceIn(1,source.width-x)
                val h=(source.height*item.height).toInt().coerceIn(1,source.height-y)
                Bitmap.createBitmap(source,x,y,w,h).also {glassCache.put(key,it)}
            }.getOrNull()
        }
    }
    AndroidView(factory={GlassView(it)},modifier=Modifier.fillMaxSize(),update={
        it.configure(crop,options["blur"]?.toFloatOrNull() ?: 16f,
            if(options["theme"]=="liquid") options["glassIntensity"]?.toFloatOrNull() ?: .4f else 0f,
            options["overlay"]?.toFloatOrNull() ?: .2f)
    })
}
