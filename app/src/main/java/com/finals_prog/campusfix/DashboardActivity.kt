package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        storage = ReportCsvStorage(this)

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
            val recent = reports.takeLast(2).asReversed()
            bindReportCard(
                R.id.cardReport1,
                R.id.tvReport1Title,
                R.id.tvReport1Status,
                recent.getOrNull(0)
            )
            bindReportCard(
                R.id.cardReport2,
                R.id.tvReport2Title,
                R.id.tvReport2Status,
                recent.getOrNull(1)
            )
        }

        private fun bindReportCard(cardId: Int, titleId: Int, statusId: Int, report: Report?) {
            val card = findViewById<MaterialCardView>(cardId)
            card.isVisible = report != null
            if (report == null) return

            // e.g. "[HVAC] College Building - Room 203"
            findViewById<TextView>(titleId).text =
                "[${report.category}] ${report.building} - ${report.room}"
            // e.g. "AC Leaking | Reported"
            findViewById<TextView>(statusId).text = "${report.title} | ${report.status}"
        }


}
