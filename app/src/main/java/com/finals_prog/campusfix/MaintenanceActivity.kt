package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportCsvStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MaintenanceActivity : AppCompatActivity() {

    private lateinit var storage: ReportCsvStorage

    private val activeAdapter = TicketAdapter(
        onAcknowledgeClick = { report -> acknowledge(report) },
        onUpdateClick = { report -> openStatusUpdate(report) }
    )
    private val resolvedAdapter = TicketAdapter(null, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintainance_task)

        storage = ReportCsvStorage(this)

        findViewById<RecyclerView>(R.id.rvActive).adapter = activeAdapter
        findViewById<RecyclerView>(R.id.rvResolved).adapter = resolvedAdapter

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
            // TODO: clear whatever session/login state is stored
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun loadReports() {
        lifecycleScope.launch {
            val reports = withContext(Dispatchers.IO) { storage.readAll() }
            showReports(reports)
        }
    }

    private fun showReports(reports: List<Report>) {
        // Newest first: the CSV is append-only
        val (resolved, active) = reports.asReversed().partition { it.isResolved }

        activeAdapter.submitList(active)
        resolvedAdapter.submitList(resolved)

        findViewById<View>(R.id.tvActiveEmpty).isVisible = active.isEmpty()
        findViewById<View>(R.id.tvResolvedEmpty).isVisible = resolved.isEmpty()
    }

    private fun openStatusUpdate(report: Report) {
        startActivity(
            Intent(this, StatusUpdateActivity::class.java)
                .putExtra(StatusUpdateActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }

    private fun acknowledge(report: Report) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                storage.update(report.copy(status = Report.STATUS_ACKNOWLEDGED))
            }
            loadReports() // re-binds the list: "Start working" hides, "Update Status" appears
        }
    }
}