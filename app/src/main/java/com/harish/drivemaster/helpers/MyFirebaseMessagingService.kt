package com.harish.drivemaster.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
        super.onMessageReceived(remoteMessage)

        // Log the received message
        Log.d("MyFirebaseMsgService", "From: ${remoteMessage.from}")
        Log.d("MyFirebaseMsgService", "Notification Message Body: ${remoteMessage.notification?.body}")

        // Create a notification channel if necessary (for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel_id",
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel description"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, "default_channel_id")
            .setSmallIcon(R.drawable.drive_master_logo)
            .setContentTitle(remoteMessage.notification?.title ?: "No Title")
            .setContentText(remoteMessage.notification?.body ?: "No Body")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
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
