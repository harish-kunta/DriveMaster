package com.harish.drivemaster.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.harish.drivemaster.R

class NotificationsActivity : AppCompatActivity() {
    private lateinit var btnAllowNotifications: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        btnAllowNotifications = findViewById(R.id.btnAllowNotifications)
        btnAllowNotifications.setOnClickListener {
            showNotificationPermissionDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showNotificationPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Allow Notifications")
        builder.setMessage("Would you like to enable notifications for this app?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            requestNotificationPermission()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
            // Optionally, show a message explaining the decision
            Toast.makeText(
                this,
                "Notifications are turned off. You can enable them in settings anytime.",
                Toast.LENGTH_LONG
            ).show()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "drive_master_notifications"
            val channelName = "Drive Master Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Additional logic to request notification permission on devices running Android 13 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Notifications enabled.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Notifications are disabled. You can enable them in settings.",
                    Toast.LENGTH_LONG
                ).show()
                // Optionally, guide the user to the app's notification settings
                val intent = Intent()
                intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                intent.putExtra("app_package", packageName)
                intent.putExtra("app_uid", applicationInfo.uid)
                startActivity(intent)
            }
        }
    }
}