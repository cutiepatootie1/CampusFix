package com.finals_prog.campusfix.data

import android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Report(
    val reportId: String,
    val title: String,
    val description: String,
    val assignedTo: String = "",
    val photoUrl: String = "",
    val building: String,
    val room: String,
    val category: String,
    val status: String = STATUS_REPORTED,
    val resolutionNotes: String = "",
    val proofPhotos: String = "",   // file paths separated by ";"
    val submittedAt: String = SimpleDateFormat("MMM. dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date()),
    val acknowledgedAt: String = SimpleDateFormat("MMM.dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date()),
    val resolvedAt: String = SimpleDateFormat("MMM.dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date()),
) {
    val isResolved: Boolean get() = status.equals(STATUS_RESOLVED, ignoreCase = true)
    val isAcknowledged: Boolean get() = status.equals(STATUS_ACKNOWLEDGED, ignoreCase = true)

    companion object {
        const val STATUS_REPORTED = "Reported"
        const val STATUS_ACKNOWLEDGED = "Acknowledged"
        const val STATUS_RESOLVED = "Resolved"
    }
}