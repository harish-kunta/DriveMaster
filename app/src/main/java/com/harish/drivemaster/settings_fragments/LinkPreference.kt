package com.harish.drivemaster.settings_fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import com.harish.drivemaster.R

class LinkPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    init {
        // Set the layout of this preference to your custom layout if needed
        layoutResource = R.layout.preference_text_layout
    }

    override fun onClick() {
        super.onClick()
        val url = summary.toString()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
