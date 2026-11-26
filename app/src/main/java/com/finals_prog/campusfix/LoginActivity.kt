package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AppCompatActivity
import com.finals_prog.campusfix.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val errorHandler = Handler(Looper.getMainLooper())
    private var hideErrorRunnable: Runnable? = null

    private lateinit var loginBtn: Button
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        binding.etEmail.doAfterTextChanged {
            binding.tilEmail.error = null
            clearError()
        }

        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
            clearError()
        }
        loginBtn = findViewById(R.id.btnLogin)

        loginBtn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                email.isEmpty() -> binding.tilEmail.error = "Enter your email"
                password.isEmpty() -> binding.tilPassword.error = "Enter your password"
                else -> signIn(email, password, etPassword)
            }
        }

        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun signIn(email: String, password: String, etPassword: EditText) {
        loginBtn.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    loginBtn.isEnabled = true
                    binding.tilPassword.error="Login failed. Please try again"
                    return@addOnSuccessListener
                }
                routeByRole(uid, email)
            }
            .addOnFailureListener { e ->
                loginBtn.isEnabled = true
                when (e) {
                    is FirebaseAuthInvalidUserException,
                    is FirebaseAuthInvalidCredentialsException ->
                        showError("Incorrect email or password")
                    else ->
                        showError("Login failed, check your connection")
                }
            }
    }

    private fun routeByRole(uid: String, email: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                // Most common cause of "no role": the document ID isn't this user's UID.
                if (!doc.exists()) {
                    failLogin("No profile found.")
                    return@addOnSuccessListener
                }

                val role = doc.getString("role")?.trim().orEmpty()

                // Ignore case, spaces and underscores: "Maintenance Staff", "maintenance_staff", ...
                val target = when (role.lowercase().filter { it.isLetter() }) {
                    "student", "faculty" -> DashboardActivity::class.java
                    "maintenancestaff", "maintenance" -> MaintenanceActivity::class.java
                    "admin", "administrator" -> AdminActivity::class.java
                    else -> null
                }

                if (target == null) {
                    failLogin(
                        if (role.isEmpty()) "The 'role' field is missing or isn't text."
                        else "Unrecognized role: \"$role\""
                    )
                    return@addOnSuccessListener
                }

                val intent = Intent(this, target)
                intent.putExtra("EMAIL", email)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                failLogin(e.localizedMessage ?: "Could not load your profile")
            }
    }

    private fun failLogin(message: String) {
        auth.signOut()
        loginBtn.isEnabled = true
        showError(message, 8000L)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.tilEmail.isEnabled = !isLoading
        binding.tilPassword.isEnabled = !isLoading
        binding.tvAuthError.visibility = View.GONE
    }
    private fun showError(message: String, durationMs: Long = 4000L) {
        // Cancel any previous timer so it doesn't hide the new error early
        hideErrorRunnable?.let { errorHandler.removeCallbacks(it) }

        binding.tvAuthError.text = message
        binding.errorCard.visibility = View.VISIBLE

        hideErrorRunnable = Runnable {
            binding.errorCard.visibility = View.GONE
        }.also { runnable ->
            errorHandler.postDelayed(runnable, durationMs)
        }
    }

    private fun clearError() {
        hideErrorRunnable?.let { errorHandler.removeCallbacks(it) }
        hideErrorRunnable = null
        binding.errorCard.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        hideErrorRunnable?.let { errorHandler.removeCallbacks(it) }
    }
}