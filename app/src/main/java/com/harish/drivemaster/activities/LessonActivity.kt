package com.harish.drivemaster.activities

import android.os.Bundle
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

    private lateinit var tvQuestion: TextView
    private lateinit var popupMessage: TextView
    private lateinit var tvCorrectAnswer: TextView
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnClose: ImageView
    private lateinit var popUpLayout: LinearLayout
    private lateinit var tvHearts: TextView
    private val questions = ArrayList<Question>()
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

        tvQuestion = findViewById(R.id.tvQuestion)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
        btnClose = findViewById(R.id.btnClose)
        popUpLayout = findViewById(R.id.popUpLayout)
        popupMessage = findViewById(R.id.tvPopupMessage)
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer)
        tvHearts = findViewById(R.id.tvHearts)

        auth = FirebaseAuth.getInstance()
        currentLevel = intent.getStringExtra("levelId") ?: return

        btnSubmit.isEnabled = false

        // Fetch questions from Firebase
        val questionsRef =
            FirebaseDatabase.getInstance().getReference("lessons").child(currentLevel)
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

    private fun showNextQuestion() {
        cleanPopUp()
        updateHeartsDisplay()
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            tvQuestion.text = question.questionText
            correctAnswer = question.correctAnswer

            displayOptions(question)
            updateProgressBar()

            selectedAnswer = null
            selectedOptionView = null
            btnSubmit.isEnabled = false
            btnSubmit.text = "CHECK"
            isAnswered = false

            currentQuestionIndex++
        } else {
            updateLevelCompletion()
            Toast.makeText(this@LessonActivity, "You've completed the lesson!", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }


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
                btnSubmit.setBackgroundColor(ContextCompat.getColor(this, R.color.correctAnswerColor))
            }

            optionsContainer.addView(optionView)
        }
    }

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
        val userId = auth.currentUser?.uid ?: return
        val userProgressRef =
            FirebaseDatabase.getInstance().getReference("users").child(userId).child("levels")

        // Mark current level as completed
        userProgressRef.child(currentLevel).child("completed").setValue(true)

        // Unlock the next level
        unlockNextLevel()
    }

    private fun unlockNextLevel() {
        val userId = auth.currentUser?.uid ?: return
        val userLevelsRef =
            FirebaseDatabase.getInstance().getReference("users").child(userId).child("levels")

        val currentLevelIndex = currentLevel.replace("level", "").toIntOrNull() ?: return
        if (currentLevelIndex < 15) {
            val nextLevelIndex = currentLevelIndex + 1
            val nextLevel = "level$nextLevelIndex"

            // Unlock next level
            userLevelsRef.child(nextLevel).child("unlocked").setValue(true)
        }
    }

    private fun updateProgressBar() {
        val progress = ((currentQuestionIndex.toDouble() / questions.size) * 100).toInt()
        progressBar.progress = progress
    }

    private fun evaluateAnswer() {
        if (selectedAnswer == correctAnswer) {
            showAnswerPopup(true)
            updatePoints(10)
            updateCheckButton(true)
        } else {
            loseHeart()
            showAnswerPopup(false)
            updateCheckButton(false)
        }
    }

    private fun loseHeart() {
        heartsLeft--
        updateHeartsDisplay()

        if (heartsLeft <= 0) {
            restartGame()
        }
    }

    private fun restartGame() {
        Toast.makeText(this, "You've lost all hearts! Restarting the game.", Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    private fun updateHeartsDisplay() {
        tvHearts.text = heartsLeft.toString()
    }

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

    private fun showAnswerPopup(isCorrect: Boolean) {
        popupMessage.visibility = View.VISIBLE
        if (isCorrect) {
            popupMessage.text = "Correct Answer!"
            popupMessage.setTextColor(ContextCompat.getColor(this, R.color.correctAnswerColor))
            popUpLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.popupCorrectBackgroundColor
                )
            )
        } else {
            popupMessage.text = "Incorrect!"
            popupMessage.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswerColor))
            tvCorrectAnswer.visibility = View.VISIBLE
            tvCorrectAnswer.text = "The correct answer was: $correctAnswer"
            tvCorrectAnswer.setTextColor(ContextCompat.getColor(this, R.color.wrongAnswerColor))
            popUpLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.popupIncorrectBackgroundColor
                )
            )
        }
    }

    private fun updateCheckButton(isCorrect: Boolean) {
        isAnswered = true
        if (isCorrect) {
            btnSubmit.text = "CONTINUE"
            btnSubmit.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.correctAnswerColor
                )
            )
        } else {
            btnSubmit.text = "GOT IT"
            btnSubmit.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.wrongAnswerColor
                )
            )
        }
    }

    private fun highlightSelectedOption(selectedView: View) {
        selectedOptionView?.setBackgroundResource(R.drawable.option_background_unselected)
        selectedView.setBackgroundResource(R.drawable.option_background_selected)
        val tvOptionText = selectedView.findViewById<TextView>(R.id.tvOptionText)
        tvOptionText.setTextColor(resources.getColor(R.color.selectedOptionTextColor))
        selectedOptionView = selectedView
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
