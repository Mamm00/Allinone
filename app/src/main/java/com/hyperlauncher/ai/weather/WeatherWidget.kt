package com.hyperlauncher.ai.weather

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.hyperlauncher.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

object WeatherReport {
    fun parse(json: String): String {
        val current=Json.parseToJsonElement(json).jsonObject["current"]!!.jsonObject
        val temperature=current["temperature_2m"]!!.jsonPrimitive.double
        require(temperature.isFinite() && temperature in -150.0..100.0)
        val code=current["weather_code"]!!.jsonPrimitive.int
        val condition=when(code) {0->"Clear";1,2,3->"Cloudy";45,48->"Fog";in 51..67->"Rain";in 71..77,85,86->"Snow";in 80..82->"Showers";in 95..99->"Thunderstorm";else->"Weather code $code"}
        return "$temperature °C · $condition"
    }
}
class WeatherRefreshWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result=withContext(Dispatchers.IO) {
        try {
            val lat=inputData.getDouble("latitude",Double.NaN);val lon=inputData.getDouble("longitude",Double.NaN)
            val report=WeatherReport.parse(WeatherHttp.fetch(lat,lon))
            val json=buildJsonObject {put("coordinate","$lat,$lon");put("report",report);put("updated",System.currentTimeMillis())}
            val tmp=File(applicationContext.filesDir,"weather.tmp");tmp.writeText(json.toString());check(tmp.renameTo(File(applicationContext.filesDir,"weather.json")))
            Result.success()
        } catch(e: Exception) {
            if(e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(workDataOf("error" to "Weather refresh failed. Saved weather is unchanged."))
        }
    }
}

@Composable
fun WeatherWidget(coordinate: String,onCoordinate: (String)->Unit) {
    val context=LocalContext.current;val manager=remember {WorkManager.getInstance(context)}
    val jobs by manager.getWorkInfosForUniqueWorkFlow("weather-explicit-refresh").collectAsState(emptyList())
    val busy=jobs.any {!it.state.isFinished}
    val saved by produceState<String?>(null,jobs,coordinate) {value=withContext(Dispatchers.IO) {runCatching {
        val data=Json.parseToJsonElement(File(context.filesDir,"weather.json").readText()).jsonObject
        if(data["coordinate"]!!.jsonPrimitive.content!=coordinate) null else
            data["report"]!!.jsonPrimitive.content+"\nUpdated "+java.text.DateFormat.getDateTimeInstance().format(java.util.Date(data["updated"]!!.jsonPrimitive.long))
    }.getOrNull()}}
    var configuring by remember {mutableStateOf(false)};var input by remember(coordinate) {mutableStateOf(coordinate)}
    val pair=input.split(',').map {it.trim().toDoubleOrNull()}
    val lat=pair.getOrNull(0);val lon=pair.getOrNull(1)
    val valid=pair.size==2 && lat!=null && lon!=null && lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
    Text(saved ?: "Weather · no saved forecast",maxLines=4)
    TextButton({configuring=true}) {Text(if(busy) "Refreshing…" else "Configure / refresh")}
    if(configuring) AlertDialog(onDismissRequest={configuring=false},title={Text("Weather · Open-Meteo")},text={Column {
        Text("Enter latitude,longitude manually. Refresh sends these coordinates and your IP address to Open-Meteo. No device location access or background polling. Saved weather stays on device.")
        OutlinedTextField(input,{input=it.take(60)},label={Text("Latitude,longitude")},singleLine=true)
        if(BuildConfig.FLAVOR!="network") Text("Network edition required for refresh.")
        jobs.firstOrNull {it.state==WorkInfo.State.FAILED}?.outputData?.getString("error")?.let {Text(it)}
        Text("Data: Open-Meteo.com · CC BY 4.0",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton({
        val normalized="$lat,$lon";onCoordinate(normalized)
        manager.enqueueUniqueWork("weather-explicit-refresh",ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WeatherRefreshWorker>().setInputData(workDataOf("latitude" to lat!!,"longitude" to lon!!)).build())
        configuring=false
    },enabled=valid && !busy && BuildConfig.FLAVOR=="network") {Text("Agree and refresh")}},dismissButton={TextButton({configuring=false}){Text("Cancel")}})
}
