package com.harish.drivemaster

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
