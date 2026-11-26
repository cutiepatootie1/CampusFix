package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.finals_prog.campusfix.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthPasswordDoesNotMeetRequirementsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val errorHandler = Handler(Looper.getMainLooper())
    private var hideErrorRunnable: Runnable? = null

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleDropdown()

        binding.btnSignUp.setOnClickListener { submitUser() }
        binding.tvLogIn.setOnClickListener { goToLogin() }
    }

    /**
     * The role field is now an exposed dropdown (TextInputLayout + AutoCompleteTextView),
     * so it needs an ArrayAdapter on the AutoCompleteTextView instead of a Spinner adapter.
     */
    private fun setupRoleDropdown() {
        val roles = resources.getStringArray(R.array.role_choices)
        binding.actvRole.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, roles)
        )
        // Replace the hardcoded "Student" from the XML with the first real choice.
        // filter = false keeps the dropdown showing every option, not just matches.
        if (roles.isNotEmpty()) {
            binding.actvRole.setText(roles[0], false)
        }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tilRole.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignUp.isEnabled = !loading
        binding.progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun submitUser() {
        clearErrors()

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()
        val role = binding.actvRole.text.toString().trim()
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()

        when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                binding.tilEmail.error = "Enter a valid email"

            password.length < 6 ->
                binding.tilPassword.error = "Password must be at least 6 characters"

            password != confirmPass ->
                binding.tilConfirmPassword.error = "Passwords do not match"

            role.isEmpty() ->
                binding.tilRole.error = "Select a role"


            else -> register(email, password, role, firstName, lastName)
        }
    }
    /**
     * Checks the password against the policy configured in the Firebase console
     * before creating the account, so we can show exactly what's missing.
     */
    private fun friendlyPolicyMessage(reason: String?): String {
        if (reason.isNullOrBlank()) return "Password does not meet the requirements"
        val inner = reason.substringAfter('[', "").substringBeforeLast(']', "")
        if (inner.isBlank()) return reason
        return inner.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n") { "• $it" }
    }

    private fun register(email: String, password: String, role: String, firstName: String,
                         lastName: String) {
        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    setLoading(false)
                    showError("Sign up failed. Please try again.")
                    return@addOnSuccessListener
                }

                val profile = hashMapOf(
                    "email" to email,
                    "role" to role,
                    "firstName" to firstName,
                    "lastName" to lastName
                )

                db.collection("users").document(user.uid).set(profile)
                    .addOnSuccessListener {
                        // Firebase signs the new user in automatically; sign out so
                        // the "Account created. Please log in." flow still applies.
                        auth.signOut()
                        showNotice("Account created. Please log in")
                        goToLogin()
                    }
                    .addOnFailureListener { e ->
                        // Roll back so we don't leave an account with no role.
                        user.delete()
                        auth.signOut()
                        setLoading(false)
                        showError("Could not save your profile")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                when (e) {
                    is FirebaseAuthUserCollisionException ->
                        binding.tilEmail.error = "An account with this email already exists"
                    // Thrown when the project's password policy rejects the password.
                    // The reason lists the unmet requirements (length, uppercase, etc.).
                    is FirebaseAuthPasswordDoesNotMeetRequirementsException ->
                        binding.tilPassword.error = friendlyPolicyMessage(e.reason)
                    // Older "weak password" rejection (e.g. under 6 characters).
                    is FirebaseAuthWeakPasswordException ->
                        binding.tilPassword.error = e.reason ?: "Password is too weak"
                    else ->
                        showError("Sign up failed. Check your connection")
                }
            }
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
    private fun showNotice(message: String, durationMs: Long = 4000L) {
        // Cancel any previous timer so it doesn't hide the new error early
        hideErrorRunnable?.let { errorHandler.removeCallbacks(it) }

        binding.tvAuthNotice.text = message
        binding.modalCard.visibility = View.VISIBLE

        hideErrorRunnable = Runnable {
            binding.modalCard.visibility = View.GONE
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

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}