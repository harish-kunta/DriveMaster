package com.harish.drivemaster.settings_fragments

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.harish.drivemaster.R

class CustomEditTextPreference(context: Context, attrs: AttributeSet?) :
    EditTextPreference(context, attrs) {

    private lateinit var editText: EditText
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        // Set the layout resource to your custom layout
        layoutResource = R.layout.custom_edit_text_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        // Find the EditText within your custom layout
        editText = holder.findViewById(R.id.custom_edit_text) as EditText

        // Set the current value to the EditText
        editText.setText(sharedPreferences?.getString(key, ""))

        // Set listener for the "Done" action on the keyboard
        editText.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Save the new value to SharedPreferences when "Done" is pressed
                val newValue = editText.text.toString()
                if (callChangeListener(newValue)) {
                    sharedPreferences?.edit()?.putString(key, newValue)?.apply()
                    summary = newValue  // Optionally update the summary

                    // Update Firebase based on the key
                    when (key) {
                        "change_name" -> updateFirebaseUsername(newValue)
                        "change_password" -> promptForOldPasswordAndReauthenticate(newValue)
                    }
                }
                editText.clearFocus()

                // Hide the keyboard
                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                true
            } else {
                false
            }
        })
    }

    override fun onAttached() {
        super.onAttached()
        // Ensure the summary is updated with the saved value
        summary = sharedPreferences?.getString(key, "")
    }

    private fun updateFirebaseUsername(newUsername: String) {
        val user = auth.currentUser
        user?.let {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build()

            it.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        // update the username in the Firebase database
                        val database = FirebaseDatabase.getInstance()
                        val userRef = database.getReference("users").child(user.uid)
                        userRef.child("name").setValue(newUsername)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Username updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to update username in database",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Failed to update username", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    private fun promptForOldPasswordAndReauthenticate(newPassword: String) {
        // Inflate the custom view for the dialog
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_old_password, null)
        val oldPasswordInput = dialogView.findViewById<EditText>(R.id.old_password_input)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Re-authenticate")
            .setMessage("Please enter your current password to re-authenticate.")
            .setView(dialogView) // Set the custom view
            .setPositiveButton("Re-authenticate") { dialogInterface, _ ->
                val oldPassword = oldPasswordInput.text.toString()
                if (oldPassword.isNotEmpty()) {
                    reauthenticateUser(oldPassword, newPassword)
                } else {
                    Toast.makeText(context, "Please enter your old password.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun reauthenticateUser(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        user?.let {
            val email = it.email ?: return
            val credential = EmailAuthProvider.getCredential(email, oldPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        updateFirebasePassword(newPassword)
                    } else {
                        Toast.makeText(context, "Re-authentication failed: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updateFirebasePassword(newPassword: String) {
        val user = auth.currentUser
        user?.let {
            it.updatePassword(newPassword)
                .addOnSuccessListener {
                    Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }
}
