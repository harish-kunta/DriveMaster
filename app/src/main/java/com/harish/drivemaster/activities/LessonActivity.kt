package com.harish.drivemaster.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R

class LessonActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var rgOptions: RadioGroup
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private val questions = ArrayList<Question>()
    private var currentQuestionIndex = 0
    private var correctAnswer: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var currentLevel: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        tvQuestion = findViewById(R.id.tvQuestion)
        rgOptions = findViewById(R.id.rgOptions)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)

        auth = FirebaseAuth.getInstance()
        currentLevel = intent.getStringExtra("levelId") ?: return

        // Fetch questions from Firebase
        val questionsRef = FirebaseDatabase.getInstance().getReference("lessons").child("level"+currentLevel)
            .child("questions")
        questionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                questions.clear()
                for (questionSnapshot in dataSnapshot.children) {
                    val question = questionSnapshot.getValue(Question::class.java)
                    question?.let { questions.add(it) }
                }
                updateProgressBar()
                showNextQuestion()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LessonActivity, "Failed to load questions.", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        btnSubmit.setOnClickListener {
            val selectedId = rgOptions.checkedRadioButtonId
            val selectedRadioButton = findViewById<RadioButton>(selectedId)
            val selectedAnswer = selectedRadioButton.text.toString()

            if (selectedAnswer == correctAnswer) {
                Toast.makeText(this@LessonActivity, "Correct Answer!", Toast.LENGTH_SHORT).show()
                updatePoints(10) // Award 10 points
                showNextQuestion()
            } else {
                Toast.makeText(this@LessonActivity, "Wrong Answer! Try Again.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            tvQuestion.text = question.questionText
            Log.d("LessonActivity", "Question: ${question.questionText}")
            Log.d(
                "LessonActivity",
                "Options: ${question.option1}, ${question.option2}, ${question.option3}, ${question.option4}"
            )
            Log.d("LessonActivity", "Correct Answer: ${question.correctAnswer}")

            // Ensure there are exactly 4 RadioButtons
            if (rgOptions.childCount >= 4) {
                (rgOptions.getChildAt(0) as RadioButton).text = question.option1
                (rgOptions.getChildAt(1) as RadioButton).text = question.option2
                (rgOptions.getChildAt(2) as RadioButton).text = question.option3
                (rgOptions.getChildAt(3) as RadioButton).text = question.option4
            } else {
                Log.e(
                    "LessonActivity",
                    "RadioGroup does not contain the expected number of RadioButtons."
                )
                // Handle this scenario appropriately
            }

            correctAnswer = question.correctAnswer
            // Reset RadioGroup selection
            rgOptions.clearCheck()
            updateProgressBar()
            currentQuestionIndex++
        } else {
            updateLevelCompletion()
            unlockNextLevel()
            Toast.makeText(this@LessonActivity, "You've completed the lesson!", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    data class Question(
        val questionText: String = "",
        val option1: String = "",
        val option2: String = "",
        val option3: String = "",
        val option4: String = "",
        val correctAnswer: String = ""
    )

    private fun updatePoints(points: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(it)
            userRef.child("points").run {
                addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val currentPoints = dataSnapshot.getValue(Int::class.java) ?: 0
                        userRef.child("points").setValue(currentPoints + points)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle possible errors.
                    }
                })
            }
        }
    }

    private fun updateLevelCompletion() {
        val userId = auth.currentUser?.uid
        userId?.let {
            val userProgressRef = FirebaseDatabase.getInstance().getReference("users").child(it).child("progress").child(currentLevel)
            userProgressRef.child("completed").setValue(true)
        }
    }

    private fun unlockNextLevel() {
        val userId = auth.currentUser!!.uid
        val userLevelsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("levels")

        // Get the current level index from the levelId (assuming "level1", "level2", etc.)
        val currentLevelIndex = currentLevel.replace("level", "").toIntOrNull() ?: return
        val nextLevelIndex = currentLevelIndex + 1
        val nextLevel = "level$nextLevelIndex"

        userLevelsRef.child(nextLevel).setValue(true)
    }

    private fun updateProgressBar() {
        val progress = ((currentQuestionIndex.toDouble() / questions.size) * 100).toInt()
        progressBar.progress = progress
    }
}
