package com.example.network

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager

class SignupActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize
        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        // Initialize views
        val nameField = findViewById<EditText>(R.id.nameField)
        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val confirmPasswordField = findViewById<EditText>(R.id.confirmPasswordField)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val backBtn = findViewById<TextView>(R.id.backBtn)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)

        signupBtn.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()
            val confirmPassword = confirmPasswordField.text.toString()

            // Validation
            if (!validateInput(name, email, password, confirmPassword,
                    nameField, emailField, passwordField, confirmPasswordField)) {
                return@setOnClickListener
            }

            // Show loading
            showLoading(true)
            signupBtn.isEnabled = false

            // Create user (in real app, use coroutines/async)
            Thread {
                // Check if email already exists
                val existingUser = repository.getUserByEmail(email)

                runOnUiThread {
                    if (existingUser != null) {
                        showLoading(false)
                        signupBtn.isEnabled = true
                        emailField.error = "Email already registered"
                        Toast.makeText(
                            this,
                            "This email is already registered",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }

                    // Create new user
                    Thread {
                        val userId = repository.createUser(name, email, password)

                        runOnUiThread {
                            showLoading(false)
                            signupBtn.isEnabled = true

                            if (userId > 0) {
                                // Save session
                                sessionManager.saveLoginSession(userId.toInt(), name, email)

                                Toast.makeText(
                                    this,
                                    "Welcome to NetWork, $name!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Navigate to home
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Registration failed. Please try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }.start()
                }
            }.start()
        }

        backBtn.setOnClickListener {
            finish() // Go back to login
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        nameField: EditText,
        emailField: EditText,
        passwordField: EditText,
        confirmPasswordField: EditText
    ): Boolean {
        // Reset errors
        nameField.error = null
        emailField.error = null
        passwordField.error = null
        confirmPasswordField.error = null

        // Name validation
        if (name.isEmpty()) {
            nameField.error = "Name is required"
            nameField.requestFocus()
            return false
        }

        if (name.length < 2) {
            nameField.error = "Name must be at least 2 characters"
            nameField.requestFocus()
            return false
        }

        // Email validation
        if (email.isEmpty()) {
            emailField.error = "Email is required"
            emailField.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Enter a valid email"
            emailField.requestFocus()
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            passwordField.error = "Password is required"
            passwordField.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordField.error = "Password must be at least 6 characters"
            passwordField.requestFocus()
            return false
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordField.error = "Please confirm your password"
            confirmPasswordField.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordField.error = "Passwords do not match"
            confirmPasswordField.requestFocus()
            return false
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }
}