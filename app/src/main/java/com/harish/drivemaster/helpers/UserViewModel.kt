package com.harish.drivemaster.helpers

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.models.FirebaseConstants.Companion.COMPLETED_LEVELS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.CURRENT_STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.EMAIL
import com.harish.drivemaster.models.FirebaseConstants.Companion.HEARTS_LEFT_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.HEARTS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.LAST_REGEN_TIME_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.NAME
import com.harish.drivemaster.models.FirebaseConstants.Companion.POINTS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.PROFILE_IMAGE_URL
import com.harish.drivemaster.models.FirebaseConstants.Companion.STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userDatabase: DatabaseReference = FirebaseDatabase.getInstance().reference.child(USERS_REF)

    private val _streak = MutableLiveData<Int>()
    private val _heartsLeft = MutableLiveData<Int>()
    private val _lastRegenTime = MutableLiveData<Long>()
    private val _completedLevels = MutableLiveData<Set<Int>>()
    private val _profileImageUrl = MutableLiveData<String>()
    private val _userName = MutableLiveData<String>()
    private val _userEmail = MutableLiveData<String>()
    private val _xp = MutableLiveData<Int>()

    val streak: LiveData<Int> = _streak
    val heartsLeft: LiveData<Int> = _heartsLeft
    val lastRegenTime: LiveData<Long> = _lastRegenTime
    val completedLevels: LiveData<Set<Int>> = _completedLevels
    val profileImageUrl: LiveData<String> = _profileImageUrl
    val userName: LiveData<String> = _userName
    val userEmail: LiveData<String> = _userEmail
    val xp: LiveData<Int> = _xp

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = userDatabase.child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _userName.value = snapshot.getValue(NAME, "User Name")
                _userEmail.value = snapshot.getValue(EMAIL, "user@example.com")
                _profileImageUrl.value = snapshot.getValue(PROFILE_IMAGE_URL)
                _streak.value = snapshot.getNestedValue(STREAK_REF, CURRENT_STREAK_REF, 0)
                _xp.value = snapshot.getValue(POINTS_REF, 0)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserViewModel", "Failed to load user data", error.toException())
            }
        })

        listenForChanges(userRef.child(STREAK_REF).child(CURRENT_STREAK_REF), _streak, 0)
        listenForChanges(userRef.child(HEARTS_REF).child(HEARTS_LEFT_REF), _heartsLeft, 0)
        listenForChanges(userRef.child(HEARTS_REF).child(LAST_REGEN_TIME_REF), _lastRegenTime, System.currentTimeMillis())
        listenForCompletedLevels(userRef.child(COMPLETED_LEVELS_REF))
    }

    private fun listenForChanges(ref: DatabaseReference, liveData: MutableLiveData<Int>, defaultValue: Int) {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                liveData.value = dataSnapshot.getValue(Int::class.java) ?: defaultValue
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("UserViewModel", "Failed to load data", databaseError.toException())
            }
        })
    }

    private fun listenForChanges(ref: DatabaseReference, liveData: MutableLiveData<Long>, defaultValue: Long) {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                liveData.value = dataSnapshot.getValue(Long::class.java) ?: defaultValue
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("UserViewModel", "Failed to load data", databaseError.toException())
            }
        })
    }

    private fun listenForCompletedLevels(ref: DatabaseReference) {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _completedLevels.value = snapshot.children.mapNotNull { it.key?.toInt() }.toSet()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserViewModel", "Failed to load completed levels", error.toException())
            }
        })
    }

    fun saveHeartsData(heartsLeft: Int, lastRegenTime: Long) {
        val userId = auth.currentUser?.uid ?: return
        val heartsData = mapOf(
            HEARTS_LEFT_REF to heartsLeft,
            LAST_REGEN_TIME_REF to lastRegenTime
        )

        userDatabase.child(userId).child(HEARTS_REF).setValue(heartsData)
            .addOnSuccessListener {
                Log.d("UserViewModel", "Hearts data saved successfully.")
            }
            .addOnFailureListener { exception ->
                Log.e("UserViewModel", "Failed to save hearts data", exception)
            }
    }

    fun updateProfileImage(newImageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        userDatabase.child(userId).child(PROFILE_IMAGE_URL).setValue(newImageUrl)
            .addOnSuccessListener {
                _profileImageUrl.value = newImageUrl
            }
            .addOnFailureListener {
                Log.e("UserViewModel", "Failed to update profile image URL", it)
            }
    }

    private inline fun <reified T> DataSnapshot.getValue(key: String, defaultValue: T? = null): T? {
        return this.child(key).getValue(T::class.java) ?: defaultValue
    }

    private inline fun <reified T> DataSnapshot.getNestedValue(parentKey: String, childKey: String, defaultValue: T): T {
        return this.child(parentKey).child(childKey).getValue(T::class.java) ?: defaultValue
    }
}


