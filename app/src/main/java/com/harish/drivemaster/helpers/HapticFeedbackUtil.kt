package com.harish.drivemaster.helpers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.preference.PreferenceManager

object HapticFeedbackUtil {
    fun performHapticFeedback(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isHapticEnabled = sharedPreferences.getBoolean("haptic_feedback", true)

        if (isHapticEnabled) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        }
    }
}

