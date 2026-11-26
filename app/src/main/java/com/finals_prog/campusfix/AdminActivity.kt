package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportRepository
import com.finals_prog.campusfix.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Admin home. "Reported Issues" holds tickets that are not yet being worked on
 * (Reported, or Acknowledged = staff assigned but not started); "In Progress" holds the rest.
 * Resolved tickets live in the archive.
 */
class AdminActivity : AppCompatActivity() {

    private val pendingAdapter = AdminTicketAdapter { openAssign(it) }
    private val inProgressAdapter = AdminTicketAdapter { openAssign(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        findViewById<RecyclerView>(R.id.rvReportedIssues).adapter = pendingAdapter
        findViewById<RecyclerView>(R.id.rvInProgressIssues).adapter = inProgressAdapter

        loadWelcome()

        // Jump buttons: "Active" -> top section, "In Progress" -> second section
        val scroll = findViewById<NestedScrollView>(R.id.scrollAdmin)
        findViewById<View>(R.id.btnFilterActive).setOnClickListener { scroll.smoothScrollTo(0, 0) }
        findViewById<View>(R.id.btnFilterInProgress).setOnClickListener {
            scroll.smoothScrollTo(0, findViewById<View>(R.id.tvInProgressHeader).top)
        }

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

        findViewById<View>(R.id.btnMenuArchive).setOnClickListener {
            setMenuVisible(false)
            startActivity(Intent(this, AdminArchiveActivity::class.java))
        }
        findViewById<View>(R.id.btnMenuSettings).setOnClickListener {
            setMenuVisible(false)
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.btnMenuLogout).setOnClickListener {
            setMenuVisible(false)
            FirebaseAuth.getInstance().signOut()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    // Reload on return so tickets move between sections after assigning staff.
    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun loadReports() {
        lifecycleScope.launch {
            try {
                showReports(ReportRepository.getAll())
            } catch (e: Exception) {
                toast("Could not load tickets: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun showReports(reports: List<Report>) {
        val open = reports.filter { !it.isResolved }
        val (inProgress, pending) = open.partition { it.isInProgress }

        pendingAdapter.submitList(pending)
        inProgressAdapter.submitList(inProgress)

        findViewById<View>(R.id.tvReportedEmpty).isVisible = pending.isEmpty()
        findViewById<View>(R.id.tvInProgressEmpty).isVisible = inProgress.isEmpty()
    }

    private fun loadWelcome() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            val first = runCatching { UserRepository.getUser(uid) }.getOrNull()
                ?.firstName?.trim().orEmpty()
            if (first.isNotEmpty()) {
                findViewById<TextView>(R.id.tvWelcomeAdmin).text = "Welcome, $first!"
            }
        }
    }

    private fun openAssign(report: Report) {
        startActivity(
            Intent(this, AssignTaskActivity::class.java)
                .putExtra(AssignTaskActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}