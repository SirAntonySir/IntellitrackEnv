package com.example.intellitrackenv.ui.home

import CollectedSignal
import ScanItem
import ScanItemAdapter
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
        listView = binding.scansListView
        fingerPrintSessionLabel = binding.sessionLabelInput
        scanButton = binding.btnScanWifi

        scanButton.setOnClickListener {
            handleScanButtonClick()
        }

        // Initialize your ViewModel, adapter, set up listeners
        val dashboardViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val adapter = ScanItemAdapter(requireContext(), mutableListOf())
        binding.scansListView.adapter = adapter

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
                scanButton.text = "Please enter the Session ID"

                handler.postDelayed({
                    scanButton.text = "Start Scanning" // Replace with your original button text
                }, 2000)
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
        binding.countdownText.visibility = View.VISIBLE
        Log.d("HomeFragment", "Scanning started")
    }

    private fun stopScanning() {
        Log.d("HomeFragment", "Scanning stopped")
        isScanning = false
        fingerPrintSessionLabel.isEnabled = true
        scanButton.text = "Start Scanning"
        scanEndTime = System.currentTimeMillis()
        sendSerializedData()
        handler.removeCallbacks(scanRunnable)
        handler.removeCallbacks(countdownRunnable)
        binding.countdownText.visibility = View.GONE
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

        // Change the button text
        scanButton.text = "Scanning..."

        val currentTimeMillis = System.currentTimeMillis()

        wifiManager.startScan()
        val currentScanResults = wifiManager.scanResults
        Log.d("HomeFragment", "Scan results size: ${currentScanResults.size}")
        accumulatedWifiLists.add(Pair(currentTimeMillis, currentScanResults)) // Store scan results with timestamp
        updateUIWithLatestScan(currentScanResults)

        // Revert the button text back to the original after 2 seconds
        handler.postDelayed({
            scanButton.text = "Stop Scanning" // Replace with your original button text
        }, 2000) // 2000 milliseconds delay
    }



    private fun updateUIWithLatestScan(scanResults: List<ScanResult>) {
        val scanItems = scanResults.map { scanResult ->
            ScanItem(scanResult.BSSID, scanResult.SSID, scanResult.level.toString())
        }

        val adapter = binding.scansListView.adapter as? ScanItemAdapter
        if (adapter == null) {
            binding.scansListView.adapter = ScanItemAdapter(requireContext(), scanItems.toMutableList())
        } else {
            adapter.updateItems(scanItems)
        }
    }

    private val _scans = MutableLiveData<List<ScanItem>>().apply {
    }

    val rooms: LiveData<List<ScanItem>> = _scans

    // Function to update the list of rooms
    fun updateRooms(newRooms: List<ScanItem>) {
        _scans.value = newRooms
    }

    // Function to add a single room
    fun addRoom(room: ScanItem) {
        val currentList = _scans.value?.toMutableList() ?: mutableListOf()
        currentList.add(room)
        _scans.value = currentList
    }

    fun removeRoom(room: ScanItem) {
        val currentList = _scans.value?.toMutableList()
        currentList?.remove(room)
        _scans.value = currentList
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
                    frequency = scanResult.frequency
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