package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.databinding.ActivityDashboardBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardActivity : AppCompatActivity() {

    private val tvActive by lazy { findViewById<TextView>(R.id.tvActive) }
    private val tvResolved by lazy { findViewById<TextView>(R.id.tvResolved) }
    private val tvRecent by lazy { findViewById<TextView>(R.id.tvRecent) }
    private val adapter = TicketAdapter(null, null) { report ->
        startActivity(
            Intent(this, ReportDetailsActivity::class.java)
                .putExtra(ReportDetailsActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViewById<RecyclerView>(R.id.rvReports).adapter = adapter
        loadUserFirstName()

        val menuPanel = findViewById<View>(R.id.menuPanel)
        val menuScrim = findViewById<View>(R.id.menuScrim)

        fun setMenuVisible(show: Boolean) {
            menuPanel.isVisible = show
            menuScrim.isVisible = show
        }

        findViewById<View>(R.id.ivMenu).setOnClickListener { setMenuVisible(!menuPanel.isVisible) }
        menuScrim.setOnClickListener { setMenuVisible(false) }

        // Back button closes the menu first
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (menuPanel.isVisible) setMenuVisible(false)
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })

        findViewById<View>(R.id.btnMenuProfile).setOnClickListener {
            setMenuVisible(false)
            // startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.btnMenuMyReports).setOnClickListener {
            setMenuVisible(false)
            // startActivity(Intent(this, MyReportsActivity::class.java))
        }
        findViewById<View>(R.id.btnMenuLogout).setOnClickListener {
            setMenuVisible(false)
            FirebaseAuth.getInstance().signOut()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }

        findViewById<FloatingActionButton>(R.id.fabReport).setOnClickListener {
            startActivity(Intent(this, ReportSubmitActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserFirstName()
        loadReports()
    }

    /** Loads the signed-in user's reports from the Firestore "reports" collection. */
    private fun loadReports() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("reports")
                    .whereEqualTo("reporterUid", uid)
                    .get()
                    .await()

                // Sort client-side (newest first) so no composite index is required.
                val reports = snapshot.documents
                    .mapNotNull { doc ->
                        val report = doc.toObject(Report::class.java) ?: return@mapNotNull null
                        // Make sure reportId is populated even if the field is missing.
                        val fixed = if (report.reportId.isEmpty()) report.copy(reportId = doc.id) else report
                        // submittedAt is null briefly for a just-written serverTimestamp.
                        val createdMillis = (doc.getTimestamp("submittedAt") ?: doc.getTimestamp("createdAt"))?.toDate()?.time ?: Long.MAX_VALUE
                        fixed to createdMillis
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }

                showReports(reports)
            } catch (e: Exception) {
                Toast.makeText(
                    this@DashboardActivity,
                    "Could not load reports: ${e.localizedMessage ?: e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showReports(reports: List<Report>) {
        val resolvedCount = reports.count { it.isResolved }
        tvActive.text = "Active Reports: ${reports.size - resolvedCount}"
        tvResolved.text = "Resolved: $resolvedCount"

        tvRecent.text = if (reports.isEmpty()) "No reports yet" else "Recent Reports"

        // Already sorted newest first in loadReports()
        adapter.submitList(reports)
    }

    private fun loadUserFirstName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                // Option A: If you store a "firstName" field directly
                val firstName = doc.getString("firstName")?.trim()
                // Option B: Fallback to splitting a "fullName" or "name" field
                    ?: doc.getString("fullName")?.trim()?.substringBefore(" ")

                if (!firstName.isNullOrEmpty()) {
                    tvWelcome.text = "Welcome Back, $firstName"
                }
            }
    }
}