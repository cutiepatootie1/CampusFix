package com.finals_prog.campusfix.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

// Every property needs a default value so Firestore can build the object (no-arg constructor).
@IgnoreExtraProperties
data class Report(
    val reportId: String = "",
    val reporterUid: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "",                      // legacy single-assignee field, no longer used
    val assignedStaff: List<String> = emptyList(),    // uids of the maintenance staff on this ticket
    val assignedBy: String = "",                      // admin uid
    val assignedAt: Timestamp? = null,
    val photoUrl: String = "",
    val photoPublicId: String = "",
    val building: String = "",
    val room: String = "",
    val category: String = "",
    val status: String = STATUS_REPORTED,
    val resolutionNotes: String = "",
    val proofPhotos: String = "",   // Cloudinary image URLs separated by ";"
    val submittedAt: Timestamp? = null,
    val acknowledgedAt: Timestamp? = null,
    val acknowledgedBy: String = "",
    val startedAt: Timestamp? = null,                 // when the first staff member set "In Progress"
    val startedBy: String = "",
    val resolvedAt: Timestamp? = null,
    val resolvedBy: String = "",
) {
    @get:Exclude
    val isResolved: Boolean get() = status.equals(STATUS_RESOLVED, ignoreCase = true)

    @get:Exclude
    val isAcknowledged: Boolean get() = status.equals(STATUS_ACKNOWLEDGED, ignoreCase = true)

    @get:Exclude
    val isInProgress: Boolean get() = status.equals(STATUS_IN_PROGRESS, ignoreCase = true)

    /** Reported, Acknowledged and In Progress tickets are "active"; only Resolved is not. */
    @get:Exclude
    val isActive: Boolean get() = !isResolved

    @get:Exclude
    val proofPhotoList: List<String> get() = proofPhotos.split(";").filter { it.isNotBlank() }

    companion object {
        const val STATUS_REPORTED = "Reported"
        const val STATUS_ACKNOWLEDGED = "Acknowledged"
        const val STATUS_IN_PROGRESS = "In Progress"
        const val STATUS_RESOLVED = "Resolved"
    }
}