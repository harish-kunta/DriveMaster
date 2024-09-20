package com.harish.drivemaster.helpers

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebasePreferences(private val userId: String) {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("preferences")

    // Function to get a preference value based on the key
    fun getPreference(key: String, callback: (String?) -> Unit) {
        database.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(String::class.java)
                callback(value) // Return the value using the callback
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null) // Return null in case of an error
            }
        })
    }

    // Function to save a preference value
    fun setPreference(key: String, value: String, callback: (Boolean) -> Unit) {
        database.child(key).setValue(value)
            .addOnSuccessListener {
                callback(true) // Successfully saved
            }
            .addOnFailureListener {
                callback(false) // Failed to save
            }
    }
}
