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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.SettingsActivity
import com.harish.drivemaster.helpers.UserViewModel
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private lateinit var userViewModel: UserViewModel

    // UI Components
    private lateinit var settingsIcon: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userJoinedTextView: TextView
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_profile, container, false)
        initializeUIComponents(v)

        setupViewModel()

        return v;
    }

    private fun setupViewModel() {
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        userViewModel.userName.observe(viewLifecycleOwner, Observer { name ->
            userNameTextView.text = name
        })

        userViewModel.userEmail.observe(viewLifecycleOwner, Observer { email ->
            userEmailTextView.text = email
        })

        userViewModel.userJoined.observe(viewLifecycleOwner, Observer { joined ->
            userJoinedTextView.text = "Joined " + convertUnixToDate(joined)
        })

        userViewModel.profileImageUrl.observe(viewLifecycleOwner, Observer { imageUrl ->
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this@ProfileFragment)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable disk caching
                    .placeholder(R.drawable.default_profile)
                    .into(profileImageView)
            } else {
                // Handle case where imageUrl is empty or null
                profileImageView.setImageResource(R.drawable.default_profile)
            }
        })

        userViewModel.streak.observe(viewLifecycleOwner, Observer { streak ->
            streakValue.text = streak.toString()
        })

        userViewModel.xp.observe(viewLifecycleOwner, Observer { xp ->
            xpValue.text = xp.toString()
        })

        // Only fetch data if it hasn't been loaded yet
        if (userViewModel.isDataLoaded().not()) {
            userViewModel.loadUserData() // Fetch data from Firebase
        }
    }

    // Convert Unix timestamp to a formatted date string like "September 2024"
    private fun convertUnixToDate(unixTime: Long): String {
        // Create a Date object from the Unix timestamp (multiply by 1000 to convert seconds to milliseconds)
        val date = Date(unixTime * 1000L)

        // Define the date format (full month name and year)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Format the date and return the result
        return sdf.format(date)
    }

    private fun initializeUIComponents(view: View) {
        settingsIcon = view.findViewById(R.id.settingsIcon)
        userNameTextView = view.findViewById(R.id.userName)
        userEmailTextView = view.findViewById(R.id.userEmail)
        userJoinedTextView = view.findViewById(R.id.userJoined)
        profilePictureSection = view.findViewById(R.id.profilePictureSection)
        profileImageView = view.findViewById(R.id.profileImageView)
        editProfileImageButton = view.findViewById(R.id.editProfileImageButton)
        streakValue = view.findViewById(R.id.streakValue)
        xpValue = view.findViewById(R.id.xpValue)

        settingsIcon.setOnClickListener {
            val settingIntent = Intent(activity, SettingsActivity::class.java)
            startActivity(settingIntent)
        }

        profilePictureSection.setOnClickListener { openGallery() }
        editProfileImageButton.setOnClickListener { openGallery() }
        profileImageView.setOnClickListener { openGallery() }
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
                        userViewModel.updateProfileImage(uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}