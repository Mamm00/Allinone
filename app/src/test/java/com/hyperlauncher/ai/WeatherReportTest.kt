package com.hyperlauncher.ai
import com.hyperlauncher.ai.weather.WeatherReport
import org.junit.Assert.*
import org.junit.Test
class WeatherReportTest {
    @Test fun parsesCurrentConditions() {
        assertEquals("21.5 °C · Clear",WeatherReport.parse("""{"current":{"temperature_2m":21.5,"weather_code":0}}"""))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsImpossibleTemperature() {
        WeatherReport.parse("""{"current":{"temperature_2m":999,"weather_code":0}}""")
    }
}
