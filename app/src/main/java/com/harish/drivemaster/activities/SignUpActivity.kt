package com.harish.drivemaster.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.harish.drivemaster.R

class SignUpActivity : AppCompatActivity() {

    private lateinit var userName: TextInputEditText
    private lateinit var userEmailAddress: TextInputEditText
    private lateinit var userPassword: TextInputEditText
    private lateinit var btnSignUp: Button
    private lateinit var btnSignIn: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        userName = findViewById(R.id.name_field)
        userEmailAddress = findViewById(R.id.email_field)
        userPassword = findViewById(R.id.password_field)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnSignIn = findViewById(R.id.btnSignIn)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        btnSignUp.setOnClickListener {
            signUpWithEmail()
        }

        btnSignIn.setOnClickListener {
            openSignInPage()
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
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToDatabase(user)
                } else {
                    Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(user: FirebaseUser?) {
        user?.let {
            val userId = it.uid
            val userRef = database.getReference("users/$userId")

            val userData = mapOf(
                "name" to (userName.text.toString().takeIf { it.isNotEmpty() } ?: user.displayName),
                "email" to user.email,
                // Initialize other fields as necessary
            )

            userRef.setValue(userData).addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                    openSignInPage()
                } else {
                    Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signUpWithEmail() {
        val name = userName.text.toString().trim()
        val email = userEmailAddress.text.toString().trim()
        val password = userPassword.text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        saveUserToDatabase(user)
                    } else {
                        Toast.makeText(this, "Sign-up failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
        }
    }

    // open sign in page
    fun openSignInPage() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
