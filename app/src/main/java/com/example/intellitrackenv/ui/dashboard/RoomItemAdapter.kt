// RoomItemAdapter.kt
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.intellitrackenv.R

class RoomItemAdapter(context: Context, rooms: List<RoomItem>) : ArrayAdapter<RoomItem>(context, 0, rooms) {

    private var mRooms: MutableList<RoomItem> = rooms.toMutableList()

    override fun getCount(): Int = mRooms.size

    override fun getItem(position: Int): RoomItem? = mRooms[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.find_list_item, parent, false)

        val roomItem = getItem(position)

        itemView.findViewById<TextView>(R.id.roomNumberValue).text = roomItem?.roomNumber
        itemView.findViewById<TextView>(R.id.wifiPredictionValue).text = roomItem?.wifiPrediction

        return itemView
    }

    fun replaceItems(items: List<RoomItem>) {
        mRooms.clear()
        mRooms.addAll(items)
        notifyDataSetChanged()
    }
}
