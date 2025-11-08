package com.example.network

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.widget.ProgressBar
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize database and session
        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome()
            return
        }

        // Initialize views
        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val signupLink = findViewById<TextView>(R.id.signupLink)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)

        loginBtn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            // Validation
            if (!validateInput(email, password, emailField, passwordField)) {
                return@setOnClickListener
            }

            // Show loading
            showLoading(true)
            loginBtn.isEnabled = false

            // Authenticate (in real app, use coroutines/async)
            Thread {
                val user = repository.authenticateUser(email, password)

                runOnUiThread {
                    showLoading(false)
                    loginBtn.isEnabled = true

                    if (user != null) {
                        // Save session
                        sessionManager.saveLoginSession(user.userId, user.name, user.email)

                        Toast.makeText(
                            this,
                            "Welcome back, ${user.name}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToHome()
                    } else {
                        Toast.makeText(
                            this,
                            "Invalid email or password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun validateInput(
        email: String,
        password: String,
        emailField: EditText,
        passwordField: EditText
    ): Boolean {
        // Reset errors
        emailField.error = null
        passwordField.error = null

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

        return true
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }
}