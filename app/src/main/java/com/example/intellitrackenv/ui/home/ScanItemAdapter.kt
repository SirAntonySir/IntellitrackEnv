//ScanItemAdapter.kt
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.intellitrackenv.R

class ScanItemAdapter(context: Context, scanItems: MutableList<ScanItem>) : ArrayAdapter<ScanItem>(context, 0, scanItems) {

        private var items: MutableList<ScanItem> = scanItems

        fun updateItems(newItems: List<ScanItem>) {
                items.clear()
                items.addAll(newItems)
                notifyDataSetChanged() // Notify the adapter to refresh the list view
        }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): ScanItem? = items.getOrNull(position)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.scan_list_item, parent, false)

        val scanItem = getItem(position)

        itemView.findViewById<TextView>(R.id.bssidTextView).text = scanItem?.scanBSSID
                itemView.findViewById<TextView>(R.id.ssidTextView).text = scanItem?.scanSsid
        itemView.findViewById<TextView>(R.id.signalStrengthTextView).text = scanItem?.scanValue

        return itemView
        }

        /*fun replaceItems(items: List<ScanItem>) {
        mScans.clear()
        mScans.addAll(items)
        notifyDataSetChanged()
        }*/
        }
