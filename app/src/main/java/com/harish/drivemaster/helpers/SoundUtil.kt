package com.harish.drivemaster.helpers

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.preference.PreferenceManager
import com.harish.drivemaster.R

class SoundUtil private constructor(context: Context) {
    private val context: Context = context.applicationContext
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val mediaPlayer = MediaPlayer()

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
        mediaPlayer.reset() // Reset the media player to reuse it
        val assetFileDescriptor = context.resources.openRawResourceFd(soundResId)
        mediaPlayer.setDataSource(
            assetFileDescriptor.fileDescriptor,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.length
        )
        assetFileDescriptor.close()

        mediaPlayer.setOnCompletionListener { mp ->
            mp.reset() // Reset after playback to prepare for the next sound
        }
        mediaPlayer.prepare()
        mediaPlayer.start()
    }

    private fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean("sound_effects", true)
    }

    companion object {
        @Volatile
        private var instance: SoundUtil? = null

        fun getInstance(context: Context): SoundUtil {
            return instance ?: synchronized(this) {
                instance ?: SoundUtil(context).also { instance = it }
            }
        }
    }
}

