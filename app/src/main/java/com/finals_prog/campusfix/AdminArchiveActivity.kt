package com.finals_prog.campusfix

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportRepository
import kotlinx.coroutines.launch

/** Admin archive: every resolved ticket, newest resolution first, with search. */
class AdminArchiveActivity : AppCompatActivity() {

    private var resolved: List<Report> = emptyList()
    private var searchQuery = ""

    private val adapter = AdminTicketAdapter { report ->
        startActivity(
            Intent(this, ReportDetailsActivity::class.java)
                .putExtra(ReportDetailsActivity.EXTRA_REPORT_ID, report.reportId)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_archive)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.rvArchivedTickets).adapter = adapter

        findViewById<SearchView>(R.id.searchArchive).setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    searchQuery = newText.orEmpty().trim()
                    render()
                    return true
                }
            }
        )

        loadArchive()
    }

    private fun loadArchive() {
        lifecycleScope.launch {
            try {
                resolved = ReportRepository.getAll()
                    .filter { it.isResolved }
                    .sortedByDescending { it.resolvedAt?.toDate()?.time ?: Long.MAX_VALUE }
                render()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminArchiveActivity,
                    "Could not load archive: ${e.localizedMessage ?: e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun render() {
        val visible = if (searchQuery.isEmpty()) resolved else resolved.filter { r ->
            listOf(r.title, r.reportId, r.building, r.room, r.category)
                .any { it.contains(searchQuery, ignoreCase = true) }
        }
        adapter.submitList(visible)
        findViewById<View>(R.id.tvArchiveEmpty).isVisible = visible.isEmpty()
    }
}