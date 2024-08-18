package com.harish.drivemaster.settings_fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.harish.drivemaster.R

class ProfileFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Define profile related settings here
        setPreferencesFromResource(R.xml.profile_preferences, rootKey)
    }
}
