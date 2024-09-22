package com.harish.drivemaster.settings_fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.EntryActivity
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF

class ButtonPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private val auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var database: DatabaseReference
    private val appContext: Context = context.applicationContext

    init {
        layoutResource = R.layout.sign_out_button_layout
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference(USERS_REF)
        configureGoogleSignIn()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // Access the button and set a click listener
        val button: Button = holder.itemView.findViewById(R.id.signOutButton)

        if(key == "sign_out_button")
            button.setText("SIGN OUT")
        else if (key == "delete_account")
            button.setText("DELETE ACCOUNT")

        button.setOnClickListener {
            // Handle button click
            if(key == "sign_out_button")
                onButtonClick()
            else if (key == "delete_account")
                showDeleteAccountConfirmationDialog()
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                deleteUserAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser ?: return

        // Remove user data from Firebase Realtime Database
        val userId = user.uid
        val userRef = database.child(userId)

        userRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Delete the user account from Firebase Authentication
                user.delete().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        Toast.makeText(appContext, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        // Optionally, redirect the user to a login or welcome screen
                        val signInIntent = Intent(context, EntryActivity::class.java)
                        context.startActivity(signInIntent)

                    } else {
                        Log.e("UserProfile", "Error deleting user account", deleteTask.exception)
                        Toast.makeText(appContext, "Failed to delete account", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.e("UserProfile", "Error removing user data", task.exception)
                Toast.makeText(appContext, "Failed to remove user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onButtonClick() {
        //sign out and redirect to sign in activity
        googleSignInClient.signOut()
        auth.signOut().also {
            val signInIntent = Intent(context, EntryActivity::class.java)
            context.startActivity(signInIntent)
        }
    }
}
