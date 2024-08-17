package com.harish.drivemaster
import android.app.Application
import com.google.firebase.FirebaseApp

class DriveMaster : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}