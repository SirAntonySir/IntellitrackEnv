//DashboardFragment.kt
package com.example.intellitrackenv.ui.dashboard

import CollectedSignal
import DashboardViewModel
import RoomItem
import RoomItemAdapter
import RoomPrediction
import WifiSession
import WifiSignal
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.intellitrackenv.databinding.FragmentDashboardBinding
import android.webkit.WebView
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import android.Manifest
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.example.intellitrackenv.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val accumulatedWifiLists: MutableList<Pair<Long, List<ScanResult>>> = mutableListOf()
    private lateinit var wifiManager: WifiManager
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the Subsampling Scale Image View
        val imageView: SubsamplingScaleImageView = binding.imageScale
        imageView.setImage(ImageSource.resource(R.drawable.floor3))
        highlightButton(binding.buttonFloor3)


        // Initialize your ViewModel, adapter, set up listeners
        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        val adapter = RoomItemAdapter(requireContext(), emptyList())
        binding.roomsListView.adapter = adapter

        binding.scanWifiButton.setOnClickListener {
            performRoomScan()
        }

        dashboardViewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            (binding.roomsListView.adapter as RoomItemAdapter).replaceItems(rooms)
        }

        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        return root
    }

    private fun serializeWifiSession(): RoomPrediction {
        val deviceID = Settings.Secure.getString(requireActivity().contentResolver, Settings.Secure.ANDROID_ID) ?: "none"
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Berlin")
        }

        // You need to flatten the list of pairs into a single list of WifiSignals
        val wifiSignals = accumulatedWifiLists.flatMap { (scanTimestamp, wifiList) ->
            wifiList.map { scanResult ->
                WifiSignal(
                    SSID = scanResult.SSID,
                    BSSID = scanResult.BSSID,
                    level = scanResult.level,
                    frequency = scanResult.frequency
                )
            }
        }

        val prediction = RoomPrediction(
            phone_id = deviceID ?: "none",
            android_version = android.os.Build.VERSION.RELEASE ?: "none",
            wifi_signals = wifiSignals // No need for the elvis operator here as flatMap will always return a list, empty if there were no scan results.
        )

        return prediction
    }


    private suspend fun sendSerializedData(): String = withContext(Dispatchers.IO) {
        val apiService = createRetrofitService()

        // Create and fill your WifiSession object here
        val myRoomPrediction = serializeWifiSession()

        try {
            val response = apiService.postRoomPrediction("Token django-insecure-ootl(_y(mf@_mu34d8cw5h3l54vbrkcfcl1!de5_jrj=1a\$ehk", myRoomPrediction).execute()

            if (response.isSuccessful) {
                // Clear accumulated Wifi scan results after successful post
                accumulatedWifiLists.clear()
                // Assuming the response body contains a string prediction
                response.body()?.string() ?: "Error: Empty response"
            } else {
                "Error: ${response.errorBody()?.string()}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }


    private fun createRetrofitService(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val httpClient = OkHttpClient.Builder().addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://pempiis.medien.ifi.lmu.de/pempiis01/") // Replace with your actual backend base URL
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        return retrofit.create(ApiService::class.java)
    }

    // Simulated scan function
    private fun performRoomScan() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            scanWifiNetworks()
        }
    }

    private fun scanWifiNetworks() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Start WiFi scan
        wifiManager.startScan()
        val currentScanResults = wifiManager.scanResults
        val currentTimeMillis = System.currentTimeMillis()
        accumulatedWifiLists.add(Pair(currentTimeMillis, currentScanResults))

        // Use coroutine to handle asynchronous task
        lifecycleScope.launch {
            val predictionResponseJson = sendSerializedData() // This should return a JSON string

            Log.d("PREDICTION", predictionResponseJson)

            // Use Gson to parse the JSON string into a Map
            val gson = Gson()
            val type = object : TypeToken<Map<String, Double>>() {}.type
            val predictionResponse: Map<String, Double> = gson.fromJson(predictionResponseJson, type)

            // Convert the Map into a list of RoomItem objects and sort them by descending prediction scores
            val simulatedScanResult = predictionResponse.map { entry ->
                RoomItem("Room ${entry.key}", "${(entry.value * 100).toInt()}%")
            }.sortedByDescending { item ->
                // Correctly extracting the numerical value for sorting
                item.wifiPrediction.trimEnd('%').toInt()
            }

            // Ensure UI updates are performed on the main thread
            withContext(Dispatchers.Main) {
                // Update the UI with the new simulatedScanResult
                (binding.roomsListView.adapter as RoomItemAdapter).replaceItems(simulatedScanResult)
            }
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFloor3.setOnClickListener {
            binding.imageScale.setImage(ImageSource.resource(R.drawable.floor3))
            highlightButton(it as Button)
        }

        binding.buttonFloor4.setOnClickListener {
            binding.imageScale.setImage(ImageSource.resource(R.drawable.floor4))
            highlightButton(it as Button)
        }
    }

    private fun highlightButton(activeButton: Button) {
        // Reset styles for all floor buttons to default
        binding.buttonFloor3.setBackgroundResource(R.drawable.default_button_background)
        binding.buttonFloor4.setBackgroundResource(R.drawable.default_button_background)

        // Highlight the active button
        activeButton.setBackgroundResource(R.drawable.highlighted_button_background)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
