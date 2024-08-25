package com.harish.drivemaster.main_fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
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
import com.harish.drivemaster.activities.EntryActivity
import com.harish.drivemaster.activities.SettingsActivity
import de.hdodenhof.circleimageview.CircleImageView

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var gridLayout: GridLayout
    private lateinit var settingsIcon: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var signOutButton: Button
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
        gridLayout = v.findViewById(R.id.gridLayout)
        settingsIcon = v.findViewById(R.id.settingsIcon)
        userNameTextView = v.findViewById(R.id.userName)
        userEmailTextView = v.findViewById(R.id.userEmail)
        signOutButton = v.findViewById(R.id.signOutButton)
        profilePictureSection = v.findViewById(R.id.profilePictureSection)
        profileImageView = v.findViewById(R.id.profileImageView)
        editProfileImageButton = v.findViewById(R.id.editProfileImageButton)

        settingsIcon.setOnClickListener {
            val settingIntent = Intent(activity, SettingsActivity::class.java)
            startActivity(settingIntent)
        }

        signOutButton.setOnClickListener {
            //sign out and redirect to sign in activity
            auth.signOut().also {
                val signInIntent = Intent(activity, EntryActivity::class.java)
                startActivity(signInIntent)
                activity?.finish()
            }
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

        populateGrid()

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
                        database.child("users").child(userId).child("profileImageUrl").setValue(uri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadProfileImage() {
        val userId = auth.currentUser?.uid ?: return
        database.child("users").child(userId).child("profileImageUrl").addListenerForSingleValueEvent(object :
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
                Toast.makeText(context, "Failed to load profile image", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("name").getValue(String::class.java)
                    val userEmail = snapshot.child("email").getValue(String::class.java)

                    // Set the values to the TextViews
                    userNameTextView.text = userName ?: "User Name"
                    userEmailTextView.text = userEmail ?: "user@example.com"
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

    private fun populateGrid() {
        for (i in 0 until 16) { // 4x4 grid
            val inflater = LayoutInflater.from(requireContext())
            val itemView = inflater.inflate(R.layout.grid_item, gridLayout, false)
            val itemText = itemView.findViewById<TextView>(R.id.itemText)

            itemText.text = "Item ${i + 1}" // Set your item text here

            // Set layout parameters for positioning in GridLayout
            val layoutParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                width = 0 // Match parent column width
                height = 0 // Match parent row height
                setMargins(4, 4, 4, 4) // Margin between items
            }
            itemView.layoutParams = layoutParams

            gridLayout.addView(itemView)
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