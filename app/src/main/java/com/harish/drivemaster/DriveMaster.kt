package com.harish.drivemaster
import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class DriveMaster : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(10 * 1024 * 1024) // 10MB
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}