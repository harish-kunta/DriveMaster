package com.harish.drivemaster.settings_fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference
import android.widget.Button
import androidx.preference.PreferenceViewHolder
import com.google.firebase.auth.FirebaseAuth
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.EntryActivity

class ButtonPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private val auth: FirebaseAuth

    init {
        layoutResource = R.layout.sign_out_button_layout
        auth = FirebaseAuth.getInstance()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // Access the button and set a click listener
        val button: Button = holder.itemView.findViewById(R.id.signOutButton)
        button.setOnClickListener {
            // Handle button click
            onButtonClick()
        }
    }

    private fun onButtonClick() {
        //sign out and redirect to sign in activity
        auth.signOut().also {
            val signInIntent = Intent(context, EntryActivity::class.java)
            context.startActivity(signInIntent)
        }
    }
}
