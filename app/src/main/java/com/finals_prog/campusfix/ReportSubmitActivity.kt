package com.finals_prog.campusfix

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportCsvStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ReportSubmitActivity : AppCompatActivity() {

    private lateinit var storage: ReportCsvStorage
    private var selectedPhoto: Uri? = null
    private val photoSubmit by lazy { findViewById<Button>(R.id.btn_photo_submit) }
    private val btnSubmit by lazy { findViewById<Button>(R.id.btn_submit) }
    private val categorySpinner by lazy { findViewById<Spinner>(R.id.category_spinner) }
    private val buildingSpinner by lazy { findViewById<Spinner>(R.id.building_spinner) }
    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhoto = uri
                photoSubmit.text = displayName(uri)
                photoSubmit.isVisible = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_report)
        storage = ReportCsvStorage(this)

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

        val closeBtn = findViewById<ImageView>(R.id.ivClose)
        closeBtn.setOnClickListener { finish() }
        photoSubmit.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnSubmit.setOnClickListener { submit() }
    }

    private fun submit() {
        val etTitle = findViewById<EditText>(R.id.etReportTitle)
        val etReportBody = findViewById<EditText>(R.id.etReportBody)
        val etRoom = findViewById<EditText>(R.id.etRoom)
        val submitBtn = findViewById<Button>(R.id.btn_submit)

        val title = etTitle.text.toString()
        val reportBody = etReportBody.text.toString()
        val room = etRoom.text.toString()
        val category = categorySpinner.selectedItem.toString()
        val building = buildingSpinner.selectedItem.toString()

        when {
            room.isEmpty() -> return etRoom.setError("Required")
            title.isEmpty() -> return etTitle.setError("Required")
            reportBody.isEmpty() -> return etReportBody.setError("Required")
        }

        submitBtn.isEnabled = false
        val photo = selectedPhoto

        lifecycleScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    val photoUrl = photo?.let { copyPhotoToAppStorage(it) }.orEmpty()
                    storage.append { id ->
                        Report(
                            reportId = id,
                            title = title,
                            description = reportBody,
                            assignedTo = "",
                            photoUrl = photoUrl,
                            building = building,
                            room = room,
                            category = category
                        )
                    }
                }
                toast("Report ${saved.reportId} submitted")
                finish()
            } catch (e: Exception) {
                submitBtn.isEnabled = true
                toast("Could not save report: ${e.message}")
            }
        }
    }

    /** Copies the picked image into app storage so it stays available after the picker permission ends. */
    private fun copyPhotoToAppStorage(source: Uri): String {
        val dir = File(filesDir, "report_photos").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: throw IllegalStateException("Cannot read selected photo")
        return Uri.fromFile(dest).toString()
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        } ?: "Photo selected"

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
