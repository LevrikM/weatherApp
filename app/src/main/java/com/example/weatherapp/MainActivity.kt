package com.example.weatherapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.time.LocalTime
import java.util.Calendar
import java.util.concurrent.Executors


class WeatherInfo {

    var temp_true = ""
    var temp_feel = ""
    var pressure = ""
    var humidity = ""
    var opady = ""
    var weatherIcon = ""

    var descriptionOfWeather = ""

    private suspend fun fetchHTMLContent(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        return withContext(Dispatchers.IO) {
            val response: Response =
                client.newCall(request).execute()
            return@withContext response.body?.string() ?: ""
        }
    }

    private fun parseWeatherData(htmlContent: String){
        val doc: Document = Jsoup.parse(htmlContent)
        /*weatherIcon = "https://sinst.fwdcdn.com/img/weatherImg/s/d300.gif"*/
        weatherIcon = doc.select(".cur")[1].child(0).child(0).attr("src")
        temp_true = doc.select(".cur")[2].text()
        temp_feel = doc.select(".cur")[3].text()
        pressure = doc.select(".cur")[4].text()
        humidity = doc.select(".cur")[5].text()
        opady = doc.select(".cur")[7].text()

        descriptionOfWeather = doc.select(".description")[0].text()

    }

    suspend fun fetchDataAndParse(url : String)
    {
        val htmlContent = fetchHTMLContent(url)
        parseWeatherData(htmlContent)
    }
}

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var labelTemp : TextView
    lateinit var labelPressure : TextView
    lateinit var labelHumidity : TextView
    lateinit var labelFeelsLike : TextView
    lateinit var labelPrecipitation : TextView
    lateinit var labelCity : TextView
    lateinit var labelDescription : TextView
    lateinit var weatherIconView : ImageView
    //info labels
    lateinit var infoPressure : TextView
    lateinit var infoHumidity : TextView
    lateinit var infoFeelsLike : TextView
    lateinit var infoPrecipitation : TextView

    //
    lateinit var textViews: Array<TextView>

    lateinit var showInfoCheckBox : CheckBox
    //time of change backgrounds
    val MORNING = 6
    val EVENING = 18
    val NIGHT = 22

    var currentBackground: Int = 0

    // array for countries in Ukraine
    var countries = arrayOf("Житомир", "Київ", "Одеса", "Миколаїв", "Крим")

    @SuppressLint("MissingInflatedId")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
        //

        //set background by time
        val currentHour = LocalTime.now().hour
        when {
            currentHour in MORNING until EVENING -> setAppBackground(MORNING)
            currentHour in EVENING until NIGHT -> setAppBackground(EVENING)
            else -> setAppBackground(NIGHT)
        }
        //

        // info labels
        infoPressure = findViewById(R.id.infoPressure)
        infoHumidity = findViewById(R.id.infoHumidity)
        infoFeelsLike = findViewById(R.id.infoFeelsLike)
        infoPrecipitation = findViewById(R.id.infoPrecipitation)
        // labels
        labelTemp = findViewById(R.id.labelTemp);
        labelPressure = findViewById(R.id.labelPressure);
        labelHumidity = findViewById(R.id.labelHumidity);
        labelFeelsLike = findViewById(R.id.labelFeelsLike);
        labelPrecipitation = findViewById(R.id.labelPrecipitation);
        labelCity = findViewById(R.id.labelCity)
        labelDescription = findViewById(R.id.labelDescription)

        weatherIconView = findViewById(R.id.weatherIcon)

        showInfoCheckBox = findViewById(R.id.showDetailInfo)
        //

        textViews = arrayOf(
            labelPressure, labelHumidity, labelFeelsLike, labelPrecipitation,
            infoPressure, infoHumidity, infoFeelsLike, infoPrecipitation
        )
        changeVisibilityToTextViews(false)
        //

        // input to spinner countries name
        val spinner : Spinner = findViewById(R.id.selectCity)
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val itemSelectedListener: AdapterView.OnItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {

                    val item = parent.getItemAtPosition(position) as String

                    // load all information
                    val w = WeatherInfo()
                    GlobalScope.launch {
                        /* delay(1000)*/
                        val s = w.fetchDataAndParse("https://ua.sinoptik.ua/%D0%BF%D0%BE%D0%B3%D0%BE%D0%B4%D0%B0-" + item)
                        withContext(Dispatchers.Main) {
                            labelTemp.text = w.temp_true
                            labelPressure.text = w.pressure + "мм"
                            labelHumidity.text = w.humidity + "%"
                            labelFeelsLike.text = w.temp_feel
                            labelPrecipitation.text = w.opady
                            labelCity.text = item;
                            labelDescription.text = w.descriptionOfWeather

                            Executors.newSingleThreadExecutor().execute {
                                val stream = URL("https:" + w.weatherIcon).openStream()
                                var image: Bitmap? = BitmapFactory.decodeStream(stream)
                                Handler(Looper.getMainLooper()).post {
                                    weatherIconView.setImageBitmap(image)
                                }
                            }
                        }
                    }

                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        spinner.onItemSelectedListener = itemSelectedListener;


        showInfoCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showInfoCheckBox.text = getString(R.string.checkBoxTextHide)
                changeVisibilityToTextViews(true)
            } else {
                showInfoCheckBox.text = getString(R.string.checkBoxTextShow)
                changeVisibilityToTextViews(false)
            }
        }
    }

    // fun to set background to app
    private fun setAppBackground(backgroundType: Int) {
        currentBackground = when (backgroundType) {
            MORNING -> R.drawable.day_bg
            EVENING -> R.drawable.eve_bg
            NIGHT -> R.drawable.night_bg
            else -> R.drawable.day_bg
        }

        window.setBackgroundDrawableResource(currentBackground)
    }

    private fun changeVisibilityToTextViews(visibilityBool : Boolean) {
        if (visibilityBool){
            for (textView in textViews) {
                textView.visibility = View.VISIBLE
            }
        }
        else{
            for (textView in textViews) {
                textView.visibility = View.INVISIBLE
            }
        }
    }
}