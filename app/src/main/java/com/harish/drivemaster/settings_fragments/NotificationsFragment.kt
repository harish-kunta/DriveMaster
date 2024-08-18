package com.harish.drivemaster.settings_fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.harish.drivemaster.R

class NotificationsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Define notifications related settings here
        setPreferencesFromResource(R.xml.notifications_preferences, rootKey)
    }
}
