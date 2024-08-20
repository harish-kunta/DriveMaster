package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.harish.drivemaster.R

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignUp = findViewById(R.id.btnSignUp)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign-up successful, populate user data
                            val user = auth.currentUser
                            user?.let {
                                val userId = it.uid
                                val userRef = database.getReference("users/$userId")

                                // Initialize user data
                                val userData = mapOf(
                                    "name" to name,
                                    "email" to email,
                                    "achievements" to mapOf(
                                        "badge1" to false,
                                        "badge2" to false
                                    ),
                                    "progress" to mapOf(
                                        "lesson1" to mapOf("completed" to false, "score" to 0),
                                        "lesson2" to mapOf("completed" to false, "score" to 0)
                                    ),
                                    "points" to 0,
                                    "levels" to mapOf(
                                        "level1" to mapOf("completed" to false, "unlocked" to true)
                                    )
                                )

                                userRef.setValue(userData)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()

                                            // Navigate to DashboardActivity or another screen
                                            val intent = Intent(this, SignInActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Sign-up failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
