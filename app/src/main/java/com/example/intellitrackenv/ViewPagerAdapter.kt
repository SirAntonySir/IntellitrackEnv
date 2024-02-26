package com.example.intellitrackenv

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.intellitrackenv.ui.dashboard.DashboardFragment
import com.example.intellitrackenv.ui.home.HomeFragment
import com.example.intellitrackenv.ui.notifications.NotificationsFragment

class ViewPagerAdapter(activity: AppCompatActivity, val itemsCount: Int) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment {
        // You can either return a new instance of a fragment
        // based on the position or use a when statement to map positions to fragment classes.
        return when (position) {
            0 -> HomeFragment()
            1 -> DashboardFragment()
            2 -> NotificationsFragment()
            else -> HomeFragment()
        }
    }

    override fun getItemCount(): Int {
        return itemsCount
    }
}
