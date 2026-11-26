package com.finals_prog.campusfix.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** All Firestore access for the ticket workflow lives here. */
object ReportRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val reports get() = db.collection("reports")

    /** Every report, newest first (admin view). */
    suspend fun getAll(): List<Report> =
        reports.get().await().documents.toSortedReports()

    /** Reports a maintenance staff member is assigned to, newest first. */
    suspend fun getAssignedTo(staffUid: String): List<Report> =
        reports.whereArrayContains("assignedStaff", staffUid).get().await().documents.toSortedReports()

    suspend fun getById(id: String): Report? =
        reports.document(id).get().await().takeIf { it.exists() }?.toReportOrNull()

    /**
     * ADMIN: sets the staff on a ticket (replacing any earlier assignment).
     * Assigning only moves a ticket from Reported -> Acknowledged; later statuses are left alone.
     */
    suspend fun assignStaff(reportId: String, staffUids: List<String>, adminUid: String) {
        val ref = reports.document(reportId)

        db.runTransaction { tx ->
            val current = tx.get(ref).getString("status").orEmpty()
            if (current.equals(Report.STATUS_RESOLVED, ignoreCase = true)) {
                throw IllegalStateException("This ticket is already resolved.")
            }

            val updates = hashMapOf<String, Any>(
                "assignedStaff" to staffUids,
                "assignedBy" to adminUid,
                "assignedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!isPastReported(current)) {
                updates["status"] = Report.STATUS_ACKNOWLEDGED
                updates["acknowledgedAt"] = FieldValue.serverTimestamp()
                updates["acknowledgedBy"] = adminUid
            }
            tx.update(ref, updates)
            Unit
        }.await()
    }

    /**
     * STAFF: marks a ticket "In Progress" when work starts. Only staff assigned to the ticket may do this.
     * If another assigned staff member already started it, nothing changes.
     */
    suspend fun startWork(reportId: String, staffUid: String) {
        val ref = reports.document(reportId)

        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val assigned = (snap.get("assignedStaff") as? List<*>).orEmpty()
            if (!assigned.contains(staffUid)) {
                throw IllegalStateException("This ticket isn't assigned to you.")
            }

            val current = snap.getString("status").orEmpty()
            val done = current.equals(Report.STATUS_IN_PROGRESS, ignoreCase = true) ||
                    current.equals(Report.STATUS_RESOLVED, ignoreCase = true)

            if (!done) {
                tx.update(
                    ref, mapOf(
                        "status" to Report.STATUS_IN_PROGRESS,
                        "startedAt" to FieldValue.serverTimestamp(),
                        "startedBy" to staffUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            Unit
        }.await()
    }

    /** STAFF: marks the ticket Resolved with repair notes and the already-uploaded proof photo URLs. */
    suspend fun resolve(report: Report, staffUid: String, notes: String, proofUrls: List<String>) {
        val updates = hashMapOf<String, Any>(
            "status" to Report.STATUS_RESOLVED,
            "resolutionNotes" to notes,
            "proofPhotos" to proofUrls.joinToString(";"),
            "resolvedAt" to FieldValue.serverTimestamp(),
            "resolvedBy" to staffUid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        // Safety net for tickets that skipped the acknowledged step of the timeline.
        if (report.acknowledgedAt == null) {
            updates["acknowledgedAt"] = FieldValue.serverTimestamp()
            updates["acknowledgedBy"] = staffUid
        }
        reports.document(report.reportId).update(updates).await()
    }

    private fun isPastReported(status: String) = listOf(
        Report.STATUS_ACKNOWLEDGED, Report.STATUS_IN_PROGRESS, Report.STATUS_RESOLVED
    ).any { status.equals(it, ignoreCase = true) }

    private fun List<DocumentSnapshot>.toSortedReports(): List<Report> =
        mapNotNull { it.toReportOrNull() }
            // A just-written report has a null (pending) timestamp: treat it as newest.
            .sortedByDescending { it.submittedAt?.toDate()?.time ?: Long.MAX_VALUE }

    private fun DocumentSnapshot.toReportOrNull(): Report? {
        val report = toObject(Report::class.java) ?: return null
        return if (report.reportId.isEmpty()) report.copy(reportId = id) else report
    }
}