package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.harish.drivemaster.R
import com.harish.drivemaster.settings_fragments.PreferencesActivity
import com.harish.drivemaster.settings_fragments.ProfilePreferencesActivity

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val preferenceScreen = findPreference<Preference>("preference_screen")
        preferenceScreen?.setOnPreferenceClickListener {
            // Navigate to TogglePreferencesActivity
            startActivity(Intent(context, PreferencesActivity::class.java))
            true
        }

        val profileScreen = findPreference<Preference>("profile_screen")
        profileScreen?.setOnPreferenceClickListener {
            // Navigate to TogglePreferencesActivity
            startActivity(Intent(context, ProfilePreferencesActivity::class.java))
            true
        }
    }
}
