package network

import WifiSession
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("wifi_sessions")
    fun postWifiSession(
        @Header("Authorization") authToken: String,
        @Body wifiSession: WifiSession
    ): Call<ResponseBody>
}

