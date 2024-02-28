//ScanItemAdapter.kt
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.intellitrackenv.R

class ScanItemAdapter(context: Context, scans: List<ScanItem>) : ArrayAdapter<ScanItem>(context, 0, scans) {

private var mScans: MutableList<ScanItem> = scans.toMutableList()

        override fun getCount(): Int = mScans.size

        override fun getItem(position: Int): ScanItem? = mScans[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.scan_list_item, parent, false)

        val scanItem = getItem(position)

        itemView.findViewById<TextView>(R.id.bssidTextView).text = scanItem?.scanBSSID
        itemView.findViewById<TextView>(R.id.signalStrengthTextView).text = scanItem?.scanValue

        return itemView
        }

        fun replaceItems(items: List<ScanItem>) {
        mScans.clear()
        mScans.addAll(items)
        notifyDataSetChanged()
        }
        }
