package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.finals_prog.campusfix.data.SignUpResult
import com.finals_prog.campusfix.data.UserCsvStore
import kotlin.jvm.java

class SignUpActivity : AppCompatActivity() {

    private lateinit var store : UserCsvStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        store = UserCsvStore(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPass = etConfirm.text.toString()

            when {
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    etEmail.error = "Enter a valid email"

                password.length < 6 ->
                    etPassword.error = "Password must be at least 6 characters"

                password != confirmPass ->
                    etConfirm.error = "Passwords do not match"

                else -> when (store.register(email, password)) {
                    SignUpResult.EMAIL_TAKEN ->
                        etEmail.error = "An account with this email already exists"

                    SignUpResult.SUCCESS -> {
                        Toast.makeText(this, "Account created. Please log in.", Toast.LENGTH_SHORT)
                            .show()
                        goToLogin()

                    }
                }
            }
        }
        findViewById<TextView>(R.id.tvLogin).setOnClickListener { goToLogin() }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}