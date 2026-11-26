package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MaintenanceActivity : AppCompatActivity() {

    private var allReports: List<Report> = emptyList()
    private var searchQuery = ""

    // Active tickets (Acknowledged / In Progress): "Start Work" while Acknowledged, "Mark Resolved" while In Progress
    private val activeAdapter = TicketAdapter(
        onStartClick = { report -> startWork(report) },
        onUpdateClick = { report -> openStatusUpdate(report) }
    )

    // Resolved tickets: tap to view details
    private val resolvedAdapter = TicketAdapter(null, null) { report -> openDetails(report) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintainance_task)

        findViewById<RecyclerView>(R.id.rvActive).adapter = activeAdapter
        findViewById<RecyclerView>(R.id.rvResolved).adapter = resolvedAdapter

        findViewById<SearchView>(R.id.search_bar).setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    searchQuery = newText.orEmpty().trim()
                    render()
                    return true
                }
            }
        )

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
    }

    // Reload on return so tickets move to "Resolved" right after StatusUpdateActivity finishes.
    override fun onResume() {
        super.onResume()
        loadReports()
    }

    /** Staff only see the tickets an admin assigned to them. */
    private fun loadReports() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                showReports(ReportRepository.getAssignedTo(uid))
            } catch (e: Exception) {
                toast("Could not load tickets: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun showReports(reports: List<Report>) {
        allReports = reports
        render()
    }

    /** Applies the search filter, then splits into Active (everything not resolved) and Resolved. */
    private fun render() {
        val visible = if (searchQuery.isEmpty()) allReports else allReports.filter { r ->
            listOf(r.title, r.reportId, r.building, r.room, r.category)
                .any { it.contains(searchQuery, ignoreCase = true) }
        }

        // Already sorted newest first by the repository.
        val (resolved, active) = visible.partition { it.isResolved }

        activeAdapter.submitList(active)
        resolvedAdapter.submitList(resolved)

        findViewById<View>(R.id.tvActiveEmpty).isVisible = active.isEmpty()
        findViewById<View>(R.id.tvResolvedEmpty).isVisible = resolved.isEmpty()
    }

    private fun openDetails(report: Report) {
        startActivity(
            Intent(this, ReportDetailsActivity::class.java)
                .putExtra(ReportDetailsActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }

    private fun openStatusUpdate(report: Report) {
        startActivity(
            Intent(this, StatusUpdateActivity::class.java)
                .putExtra(StatusUpdateActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }

    /** Sets the ticket to "In Progress" when the staff member starts working on it. */
    private fun startWork(report: Report) {
        val staffUid = FirebaseAuth.getInstance().currentUser?.uid
        if (staffUid == null) {
            toast("Please sign in again.")
            return
        }
        if (report.isInProgress) {
            toast("Already in progress")
            return
        }

        lifecycleScope.launch {
            try {
                ReportRepository.startWork(report.reportId, staffUid)
                toast("Ticket marked In Progress")
                loadReports()
            } catch (e: Exception) {
                toast("Could not start work: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}