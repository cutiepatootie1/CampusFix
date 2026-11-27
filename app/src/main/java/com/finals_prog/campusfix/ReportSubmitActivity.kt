package com.finals_prog.campusfix

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finals_prog.campusfix.data.CloudinaryUploader
import com.finals_prog.campusfix.data.Report
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReportSubmitActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var selectedPhoto: Uri? = null

    private val photoSubmit by lazy { findViewById<Button>(R.id.btn_photo_submit) }
    private val btnSubmit by lazy { findViewById<Button>(R.id.btn_submit) }
    private val categorySpinner by lazy { findViewById<Spinner>(R.id.category_spinner) }
    private val buildingSpinner by lazy { findViewById<Spinner>(R.id.building_spinner) }
    private val progressIndicator by lazy { findViewById<CircularProgressIndicator>(R.id.progressIndicator) }

    private val tilRoom by lazy { findViewById<TextInputLayout>(R.id.tilRoom) }
    private val tilReportTitle by lazy { findViewById<TextInputLayout>(R.id.tilReportTitle) }
    private val tilReportBody by lazy { findViewById<TextInputLayout>(R.id.tilReportBody) }

    private val etRoom by lazy { findViewById<TextInputEditText>(R.id.etRoom) }
    private val etReportTitle by lazy { findViewById<TextInputEditText>(R.id.etReportTitle) }
    private val etReportBody by lazy { findViewById<TextInputEditText>(R.id.etReportBody) }

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhoto = uri
                photoSubmit.text = displayName(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_report)

        setupSpinners()

        findViewById<ImageView>(R.id.ivClose).setOnClickListener { finish() }
        photoSubmit.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnSubmit.setOnClickListener { submit() }
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            this,
            R.array.category_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.building_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            buildingSpinner.adapter = adapter
        }
    }

    private fun submit() {
        val currentUser = auth.currentUser?.uid
        if (currentUser == null) {
            toast("Please sign in before submitting a report.")
            return
        }

        val room = etRoom.text?.toString()?.trim().orEmpty()
        val title = etReportTitle.text?.toString()?.trim().orEmpty()
        val reportBody = etReportBody.text?.toString()?.trim().orEmpty()
        val category = categorySpinner.selectedItem?.toString().orEmpty()
        val building = buildingSpinner.selectedItem?.toString().orEmpty()

        tilRoom.error = if (room.isEmpty()) "Required" else null
        tilReportTitle.error = if (title.isEmpty()) "Required" else null
        tilReportBody.error = if (reportBody.isEmpty()) "Required" else null

        if (room.isEmpty() || title.isEmpty() || reportBody.isEmpty()) return

        setSubmitting(true)
        val photo = selectedPhoto

        lifecycleScope.launch {
            try {
                val docRef = firestore.collection("reports").document()

                // 1) Upload the photo (if any) to Cloudinary.
                val upload = photo?.let {
                    CloudinaryUploader.upload(contentResolver, it, docRef.id, "campusfix/reports")
                }

                // 2) Save the report document with the hosted image URL.
                val reportData = hashMapOf(
                    "reportId" to docRef.id,
                    "reporterUid" to currentUser,
                    "title" to title,
                    "description" to reportBody,
                    "category" to category,
                    "building" to building,
                    "room" to room,
                    "status" to Report.STATUS_REPORTED,
                    "assignedStaff" to emptyList<String>(),
                    "photoUrl" to (upload?.url ?: ""),
                    "photoPublicId" to (upload?.publicId ?: ""),
                    "submittedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                docRef.set(reportData).await()

                toast("Report submitted")
                finish()
            } catch (e: Exception) {
                setSubmitting(false)
                toast("Could not submit report: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun setSubmitting(submitting: Boolean) {
        btnSubmit.isEnabled = !submitting
        photoSubmit.isEnabled = !submitting
        categorySpinner.isEnabled = !submitting
        buildingSpinner.isEnabled = !submitting
        tilRoom.isEnabled = !submitting
        tilReportTitle.isEnabled = !submitting
        tilReportBody.isEnabled = !submitting
        progressIndicator.visibility = if (submitting) View.VISIBLE else View.GONE
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        } ?: "Photo selected"

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}