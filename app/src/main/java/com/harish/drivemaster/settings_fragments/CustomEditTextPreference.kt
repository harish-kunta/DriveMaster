package com.harish.drivemaster.settings_fragments

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import com.harish.drivemaster.R

class CustomEditTextPreference(context: Context, attrs: AttributeSet?) : EditTextPreference(context, attrs) {

    private lateinit var editText: EditText

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
                }
                editText.clearFocus()

                // Hide the keyboard
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
}
