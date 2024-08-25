package com.harish.drivemaster.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.harish.drivemaster.main_fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

}