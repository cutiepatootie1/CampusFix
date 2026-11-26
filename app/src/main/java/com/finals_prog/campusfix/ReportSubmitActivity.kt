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
import com.finals_prog.campusfix.data.Report
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ReportSubmitActivity : AppCompatActivity() {

    companion object {
        // Cloudinary dashboard -> top-left "Cloud name".
        private const val CLOUDINARY_CLOUD_NAME = "kbqtj3ps"

        // Settings -> Upload -> Upload presets. Must be created with Signing Mode = "Unsigned".
        private const val CLOUDINARY_UPLOAD_PRESET = "kotlin_uploads"

        private const val CLOUDINARY_FOLDER = "campusfix/reports"
    }

    private data class UploadResult(val url: String, val publicId: String)

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
        val currentUser = auth.currentUser
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
                    withContext(Dispatchers.IO) { uploadToCloudinary(it, docRef.id) }
                }

                // 2) Save the report document with the hosted image URL.
                val reportData = hashMapOf(
                    "reportId" to docRef.id,
                    "reporterUid" to currentUser.uid,
                    "title" to title,
                    "description" to reportBody,
                    "category" to category,
                    "building" to building,
                    "room" to room,
                    "status" to Report.STATUS_REPORTED,
                    "assignedTo" to "",
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

    /**
     * Uploads the image to Cloudinary using an unsigned upload preset (no API secret in the app).
     * Runs a plain multipart POST, so no extra dependency is needed. Call from Dispatchers.IO.
     */
    private fun uploadToCloudinary(source: Uri, reportId: String): UploadResult {
        val boundary = "----CampusFix${UUID.randomUUID()}"
        val endpoint = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload"
        val mime = contentResolver.getType(source) ?: "image/jpeg"

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setChunkedStreamingMode(0) // stream the file instead of buffering it in memory
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(conn.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }

                field("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                field("folder", CLOUDINARY_FOLDER)
                field("public_id", reportId)

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$reportId.jpg\"\r\n")
                out.writeBytes("Content-Type: $mime\r\n\r\n")
                contentResolver.openInputStream(source)?.use { it.copyTo(out) }
                    ?: throw IOException("Cannot read selected photo")
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val msg = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP $code")
                throw IOException("Photo upload failed: $msg")
            }

            val json = JSONObject(body)
            return UploadResult(
                url = json.getString("secure_url"),
                publicId = json.getString("public_id")
            )
        } finally {
            conn.disconnect()
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