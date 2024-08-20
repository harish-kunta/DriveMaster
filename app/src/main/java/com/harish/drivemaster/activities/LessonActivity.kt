package com.harish.drivemaster.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R

class LessonActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvQuestion: TextView
    private lateinit var popupMessage: TextView
    private lateinit var tvCorrectAnswer: TextView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnClose: ImageView
    private lateinit var popUpLayout: LinearLayout
    private lateinit var tvHearts: TextView

    // Data & State Management
    private val questions = mutableListOf<Question>()
    private var currentQuestionIndex = 0
    private var correctAnswer: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var currentLevel: String
    private var selectedAnswer: String? = null
    private var selectedOptionView: View? = null
    private var isAnswered = false

    // Hearts management
    private var heartsLeft = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        initializeUIComponents()
        initializeFirebase()
        setupEventListeners()

        fetchQuestionsFromFirebase()
    }

    // Initialize UI Components
    private fun initializeUIComponents() {
        tvQuestion = findViewById(R.id.tvQuestion)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
        btnClose = findViewById(R.id.btnClose)
        popUpLayout = findViewById(R.id.popUpLayout)
        popupMessage = findViewById(R.id.tvPopupMessage)
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer)
        tvHearts = findViewById(R.id.tvHearts)
        btnSubmit.isEnabled = false
    }

    // Initialize Firebase
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        currentLevel = intent.getStringExtra("levelId") ?: run {
            showErrorAndExit("Invalid level ID")
            return
        }
    }

    // Set up event listeners for UI components
    private fun setupEventListeners() {
        btnSubmit.setOnClickListener {
            if (!isAnswered) {
                evaluateAnswer()
            } else {
                showNextQuestion()
            }
        }
        btnClose.setOnClickListener {
            finish()
        }
    }

    // Fetch questions from Firebase
    private fun fetchQuestionsFromFirebase() {
        val questionsRef = FirebaseDatabase.getInstance()
            .getReference("lessons")
            .child(currentLevel)
            .child("questions")

        questionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                questions.clear()
                dataSnapshot.children.mapNotNullTo(questions) { it.getValue(Question::class.java) }
                if (questions.isNotEmpty()) {
                    updateProgressBar()
                    showNextQuestion()
                } else {
                    showErrorAndExit("No questions available for this level")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to load questions", databaseError.toException())
            }
        })
    }

    // Display the next question
    private fun showNextQuestion() {
        cleanPopUp()
        updateHeartsDisplay()
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            displayQuestion(question)
            currentQuestionIndex++
        } else {
            completeLevel()
        }
    }

    // Display the question and options
    private fun displayQuestion(question: Question) {
        tvQuestion.text = question.questionText
        correctAnswer = question.correctAnswer
        displayOptions(question)
        updateProgressBar()
        resetAnswerSelection()
    }

    // Display the options for the question
    private fun displayOptions(question: Question) {
        val optionsContainer = findViewById<LinearLayout>(R.id.optionsContainer)
        optionsContainer.removeAllViews()
        val options = listOf(question.option1, question.option2, question.option3, question.option4)

        for (option in options) {
            val optionView = layoutInflater.inflate(R.layout.custom_option, optionsContainer, false)
            val tvOptionText = optionView.findViewById<TextView>(R.id.tvOptionText)
            tvOptionText.text = option

            optionView.setOnClickListener {
                selectedAnswer = option
                highlightSelectedOption(optionView)
                btnSubmit.isEnabled = true
                btnSubmit.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.correctAnswerColor
                    )
                )
            }

            optionsContainer.addView(optionView)
        }
    }

    // Evaluate the selected answer
    private fun evaluateAnswer() {
        val isCorrect = selectedAnswer == correctAnswer
        showAnswerPopup(isCorrect)
        if (isCorrect) {
            updatePoints(10)
        } else {
            loseHeart()
        }
        updateCheckButton(isCorrect)
    }

    // Handle losing a heart
    private fun loseHeart() {
        heartsLeft--
        updateHeartsDisplay()
        if (heartsLeft <= 0) {
            restartGame()
        }
    }

    // Update the display of hearts remaining
    private fun updateHeartsDisplay() {
        tvHearts.text = heartsLeft.toString()
    }

    // Restart the game when all hearts are lost
    private fun restartGame() {
        Toast.makeText(this, "You've lost all hearts! Restarting the game.", Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    // Update the progress bar
    private fun updateProgressBar() {
        val progress = ((currentQuestionIndex.toDouble() / questions.size) * 100).toInt()
        progressBar.progress = progress
    }

    // Show the popup indicating whether the answer was correct or incorrect
    private fun showAnswerPopup(isCorrect: Boolean) {
        popupMessage.visibility = View.VISIBLE
        popupMessage.text = if (isCorrect) "Correct Answer!" else "Incorrect!"
        popupMessage.setTextColor(
            ContextCompat.getColor(
                this,
                if (isCorrect) R.color.correctAnswerColor else R.color.wrongAnswerColor
            )
        )
        popUpLayout.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isCorrect) R.color.popupCorrectBackgroundColor else R.color.popupIncorrectBackgroundColor
            )
        )

        if (!isCorrect) {
            tvCorrectAnswer.visibility = View.VISIBLE
            tvCorrectAnswer.text = "The correct answer was: $correctAnswer"
            tvCorrectAnswer.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswerColor))
        }
    }

    // Update the "CHECK" button based on whether the answer was correct
    private fun updateCheckButton(isCorrect: Boolean) {
        isAnswered = true
        btnSubmit.text = if (isCorrect) "CONTINUE" else "GOT IT"
        btnSubmit.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isCorrect) R.color.correctAnswerColor else R.color.wrongAnswerColor
            )
        )
    }

    // Reset the answer selection state
    private fun resetAnswerSelection() {
        selectedAnswer = null
        selectedOptionView = null
        btnSubmit.isEnabled = false
        btnSubmit.text = "CHECK"
        isAnswered = false
    }

    // Highlight the selected option
    private fun highlightSelectedOption(selectedView: View) {
        selectedOptionView?.setBackgroundResource(R.drawable.option_background_unselected)
        selectedView.setBackgroundResource(R.drawable.option_background_selected)
        selectedOptionView = selectedView
    }

    // Update the user's points in Firebase
    private fun updatePoints(points: Int) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.child("points").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentPoints = dataSnapshot.getValue(Int::class.java) ?: 0
                userRef.child("points").setValue(currentPoints + points)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to update points", databaseError.toException())
            }
        })
    }

    // Mark the current level as completed and unlock the next level
    private fun completeLevel() {
        updateLevelCompletion()
        Toast.makeText(this@LessonActivity, "You've completed the lesson!", Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    private fun updateLevelCompletion() {
        val userId = auth.currentUser?.uid ?: return
        val userProgressRef =
            FirebaseDatabase.getInstance().getReference("users").child(userId).child("levels")

        userProgressRef.child(currentLevel).child("completed").setValue(true)
        unlockNextLevel()
    }

    private fun unlockNextLevel() {
        val userId = auth.currentUser?.uid ?: return
        val currentLevelIndex = currentLevel.replace("level", "").toIntOrNull() ?: return
        if (currentLevelIndex < 15) {
            val nextLevel = "level${currentLevelIndex + 1}"
            val userLevelsRef =
                FirebaseDatabase.getInstance().getReference("users").child(userId).child("levels")
            userLevelsRef.child(nextLevel).child("unlocked").setValue(true)
        }
    }

    // Clean the popup and reset it to the default state
    private fun cleanPopUp() {
        popupMessage.visibility = View.GONE
        popupMessage.text = ""
        popUpLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundColor))
        tvCorrectAnswer.visibility = View.GONE
        tvCorrectAnswer.text = ""
        btnSubmit.text = "CHECK"
        btnSubmit.setBackgroundColor(ContextCompat.getColor(this, R.color.textColorSecondary))
        isAnswered = false
    }

    // Log error and show a toast message
    private fun logAndToastError(message: String, exception: Exception) {
        Log.e("LessonActivity", message, exception)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Handle errors and show a message
    private fun showErrorAndExit(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    data class Question(
        val questionText: String = "",
        val option1: String = "",
        val option2: String = "",
        val option3: String = "",
        val option4: String = "",
        val correctAnswer: String = ""
    )

}