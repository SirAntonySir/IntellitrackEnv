package network

import WifiSession
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/data-collection/")
    fun postWifiSession(@Header("Authorization") token: String, @Body wifiSession: WifiSession): Call<ResponseBody>

}
