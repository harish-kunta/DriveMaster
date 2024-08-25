package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.harish.drivemaster.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Custom action when clicking on a PreferenceScreen
        val preferenceScreen = findPreference<Preference>("preference_screen")
        preferenceScreen?.setOnPreferenceClickListener {
            // Navigate to TogglePreferencesActivity
            startActivity(Intent(context, TogglePreferencesActivity::class.java))
            true
        }
    }
}
