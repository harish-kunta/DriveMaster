package com.harish.drivemaster.settings_fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.harish.drivemaster.R

class HelpCenterFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Define Help Center related settings here
        setPreferencesFromResource(R.xml.help_center_preferences, rootKey)
    }
}
