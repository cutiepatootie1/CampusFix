package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportCsvStorage
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var storage: ReportCsvStorage
    private val tvActive by lazy { findViewById<TextView>(R.id.tvActive) }
    private val tvResolved by lazy { findViewById<TextView>(R.id.tvResolved) }
    private val tvRecent by lazy { findViewById<TextView>(R.id.tvRecent) }
    private val adapter = TicketAdapter(null, null) { report ->
        startActivity(
            Intent(this, ReportDetailsActivity::class.java)
                .putExtra(ReportDetailsActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        storage = ReportCsvStorage(this)
        findViewById<RecyclerView>(R.id.rvReports).adapter = adapter

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

        val fabReport = findViewById<FloatingActionButton>(R.id.fabReport)

        fabReport.setOnClickListener {
            val intent = Intent(this, ReportSubmitActivity::class.java)
            startActivity(intent)
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
        val resolvedCount = reports.count { it.isResolved }
        tvActive.text = "Active Reports: ${reports.size - resolvedCount}"
        tvResolved.text = "Resolved: $resolvedCount"

        tvRecent.text = if (reports.isEmpty()) "No reports yet" else "Recent Reports"

        // Newest first: the CSV is append-only, so the last rows are the latest
        adapter.submitList(reports.reversed())
    }

}
