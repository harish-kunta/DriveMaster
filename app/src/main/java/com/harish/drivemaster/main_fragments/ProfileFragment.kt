package com.harish.drivemaster.main_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.activities.SettingsActivity

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
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

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

        settingsIcon.setOnClickListener {
            val settingIntent = Intent(activity, SettingsActivity::class.java)
            startActivity(settingIntent)
        }

        populateUserInfo()

        populateGrid()

        return v;
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