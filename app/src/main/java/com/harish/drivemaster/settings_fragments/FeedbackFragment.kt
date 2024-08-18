package com.harish.drivemaster.settings_fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.harish.drivemaster.R

class FeedbackFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Define feedback related settings here
        setPreferencesFromResource(R.xml.feedback_preferences, rootKey)
    }
}
