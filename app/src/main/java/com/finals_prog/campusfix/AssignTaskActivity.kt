package com.finals_prog.campusfix

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.finals_prog.campusfix.data.Report
import com.finals_prog.campusfix.data.ReportRepository
import com.finals_prog.campusfix.data.UserRepository
import com.finals_prog.campusfix.data.Users
import com.finals_prog.campusfix.utils.ImageLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/** Admin screen: view a ticket and assign one or more maintenance staff to it. */
class AssignTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPORT_ID = "report_id"
    }

    private var reportId = ""
    private var report: Report? = null
    private var staffList: List<Users> = emptyList()

    private val assignedAdapter = AssignedStaffAdapter()

    private val btnAssign by lazy { findViewById<MaterialButton>(R.id.btnAssignStaff) }
    private val btnChange by lazy { findViewById<MaterialButton>(R.id.btnChangeStaff) }
    private val cardAssigned by lazy { findViewById<View>(R.id.cardAssignedStaff) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_task)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.rvAssignedStaff).adapter = assignedAdapter
        btnAssign.setOnClickListener { showStaffDialog() }
        btnChange.setOnClickListener { showStaffDialog() }

        val id = intent.getStringExtra(EXTRA_REPORT_ID)
        if (id.isNullOrBlank()) {
            toast("Report not found")
            finish()
            return
        }
        reportId = id
        load()
    }

    private fun load() {
        btnAssign.isEnabled = false
        btnChange.isEnabled = false

        lifecycleScope.launch {
            try {
                val found = ReportRepository.getById(reportId)
                if (found == null) {
                    toast("Report not found")
                    finish()
                    return@launch
                }
                staffList = UserRepository.getMaintenanceStaff()
                report = found
                bind(found)
            } catch (e: Exception) {
                toast("Could not load ticket: ${e.localizedMessage ?: e.message}")
                finish()
            } finally {
                btnAssign.isEnabled = true
                btnChange.isEnabled = true
            }
        }
    }

    private fun bind(r: Report) {
        findViewById<TextView>(R.id.tvReportTitle).text = r.title
        findViewById<TextView>(R.id.tvReportId).text = "Report ID: ${r.reportId}"
        findViewById<TextView>(R.id.tvBuildingRoom).text = "${r.building} | ${r.room}"
        findViewById<TextView>(R.id.tvCategory).text = "Category: ${r.category}"
        findViewById<TextView>(R.id.tvDescription).text = r.description

        val hasPhoto = r.photoUrl.isNotBlank()
        findViewById<TextView>(R.id.tvImagePlaceholder).apply {
            isVisible = !hasPhoto
            text = "No photo attached"
        }
        ImageLoader.load(findViewById<ImageView>(R.id.ivReportImage), r.photoUrl, 1000)

        renderAssignment(r)
    }

    /** State A: nobody assigned -> "Assign staff(s)". State B: staff list + "Change". */
    private fun renderAssignment(r: Report) {
        val canAssign = !r.isResolved
        val byUid = staffList.associateBy { it.uid }
        // uid with no matching staff user (e.g. role changed since) still shows up as "Unknown staff".
        val assigned = r.assignedStaff.map { byUid[it] ?: Users(uid = it, firstName = "Unknown staff") }

        assignedAdapter.submitList(assigned)
        cardAssigned.isVisible = assigned.isNotEmpty()
        btnChange.isVisible = canAssign
        btnAssign.isVisible = canAssign && assigned.isEmpty()
    }

    private fun showStaffDialog() {
        val current = report ?: return

        val view = layoutInflater.inflate(R.layout.dialog_select_staff, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvStaffChecklist)
        val tvEmpty = view.findViewById<View>(R.id.tvStaffEmpty)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnConfirmStaff)

        val adapter = StaffChecklistAdapter(staffList, current.assignedStaff.toMutableSet())
        rv.adapter = adapter
        // android:maxHeight isn't supported by RecyclerView, so cap long lists here (~6 rows).
        rv.layoutParams = rv.layoutParams.apply {
            height = if (staffList.size > 6) (340 * resources.displayMetrics.density).toInt()
            else ViewGroup.LayoutParams.WRAP_CONTENT
        }
        rv.isVisible = staffList.isNotEmpty()
        tvEmpty.isVisible = staffList.isEmpty()
        btnSave.isEnabled = staffList.isNotEmpty()

        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()

        view.findViewById<View>(R.id.btnCancelStaff).setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            if (adapter.selected.isEmpty()) {
                toast("Select at least one staff member")
                return@setOnClickListener
            }
            dialog.dismiss()
            saveAssignment(adapter.selected.toList())
        }
        dialog.show()
    }

    private fun saveAssignment(staffUids: List<String>) {
        val adminUid = FirebaseAuth.getInstance().currentUser?.uid
        if (adminUid == null) {
            toast("Please sign in again.")
            return
        }

        btnAssign.isEnabled = false
        btnChange.isEnabled = false

        lifecycleScope.launch {
            try {
                ReportRepository.assignStaff(reportId, staffUids, adminUid)
                ReportRepository.getById(reportId)?.let {
                    report = it
                    bind(it)
                }
                toast("Staff assigned")
            } catch (e: Exception) {
                toast("Could not assign staff: ${e.localizedMessage ?: e.message}")
            } finally {
                btnAssign.isEnabled = true
                btnChange.isEnabled = true
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}