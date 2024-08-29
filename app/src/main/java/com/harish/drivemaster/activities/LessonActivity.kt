package com.harish.drivemaster.activities

import android.os.Build
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.harish.drivemaster.R
import com.harish.drivemaster.helpers.HapticFeedbackUtil
import com.harish.drivemaster.helpers.SoundUtil
import com.harish.drivemaster.models.FirebaseConstants.Companion.COMPLETED_LEVELS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.CURRENT_STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.HEARTS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.LAST_ACTIVITY_DATE_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.LESSONS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.POINTS_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.STREAK_REF
import com.harish.drivemaster.models.FirebaseConstants.Companion.USERS_REF
import java.time.LocalDate

class LessonActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvQuestion: TextView
    private lateinit var popupMessage: TextView
    private lateinit var popupResultView: LinearLayout
    private lateinit var correctIcon: ImageView
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
    private lateinit var currentLevelId: String
    private var selectedAnswer: String? = null
    private var selectedOptionView: View? = null
    private var selectedOptionText: TextView? = null
    private var isAnswered = false
    private lateinit var database: DatabaseReference

    // Data & State Management
    private var accumulatedPoints = 0

    // Hearts management
    private var heartsLeft = 0

    // Streak management variables
    private var currentStreak = 0
    private lateinit var lastActivityDate: LocalDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        initializeUIComponents()
        initializeFirebase()
        initializeStreakTracking()
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
        popupResultView = findViewById(R.id.popUpResultView)
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer)
        correctIcon = findViewById(R.id.correctImage)
        tvHearts = findViewById(R.id.tvHearts)
        btnSubmit.isEnabled = false
    }

    // Initialize Firebase
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        currentLevelId = intent.getStringExtra("levelId") ?: run {
            showErrorAndExit("Invalid level ID")
            return
        }
        currentLevel = "level$currentLevelId"

        // Load hearts and regeneration time
        loadHeartsData()
    }

    // Load hearts and last regeneration time from Firebase
    private fun loadHeartsData() {
        val userId = auth.currentUser?.uid ?: return
        val heartsRef = database.child(USERS_REF).child(userId).child(HEARTS_REF)

        heartsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                heartsLeft = dataSnapshot.child("heartsLeft").getValue(Int::class.java) ?: 5
                val lastRegenTime = dataSnapshot.child("lastRegenTime").getValue(Long::class.java)
                    ?: System.currentTimeMillis()

                // Check if hearts need to be regenerated
                regenerateHearts(lastRegenTime)
                updateHeartsDisplay()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to load hearts data", databaseError.toException())
            }
        })
    }

    // Regenerate hearts based on the time elapsed
    private fun regenerateHearts(lastRegenTime: Long) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastRegenTime
        val hoursElapsed = elapsedTime / (2 * 60 * 60 * 1000) // 2 hours in milliseconds

        if (hoursElapsed > 0 && heartsLeft < 5) {
            heartsLeft = minOf(heartsLeft + hoursElapsed.toInt(), 5)
            saveHeartsData(currentTime)
        }
    }

    // Save hearts data to Firebase
    private fun saveHeartsData(lastRegenTime: Long) {
        val userId = auth.currentUser?.uid ?: return
        val heartsData = mapOf(
            "heartsLeft" to heartsLeft,
            "lastRegenTime" to lastRegenTime
        )

        database.child(USERS_REF).child(userId).child(HEARTS_REF).setValue(heartsData)
            .addOnSuccessListener {
                Log.d("LessonActivity", "Hearts data saved successfully.")
            }
            .addOnFailureListener { exception ->
                logAndToastError("Failed to save hearts data", exception)
            }
    }

    private fun initializeStreakTracking() {
        val userId = auth.currentUser?.uid ?: return
        val streakRef = database.child(USERS_REF).child(userId).child(STREAK_REF)

        streakRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                currentStreak =
                    dataSnapshot.child(CURRENT_STREAK_REF).getValue(Int::class.java) ?: 0
                lastActivityDate =
                    dataSnapshot.child(LAST_ACTIVITY_DATE_REF).getValue(String::class.java)?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            LocalDate.parse(it)
                        } else {
                            TODO("VERSION.SDK_INT < O")
                        }
                    } ?: LocalDate.now()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to load streak data", databaseError.toException())
            }
        })
    }

    private fun checkAndUpdateStreak() {
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        when {
            today.isEqual(lastActivityDate) -> {
                // Same day, streak remains the same
            }

            today.isEqual(lastActivityDate.plusDays(1)) -> {
                // Next day, increment streak
                currentStreak++
            }

            else -> {
                // More than a day passed, reset streak
                currentStreak = 1
            }
        }

        // Save streak data to Firebase
        saveStreakData(today)
    }

    private fun saveStreakData(today: LocalDate) {
        val userId = auth.currentUser?.uid ?: return
        val streakRef = database.child(USERS_REF).child(userId).child(STREAK_REF)

        val streakData = mapOf(
            CURRENT_STREAK_REF to currentStreak,
            LAST_ACTIVITY_DATE_REF to today.toString()
        )

        streakRef.setValue(streakData)
            .addOnSuccessListener {
                Log.d("LessonActivity", "Streak data saved successfully.")
            }
            .addOnFailureListener { exception ->
                logAndToastError("Failed to save streak data", exception)
            }
    }

    // Set up event listeners for UI components
    private fun setupEventListeners() {
        btnSubmit.setOnClickListener {
            // Perform haptic feedback
            HapticFeedbackUtil.performHapticFeedback(this)
            if (heartsLeft > 0 && !isAnswered) {
                evaluateAnswer()
            } else if (isAnswered) {
                showNextQuestion()
            } else {
                Toast.makeText(
                    this,
                    "You don't have enough hearts to continue.",
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
            }
        }
        btnClose.setOnClickListener {
            // Perform haptic feedback
            HapticFeedbackUtil.performHapticFeedback(this)
            finish()
        }
    }

    // Fetch questions from Firebase
    private fun fetchQuestionsFromFirebase() {
        val questionsRef = FirebaseDatabase.getInstance()
            .getReference(LESSONS_REF)
            .child(currentLevel)
            .child("questions")

        questionsRef.keepSynced(true)

        questionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                questions.clear()
                dataSnapshot.children.mapNotNullTo(questions) { it.getValue(Question::class.java) }

                // Randomize the order of questions
                questions.shuffle()

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
        val options = listOf(question.option1, question.option2, question.option3, question.option4).shuffled()

        for (option in options) {
            val optionView = layoutInflater.inflate(R.layout.custom_option, optionsContainer, false)
            val tvOptionText = optionView.findViewById<TextView>(R.id.tvOptionText)
            tvOptionText.text = option

            optionView.setOnClickListener {
                selectedAnswer = option
                highlightSelectedOption(optionView, tvOptionText)
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
        // Perform haptic feedback
        HapticFeedbackUtil.performHapticFeedback(this)
        showAnswerPopup(isCorrect)
        if (isCorrect) {
            SoundUtil.getInstance(this).playSuccessSound()
            accumulatedPoints += 10  // Accumulate points locally
        } else {
            SoundUtil.getInstance(this).playFailureSound()
            loseHeart()
        }
        updateCheckButton(isCorrect)
    }

    // Handle losing a heart
    private fun loseHeart() {
        if (heartsLeft > 0) {
            heartsLeft--
            saveHeartsData(System.currentTimeMillis())
            updateHeartsDisplay()

            if (heartsLeft <= 0) {
                disableGameplay()
            }
        }
    }

    private fun disableGameplay() {
        btnSubmit.isEnabled = false
        Toast.makeText(
            this,
            "You've lost all hearts! Wait for them to regenerate.",
            Toast.LENGTH_SHORT
        )
            .show()
        finish()
    }

    // Update the display of hearts remaining
    private fun updateHeartsDisplay() {
        tvHearts.text = heartsLeft.toString()
    }

    // Update the progress bar
    private fun updateProgressBar() {
        val progress = (((currentQuestionIndex + 1).toDouble()  / questions.size) * 100).toInt()
        progressBar.progress = progress
    }

    // Show the popup indicating whether the answer was correct or incorrect
    private fun showAnswerPopup(isCorrect: Boolean) {
        popupResultView.visibility = View.VISIBLE
        popupMessage.text = if (isCorrect) "Correct Answer!" else "Incorrect!"
        correctIcon.setImageResource(if (isCorrect) R.drawable.ic_correct_icon else R.drawable.ic_wrong_icon)
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
            tvCorrectAnswer.text = "Correct Answer:\n$correctAnswer"
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
        selectedOptionText = null
        btnSubmit.isEnabled = false
        btnSubmit.text = "CHECK"
        isAnswered = false
    }

    // Highlight the selected option
    private fun highlightSelectedOption(selectedView: View, selectedTextView: TextView) {
        selectedOptionView?.setBackgroundResource(R.drawable.option_background_unselected)
        selectedOptionText?.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary))
        selectedView.setBackgroundResource(R.drawable.option_background_selected)
        selectedTextView.setTextColor(ContextCompat.getColor(this, R.color.selectedOptionTextColor))
        selectedOptionView = selectedView
        selectedOptionText = selectedTextView
    }

    // Update the user's points in Firebase
    private fun updatePoints(points: Int) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference(USERS_REF).child(userId)

        userRef.child(POINTS_REF).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentPoints = dataSnapshot.getValue(Int::class.java) ?: 0
                userRef.child(POINTS_REF).setValue(currentPoints + points)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                logAndToastError("Failed to update points", databaseError.toException())
            }
        })
    }

    // Mark the current level as completed and unlock the next level
    private fun completeLevel() {
        updatePointsInDatabase()
        updateLevelCompletion()
        checkAndUpdateStreak()
        Toast.makeText(this@LessonActivity, "You've completed the lesson!", Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    private fun updatePointsInDatabase() {
        val userId = auth.currentUser?.uid ?: return
        val pointsRef = database.child(USERS_REF).child(userId).child(POINTS_REF)

        pointsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentPoints = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentPoints + accumulatedPoints
                return Transaction.success(currentData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (databaseError != null) {
                    logAndToastError("Failed to update points", databaseError.toException())
                } else {
                    Log.d("LessonActivity", "Points updated successfully.")
                }
            }
        })
    }

    private fun updateLevelCompletion() {
        val userId = auth.currentUser?.uid ?: return
        val userLevelsRef =
            FirebaseDatabase.getInstance().getReference(USERS_REF).child(userId)
                .child(COMPLETED_LEVELS_REF)
        userLevelsRef.child(currentLevelId).setValue(true)
    }

    // Clean the popup and reset it to the default state
    private fun cleanPopUp() {
        popupResultView.visibility = View.GONE
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