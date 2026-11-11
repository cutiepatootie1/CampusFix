package com.finals_prog.campusfix.data
/**
 * One row of reports.csv.
 * Column order matches ReportCsvStorage.HEADER.
 */
data class Report(
    val reportId: String,
    val title: String,
    val description: String,
    val assignedTo: String = "",   // empty until an admin assigns the report
    val photoUrl: String = "",     // empty when no photo was attached
    val building: String,
    val room: String,
    val category: String,
    val status: String = STATUS_REPORTED
) {
    val isResolved: Boolean get() = status.equals(STATUS_RESOLVED, ignoreCase = true)

    companion object {
        const val STATUS_REPORTED = "Reported"
        const val STATUS_ACKNOWLEDGED = "Acknowledged"
        const val STATUS_RESOLVED = "Resolved"
    }
}


