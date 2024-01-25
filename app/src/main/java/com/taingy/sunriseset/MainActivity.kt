package com.taingy.sunriseset

import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.taingy.sunriseset.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btLangCh.setOnClickListener {
            changeLanguage("zh")
        }
        binding.btLangEn.setOnClickListener {
            changeLanguage("en")
        }

        getTime()
    }

    private fun getTime() {
        GlobalScope.launch(Dispatchers.Main) {
            val sunriseDeferred = async(Dispatchers.IO) { fetchTime("sunrise") }
            val sunsetDeferred = async(Dispatchers.IO) { fetchTime("sunset") }

            val sunriseTime = sunriseDeferred.await()
            val sunsetTime = sunsetDeferred.await()

            if (sunriseTime != null && sunsetTime != null) {
                val localizedSunrise = getLocalizedTime(sunriseTime)
                val localizedSunset = getLocalizedTime(sunsetTime)

                binding.textViewSunrise.text =
                    "${getString(R.string.SunriseTime)} $localizedSunrise"
                binding.textViewSunset.text =
                    "${getString(R.string.SunsetTime)} $localizedSunset"
            }
        }
    }

    private fun getLocalizedTime(time: LocalDateTime): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(
            time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    private suspend fun fetchTime(type: String): LocalDateTime? {
        return try {
            val apiUrl =
                URL("https://api.sunrise-sunset.org/json?lat=37.7749&lng=-122.4194&formatted=0")
            val urlConnection: HttpURLConnection = apiUrl.openConnection() as HttpURLConnection
            try {
                val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val jsonResponse = JSONObject(response.toString())
                val timeUTC = jsonResponse.getJSONObject("results").getString(type)
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
                val dateTime = formatter.parse(timeUTC)
                LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault())
            } finally {
                urlConnection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun changeLanguage(code: String) {
        val config = resources.configuration

        val locale = Locale(code)
        Locale.setDefault(locale)
        config.locale = locale

        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}