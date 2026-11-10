package com.finals_prog.campusfix

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            if (etPassword.text.toString() != etConfirm.text.toString()) {
                etConfirm.error = "Passwords do not match"
            }
        }
    }
}