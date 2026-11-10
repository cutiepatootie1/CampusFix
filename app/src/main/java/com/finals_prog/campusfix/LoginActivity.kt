package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.finals_prog.campusfix.data.UserCsvStore

class LoginActivity : AppCompatActivity() {

    private lateinit var store: UserCsvStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        store = UserCsvStore(this)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)

        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty()) {
                etEmail.error = "Enter your email"
            } else if (password.isEmpty()) {
                etPassword.error = "Enter your password"
            } else if (store.authenticate(email, password)) {
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("EMAIL", email)
                startActivity(intent)
                finish()
            } else {
                etPassword.error = "Incorrect email or password"
            }
        }

        findViewById<TextView>(R.id.tv_regRedirect).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}