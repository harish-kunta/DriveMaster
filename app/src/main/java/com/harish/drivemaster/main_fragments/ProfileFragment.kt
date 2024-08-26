package com.harish.drivemaster.main_fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.SettingsActivity
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF
import de.hdodenhof.circleimageview.CircleImageView

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // UI Components
    private lateinit var settingsIcon: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var streakValue: TextView
    private lateinit var xpValue: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var editProfileImageButton: ImageButton
    private lateinit var profilePictureSection: FrameLayout
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_profile, container, false)
        settingsIcon = v.findViewById(R.id.settingsIcon)
        userNameTextView = v.findViewById(R.id.userName)
        userEmailTextView = v.findViewById(R.id.userEmail)
        profilePictureSection = v.findViewById(R.id.profilePictureSection)
        profileImageView = v.findViewById(R.id.profileImageView)
        editProfileImageButton = v.findViewById(R.id.editProfileImageButton)
        streakValue = v.findViewById(R.id.streakValue)
        xpValue = v.findViewById(R.id.xpValue)

        settingsIcon.setOnClickListener {
            val settingIntent = Intent(activity, SettingsActivity::class.java)
            startActivity(settingIntent)
        }

        profilePictureSection.setOnClickListener {
            openGallery()
        }

        editProfileImageButton.setOnClickListener {
            openGallery()
        }

        profileImageView.setOnClickListener {
            openGallery()
        }

        // Load the existing profile image
        loadProfileImage()

        populateUserInfo()

        return v;
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            profileImageView.setImageURI(imageUri) // Show selected image
            uploadImageToFirebase(imageUri)
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.child("profile_images/$userId.jpg")

        imageUri?.let {
            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Save image URL to Firebase Database
                        database.child(USERS_REF).child(userId).child("profileImageUrl")
                            .setValue(uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadProfileImage() {
        val userId = auth.currentUser?.uid ?: return
        database.child(USERS_REF).child(userId).child("profileImageUrl")
            .addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profileImageUrl = snapshot.getValue(String::class.java)
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.default_profile)
                            .into(profileImageView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load profile image", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun populateUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child(USERS_REF).child(userId).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("name").getValue(String::class.java)
                    val userEmail = snapshot.child("email").getValue(String::class.java)

                    // Set the values to the TextViews
                    userNameTextView.text = userName ?: "User Name"
                    userEmailTextView.text = userEmail ?: "user@example.com"

                    // Fetch and display streak
                    val currentStreak = snapshot.child("streak").child("currentStreak").getValue(Int::class.java) ?: 0
                    val currentXP = snapshot.child("points").getValue(Int::class.java) ?: 0
                    streakValue.text = currentStreak.toString()
                    xpValue.text = currentXP.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(context, "Failed to load user info", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}