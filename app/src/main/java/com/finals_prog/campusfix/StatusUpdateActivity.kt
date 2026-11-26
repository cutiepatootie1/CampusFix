package com.finals_prog.campusfix

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finals_prog.campusfix.data.CloudinaryUploader
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Staff screen: confirm a ticket as Resolved with repair notes and optional proof photos. */
class StatusUpdateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "report_id"
    }

    private val proofUris = mutableListOf<Uri>()
    private var report: Report? = null

    private val etNotes by lazy { findViewById<EditText>(R.id.etNotes) }
    private val ivProof by lazy { findViewById<ImageView>(R.id.ivProof) }
    private val tvProofCount by lazy { findViewById<TextView>(R.id.tvProofCount) }
    private val btnConfirm by lazy { findViewById<MaterialButton>(R.id.btnConfirm) }
    private val btnAddMore by lazy { findViewById<View>(R.id.btnAddMore) }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                proofUris.add(uri)
                ivProof.setImageURI(uri)
                tvProofCount.text = "${proofUris.size} photo(s) added"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_update)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
        btnAddMore.setOnClickListener { pickImage.launch("image/*") }
        btnConfirm.setOnClickListener { confirm() }
        btnConfirm.isEnabled = false // enabled once the report has loaded

        val id = intent.getStringExtra(EXTRA_REPORT_ID)
        if (id.isNullOrBlank()) {
            toast("Report not found")
            finish()
            return
        }
        loadReport(id)
    }

    private fun loadReport(id: String) {
        lifecycleScope.launch {
            try {
                val found = ReportRepository.getById(id)
                if (found == null) {
                    toast("Report not found")
                    finish()
                    return@launch
                }
                if (found.isResolved) {
                    toast("This ticket is already resolved")
                    finish()
                    return@launch
                }

                report = found
                bind(found)
                btnConfirm.isEnabled = true
            } catch (e: Exception) {
                toast("Could not load report: ${e.localizedMessage ?: e.message}")
                finish()
            }
        }
    }

    private fun bind(r: Report) {
        val submitted = r.submittedAt?.toDate()
            ?.let { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(it) }
            .orEmpty()

        findViewById<TextView>(R.id.tvReportTitle).text = r.title
        findViewById<TextView>(R.id.tvTicketId).text = "Ticket ID: ${r.reportId}"
        findViewById<TextView>(R.id.tvLocation).text = "${r.building} - ${r.room}"
        findViewById<TextView>(R.id.tvSubmitted).text = "Submitted: $submitted"
    }

    private fun confirm() {
        val current = report ?: return

        val notes = etNotes.text.toString().trim()
        if (notes.isEmpty()) {
            etNotes.error = "Resolution notes are required"
            return
        }

        val staffUid = FirebaseAuth.getInstance().currentUser?.uid
        if (staffUid == null) {
            toast("Please sign in again.")
            return
        }

        setSaving(true)
        val photos = proofUris.toList()

        lifecycleScope.launch {
            try {
                // 1) Upload each proof photo to Cloudinary.
                val proofUrls = photos.mapIndexed { index, uri ->
                    CloudinaryUploader.upload(
                        resolver = contentResolver,
                        source = uri,
                        publicId = "${current.reportId}_proof_${index + 1}",
                        folder = "campusfix/proofs"
                    ).url
                }

                // 2) Mark the ticket Resolved in Firestore.
                ReportRepository.resolve(current, staffUid, notes, proofUrls)

                toast("Status updated")
                finish()
            } catch (e: Exception) {
                setSaving(false)
                toast("Could not update ticket: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        btnConfirm.isEnabled = !saving
        btnConfirm.text = if (saving) "Saving..." else "Confirm Resolution"
        btnAddMore.isEnabled = !saving
        etNotes.isEnabled = !saving
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}