package com.harish.drivemaster.helpers

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.preference.PreferenceManager
import com.harish.drivemaster.R

class SoundUtil private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun playSuccessSound() {
        if (isSoundEnabled()) {
            playSound(R.raw.success_sound)
        }
    }

    fun playFailureSound() {
        if (isSoundEnabled()) {
            playSound(R.raw.wrong_answer_sound)
        }
    }

    private fun playSound(soundResId: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release() // Release resources after playback
        }
        mediaPlayer.start()
    }

    private fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean("sound_effects", true)
    }

    companion object {
        private var instance: SoundUtil? = null

        fun getInstance(context: Context): SoundUtil {
            if (instance == null) {
                instance = SoundUtil(context.applicationContext)
            }
            return instance!!
        }
    }
}
