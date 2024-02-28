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
import com.example.intellitrackenv.R
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
        val imageView: SubsamplingScaleImageView = binding.imageScale // Assuming you have a SubsamplingScaleImageView with the ID ivLargeImage in your FragmentDashboardBinding
        imageView.setImage(ImageSource.resource(R.drawable.floor4))


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


    private fun sendSerializedData() {
        val apiService = createRetrofitService()

        // Assuming you have filled the WifiSession object with your data
        val myRoomPrediction = serializeWifiSession(); // Create and fill your WifiSession object here

        Log.d("REQUESTabc", myRoomPrediction.toString())
        apiService.postRoomPrediction("Token django-insecure-ootl(_y(mf@_mu34d8cw5h3l54vbrkcfcl1!de5_jrj=1a\$ehk", myRoomPrediction).enqueue(object :
            Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("PostSuccess", "Successfully posted session data: ${response.body()?.string()}")

                    // Clear accumulated Wifi scan results after successful post
                    accumulatedWifiLists.clear()
                } else {
                    Log.e("PostError", "Failed to post session data: ${response.errorBody()?.string()}")
                    accumulatedWifiLists.clear()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("NetworkError", "Network call failed", t)
            }
        })
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
            // Start WiFi scan
        scanWifiNetworks()
        }

        // Simulate scanning process
        val simulatedScanResult = listOf(RoomItem("459", "95%"))

        // Update the UI
        (binding.roomsListView.adapter as RoomItemAdapter).replaceItems(simulatedScanResult)


    }

    private fun scanWifiNetworks() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        Toast.makeText(requireContext(), "performed Scan", Toast.LENGTH_SHORT).show()

        val currentTimeMillis = System.currentTimeMillis()

        wifiManager.startScan()
        val currentScanResults = wifiManager.scanResults
        accumulatedWifiLists.add(Pair(currentTimeMillis, currentScanResults))

        // Convert the results to your RoomItem format and update UI
        val roomItems = currentScanResults.map { scanResult ->
            RoomItem(scanResult.BSSID, "${scanResult.level}%")
        }

        // Update the UI
        (binding.roomsListView.adapter as RoomItemAdapter).replaceItems(roomItems)

        // Send the results to the backend
        sendSerializedData()// Store scan results with timestamp
    }

    private fun processScanResults() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
