//DashboardFragment.kt
package com.example.intellitrackenv.ui.dashboard

import DashboardViewModel
import RoomItem
import RoomItemAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.intellitrackenv.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val adapter = RoomItemAdapter(requireContext(), emptyList())
        binding.roomsListView.adapter = adapter

        binding.scanWifiButton.setOnClickListener {
            performRoomScan()
        }

        // Observe the LiveData from the ViewModel
        dashboardViewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            (binding.roomsListView.adapter as RoomItemAdapter).replaceItems(rooms)
        }

        return root
    }

    // Simulated scan function
    private fun performRoomScan() {
        // Simulate scanning process
        val simulatedScanResult = listOf(RoomItem("459", "95%"))

        // Update the UI
        (binding.roomsListView.adapter as RoomItemAdapter).replaceItems(simulatedScanResult)

        // Optionally, update the ViewModel if you need to reflect this change across other parts of the app
        // dashboardViewModel.updateRooms(simulatedScanResult)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
