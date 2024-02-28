package network

import WifiSession
import RoomPrediction
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/data-collection/")
    fun postWifiSession(@Header("Authorization") token: String, @Body wifiSession: WifiSession): Call<ResponseBody>

    // New endpoint for room prediction
    @POST("api/room-prediction/")
    fun postRoomPrediction(@Header("Authorization") token: String, @Body roomPrediction: RoomPrediction): Call<ResponseBody>

}
