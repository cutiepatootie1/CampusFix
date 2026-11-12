package com.finals_prog.campusfix

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportCsvStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "report_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_details)

        findViewById<View>(R.id.ivClose).setOnClickListener { finish() }

        val id = intent.getStringExtra(EXTRA_REPORT_ID)
        val storage = ReportCsvStorage(this)

        lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) {
                storage.readAll().firstOrNull { it.reportId == id }
            }
            if (report == null) {
                Toast.makeText(this@ReportDetailsActivity, "Report not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            bind(report)
        }
    }

    private fun bind(report: Report) {
        findViewById<TextView>(R.id.tvReportTitle).text = report.title
        findViewById<TextView>(R.id.tvTicketId).text = "Ticket ID: ${report.reportId}"
        findViewById<TextView>(R.id.tvCategory).text = "Category: ${report.category} | "
        findViewById<TextView>(R.id.tvBuildingRm).text = "Building: ${report.building} - ${report.room}"
        findViewById<TextView>(R.id.tvDescription).text = report.description

        val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
        if (report.photoUrl.isBlank()) {
            ivPhoto.isVisible = false
        } else {
            ivPhoto.isVisible = true
            ivPhoto.setImageURI(Uri.parse(report.photoUrl))
        }

        // Timeline
        findViewById<TextView>(R.id.tvReportedTime).text = report.submittedAt

        val acknowledged = report.status != Report.STATUS_REPORTED   // Acknowledged or Resolved
        val resolved = report.isResolved

        findViewById<View>(R.id.stepAcknowledged).isVisible = acknowledged
        findViewById<View>(R.id.lineReported).isVisible = acknowledged
        findViewById<TextView>(R.id.tvAcknowledgedTime).text = report.acknowledgedAt

        findViewById<View>(R.id.stepResolved).isVisible = resolved
        findViewById<View>(R.id.lineAcknowledged).isVisible = resolved
        findViewById<TextView>(R.id.tvResolvedTime).text = report.resolvedAt
    }
}