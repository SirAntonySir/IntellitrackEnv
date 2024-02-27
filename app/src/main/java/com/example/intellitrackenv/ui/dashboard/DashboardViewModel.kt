// DashboardViewModel.kt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _rooms = MutableLiveData<List<RoomItem>>().apply {
        value = listOf(
            RoomItem("357", "90%"),
            RoomItem("358", "85%")
            // Initialize with default rooms or empty list
        )
    }

    val rooms: LiveData<List<RoomItem>> = _rooms

    // Function to update the list of rooms
    fun updateRooms(newRooms: List<RoomItem>) {
        _rooms.value = newRooms
    }

    // Function to add a single room
    fun addRoom(room: RoomItem) {
        val currentList = _rooms.value?.toMutableList() ?: mutableListOf()
        currentList.add(room)
        _rooms.value = currentList
    }

    fun removeRoom(room: RoomItem) {
        val currentList = _rooms.value?.toMutableList()
        currentList?.remove(room)
        _rooms.value = currentList
    }
}
