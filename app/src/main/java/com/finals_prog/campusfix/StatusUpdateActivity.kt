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
import com.finals_prog.campusfix.data.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StatusUpdateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "report_id"
    }
    private val proofUris = mutableListOf<Uri>()

    private val etNotes by lazy { findViewById<EditText>(R.id.etNotes) }
    private val ivProof by lazy { findViewById<ImageView>(R.id.ivProof) }
    private val tvProofCount by lazy { findViewById<TextView>(R.id.tvProofCount) }

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
        findViewById<View>(R.id.btnAddMore).setOnClickListener { pickImage.launch("image/*") }
        findViewById<View>(R.id.btnConfirm).setOnClickListener { confirm() }

        val id = intent.getStringExtra(EXTRA_REPORT_ID)
        lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {

            }
            if (found == null) {
                Toast.makeText(this@StatusUpdateActivity, "Report not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
        }
    }

    private fun confirm() {
        val notes = etNotes.text.toString().trim()
        if (notes.isEmpty()) {
            etNotes.error = "Resolution notes are required"
            return
        }
        lifecycleScope.launch {

            }
            Toast.makeText(this@StatusUpdateActivity, "Status updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }