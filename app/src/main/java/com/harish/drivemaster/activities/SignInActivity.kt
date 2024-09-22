package com.harish.drivemaster.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.harish.drivemaster.R
import com.harish.drivemaster.models.FirebaseConstants.Companion.EMAIL
import com.harish.drivemaster.models.FirebaseConstants.Companion.NAME
import com.harish.drivemaster.models.FirebaseConstants.Companion.UID
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.USER_JOINED

class SignInActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var userEmailAddress: TextInputEditText
    private lateinit var userPassword: TextInputEditText
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        userEmailAddress = findViewById(R.id.email_field)
        userPassword = findViewById(R.id.password_field)

        database = FirebaseDatabase.getInstance()

        auth = FirebaseAuth.getInstance()

        btnSignIn.setOnClickListener {
            signIn()
        }

        btnSignUp.setOnClickListener {
            signUp()
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }

        configureGoogleSignIn()

        findViewById<SignInButton>(R.id.btnGoogleSignIn).setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkIfUserExists(auth.currentUser?.uid)
                } else {
                    Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun checkIfUserExists(uid: String?) {
        if (uid == null) return
        database.getReference(USERS_REF).child(uid).get().addOnSuccessListener {
            if (it.exists()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // User doesn't exist, save new user info
                saveNewUserToDatabase(auth.currentUser)
            }
        }.addOnFailureListener {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveNewUserToDatabase(user: FirebaseUser?) {
        user?.let {
            val userId = it.uid
            val userRef = database.getReference("users/$userId")
            val username = user.displayName
            val signUpTimestamp = System.currentTimeMillis() / 1000 // Unix time in seconds

            val userData = mapOf(
                NAME to user.displayName,
                EMAIL to user.email,
                UID to user.uid,
                USER_JOINED to signUpTimestamp
            )

            userRef.setValue(userData).addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                    openWelcomePage(username.toString())
                } else {
                    Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signIn() {
        val email = userEmailAddress.text.toString().trim()
        val password = userPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    retrieveFcmToken()
                    if (areNotificationsGranted()) {
                        // If notifications are already granted, proceed directly to SignInActivity
                        val signInIntent = Intent(this, MainActivity::class.java)
                        startActivity(signInIntent)
                        finish()
                    } else {
                        // Otherwise, open the NotificationsActivity
                        val notificationIntent = Intent(this, NotificationsActivity::class.java)
                        startActivity(notificationIntent)
                        finish()
                    }

                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("MainActivity", "FCM Token: $token")
                saveFcmToken(token)  // Save the token to your database
            } else {
                Log.e("MainActivity", "Fetching FCM token failed", task.exception)
            }
        }
    }

    private fun signUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun resetPassword() {
        val email = userEmailAddress.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveFcmToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        val tokenRef = database.getReference(USERS_REF).child(userId).child("fcmToken")

        tokenRef.setValue(token)
            .addOnSuccessListener {
                Log.d("MainActivity", "FCM token saved successfully.")
            }
            .addOnFailureListener { exception ->
                Log.d("MainActivity", "Failed to save FCM token: ${exception.message}")
            }
    }

    private fun areNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    // open welcome page
    fun openWelcomePage(name: String) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

