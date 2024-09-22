package com.harish.drivemaster

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.harish.drivemaster.settings_fragments.LinkPreference

class PreferencesFragment : PreferenceFragmentCompat() {

    private lateinit var databaseReference: DatabaseReference
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference.child("appSettings")

        fetchUrlsFromFirebase()
    }

    private fun fetchUrlsFromFirebase() {
        databaseReference.get().addOnSuccessListener { dataSnapshot ->
            val termsOfServiceUrl = dataSnapshot.child("termsOfServiceUrl").getValue(String::class.java)
            val privacyPolicyUrl = dataSnapshot.child("privacyPolicyUrl").getValue(String::class.java)

            findPreference<LinkPreference>("terms_of_service")?.summary = termsOfServiceUrl
            findPreference<LinkPreference>("privacy_policy")?.summary = privacyPolicyUrl
        }.addOnFailureListener { exception ->
            // Handle errors
        }
    }
}
