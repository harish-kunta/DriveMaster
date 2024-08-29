package com.harish.drivemaster.helpers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.MainActivity
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        Log.d("MyFirebaseMessagingService", "Refreshed token: $token")
        saveFcmToken(token)  // Save the refreshed token to the database
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", "streak_reminder")
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, "streak_channel")
            .setSmallIcon(R.drawable.drive_master_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun saveFcmToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        val tokenRef = database.getReference(USERS_REF).child(userId).child("fcmToken")

        tokenRef.setValue(token)
            .addOnSuccessListener {
                Log.d("MainActivity", "FCM token saved successfully.")
            }
            .addOnFailureListener { exception ->
                Log.d("MainActivity", "Failed to save FCM token: ${exception.message}")
            }
    }
}
