package com.finals_prog.campusfix

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.finals_prog.campusfix.data.Report
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class ReportDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "report_id"
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val dateFormat by lazy { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_details)

        findViewById<View>(R.id.ivClose).setOnClickListener { finish() }

        val id = intent.getStringExtra(EXTRA_REPORT_ID)
        if (id.isNullOrBlank()) {
            toast("Report not found")
            finish()
            return
        }

        loadReport(id)
    }

    /** Fetches one report document from Firestore and binds it to the screen. */
    private fun loadReport(id: String) {
        lifecycleScope.launch {
            try {
                val doc = firestore.collection("reports").document(id).get().await()
                val report = doc.toObject(Report::class.java)

                if (report == null) {
                    toast("Report not found")
                    finish()
                    return@launch
                }

                bind(if (report.reportId.isEmpty()) report.copy(reportId = doc.id) else report)
            } catch (e: Exception) {
                toast("Could not load report: ${e.localizedMessage ?: e.message}")
            }
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
            loadPhoto(ivPhoto, report.photoUrl)
        }

        // Timeline
        findViewById<TextView>(R.id.tvReportedTime).text = formatTime(report.submittedAt)

        val resolved = report.isResolved
        val acknowledged = report.isAcknowledged || report.isInProgress || resolved   // Acknowledged or Resolved

        findViewById<View>(R.id.stepAcknowledged).isVisible = acknowledged
        findViewById<View>(R.id.lineReported).isVisible = acknowledged
        findViewById<TextView>(R.id.tvAcknowledgedTime).text = formatTime(report.acknowledgedAt)

        findViewById<View>(R.id.stepResolved).isVisible = resolved
        findViewById<View>(R.id.lineAcknowledged).isVisible = resolved
        findViewById<TextView>(R.id.tvResolvedTime).text = formatTime(report.resolvedAt)
    }

    private fun formatTime(ts: Timestamp?): String =
        ts?.toDate()?.let { dateFormat.format(it) }.orEmpty()

    /**
     * Downloads the hosted image and shows it. Cloudinary URLs are resized to ~1000px wide
     * so a full-size phone photo isn't decoded into memory.
     * (Glide/Coil can replace this with one line if you add either library.)
     */
    private fun loadPhoto(view: ImageView, url: String) {
        val sized = if (url.contains("res.cloudinary.com") && url.contains("/upload/")) {
            url.replace("/upload/", "/upload/w_1000,q_auto/")
        } else url

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(sized).openStream().use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
            if (bitmap != null) view.setImageBitmap(bitmap) else view.isVisible = false
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}