package com.example.intellitrackenv.ui.home

import CollectedSignal
import WifiSession
import WifiSignal
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.intellitrackenv.databinding.FragmentHomeBinding
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import network.ApiService
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class HomeFragment : Fragment() {
    private val binding get() = _binding!!

    private var _binding: FragmentHomeBinding? = null

    private lateinit var wifiManager: WifiManager
    private lateinit var listView: ListView
    private lateinit var fingerPrintSessionLabel: EditText
    private lateinit var scanButton: Button

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val scanInterval: Long = 30_000
    private var countdownSeconds: Int = 30
    private var scanStartTime: Long = 0
    private var scanEndTime: Long = 0
    private val accumulatedWifiLists: MutableList<Pair<Long, List<ScanResult>>> = mutableListOf()


    private val LOCATION_PERMISSION_REQUEST_CODE = 100



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        listView = binding.lvWifiNetworks
        fingerPrintSessionLabel = binding.sessionLabelInput
        scanButton = binding.btnScanWifi

        scanButton.setOnClickListener {
            handleScanButtonClick()
        }

        // Initialize other elements...
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            // Update UI with the remaining time

            val countdownText: TextView = binding.countdownText
            countdownText.text = "Next scan in $countdownSeconds seconds"

            if (countdownSeconds > 0) {
                countdownSeconds--
                // Post itself again after 1 second to update the countdown
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                scanWifiNetworks()
                // Reset the countdown
                countdownSeconds = 30 // Reset to your scanInterval duration in seconds
                // Start or restart the countdown
                handler.removeCallbacks(countdownRunnable) // Remove any existing callbacks to avoid duplication
                handler.post(countdownRunnable) // Start the countdown
                // Schedule the next scan
                handler.postDelayed(this, scanInterval)
            }
        }
    }

    private fun handleScanButtonClick() {
        val fingerPrintSessionIDText = fingerPrintSessionLabel.text.toString()
        if (!isScanning) {
            if (fingerPrintSessionIDText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the session ID.", Toast.LENGTH_SHORT).show()
            }
            else {
                startScanning()
            }
        } else {
            stopScanning()
        }
    }

    private fun startScanning() {
        isScanning = true
        fingerPrintSessionLabel.isEnabled = false
        scanButton.text = "Stop Scanning"
        accumulatedWifiLists.clear() // Clear previous scans
        scanStartTime = System.currentTimeMillis()
        checkAndRequestPermissions()
        handler.post(scanRunnable)
    }

    private fun stopScanning() {
        isScanning = false
        fingerPrintSessionLabel.isEnabled = true
        scanButton.text = "Start Scanning"
        scanEndTime = System.currentTimeMillis()
        sendSerializedData()
        handler.removeCallbacks(scanRunnable)
    }

    private fun createRetrofitService(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder().addInterceptor(logging).build()
        /*        val httpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val requestBuilder = original.newBuilder()
                            .header("Authorization", "Token django-insecure-ootl(_y(mf@_mu34d8cw5h3l54vbrkcfcl1!de5_jrj=1a\$ehk") // Add your token here
                        val request = requestBuilder.build()
                        chain.proceed(request)
                    }
                    .build()*/

        val retrofit = Retrofit.Builder()
            .baseUrl("https://pempiis.medien.ifi.lmu.de/pempiis01/") // Replace with your actual backend base URL
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun sendSerializedData() {
        val apiService = createRetrofitService()

        // Assuming you have filled the WifiSession object with your data
        val wifiSession = serializeWifiSession(); // Create and fill your WifiSession object here

        Log.d("REQUEST", wifiSession.toString())
        apiService.postWifiSession("Token django-insecure-ootl(_y(mf@_mu34d8cw5h3l54vbrkcfcl1!de5_jrj=1a\$ehk", wifiSession).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("PostSuccess", "Successfully posted session data")
                } else {
                    Log.e("PostError", "Failed to post session data: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("NetworkError", "Network call failed", t)
            }
        })
    }


    private fun checkAndRequestPermissions() {
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
        Toast.makeText(requireContext(), "Scanning WiFi networks...", Toast.LENGTH_SHORT).show()
        // Rest of your scanning logic...
    }


    private fun updateUIWithLatestScan(scanResults: List<ScanResult>) {
        val formattedScanResults = scanResults.map { scanResult ->
            val distance = calculateDistance(scanResult.level, scanResult.frequency)
            "📶 SSID: ${scanResult.SSID} \n" +
                    "🔑 BSSID: ${scanResult.BSSID} \n" +
                    "📡 Signal Strength: ${scanResult.level} dBm\n" +
                    "🔊 Channel Bandwidth: ${channelWidthToString(scanResult.channelWidth)} \n" +
                    "🌐 Frequency: ${scanResult.frequency} MHz \n"
        }

        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, formattedScanResults)
        listView.adapter = adapter
    }

    private fun calculateDistance(level: Int, frequency: Int): Double {
        // Placeholder for your distance calculation logic
        return 0.0 // Replace with actual distance calculation
    }

    private fun channelWidthToString(channelWidth: Int): String {
        return when (channelWidth) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
            ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
            ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
            ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
            else -> "Unknown"
        }
    }

    private fun serializeWifiSession(): WifiSession {
        val sessionLabel = fingerPrintSessionLabel.text.toString()
        val deviceID = Settings.Secure.getString(requireActivity().contentResolver, Settings.Secure.ANDROID_ID) ?: "none"
        val androidVersion = android.os.Build.VERSION.RELEASE ?: "none"

        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Berlin")
        }
        val startTimeFormatted = format.format(Date(scanStartTime))
        val endTimeFormatted = format.format(Date(scanEndTime))
        val collectedSignals: List<CollectedSignal> = accumulatedWifiLists.map { (scanTimestamp, wifiList) ->
            val scanTimeFormatted = format.format(Date(scanTimestamp))
            val wifiSignals = wifiList.map { scanResult ->
                WifiSignal(
                    SSID = scanResult.SSID.takeIf { it?.isNotEmpty() == true } ?: "NO_SSID",
                    BSSID = scanResult.BSSID,
                    level = scanResult.level,
                    frequency = scanResult.frequency,
                    channelWidth = scanResult.channelWidth,
                    centerFreq0 = scanResult.centerFreq0,
                    centerFreq1 = scanResult.centerFreq1
                )
            }
            CollectedSignal(scanTimeFormatted, wifiSignals)
        }

        val session = WifiSession(
            phone_id = deviceID ?: "none",
            android_version = android.os.Build.VERSION.RELEASE ?: "none",
            start_time = startTimeFormatted ?: "none",
            end_time = endTimeFormatted ?: "none",
            session_label = sessionLabel,
            collected_signals = collectedSignals ?: emptyList()
        )


        return session
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Handle permission result...
    }

    override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }