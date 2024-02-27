import kotlinx.serialization.Serializable

@Serializable
data class WifiSession(
    //val id: Int,
    val phone_id: String,
    val android_version: String,
    val start_time: String,
    val end_time: String,
    val session_label: String,
    val collected_signals: List<CollectedSignal>
)

@Serializable
data class CollectedSignal(
    val timestamp: String,
    val wifi_signals: List<WifiSignal>
)

@Serializable
data class WifiSignal(
    val SSID: String,
    val BSSID: String,
    val level: Int,
    val frequency: Int,
    val channelWidth: Int,
    val centerFreq0: Int,
    val centerFreq1: Int,
    val standard: String? = null,
    val interface_name: String? = null
)