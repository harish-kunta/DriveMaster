package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.harish.drivemaster.R
import com.harish.drivemaster.helpers.HapticFeedbackUtil

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // User is not signed in, navigate to the sign-in activity
            startActivity(Intent(this, EntryActivity::class.java))
            finish()
        }

        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Set up haptic feedback on navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            // if item already selected then don't navigate
            if (item.itemId == bottomNav.selectedItemId) {
                return@setOnItemSelectedListener true
            }
            // Perform haptic feedback
            HapticFeedbackUtil.performHapticFeedback(this)
            navController.navigate(item.itemId)
            true
        }
    }
}
