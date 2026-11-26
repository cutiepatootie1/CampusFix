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
    val assignedTo: String = "",
    val photoUrl: String = "",
    val photoPublicId: String = "",
    val building: String = "",
    val room: String = "",
    val category: String = "",
    val status: String = STATUS_REPORTED,
    val resolutionNotes: String = "",
    val proofPhotos: String = "",   // file paths separated by ";"
    val submittedAt: Timestamp? = null,
    val acknowledgedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
) {
    @get:Exclude
    val isResolved: Boolean get() = status.equals(STATUS_RESOLVED, ignoreCase = true)

    @get:Exclude
    val isAcknowledged: Boolean get() = status.equals(STATUS_ACKNOWLEDGED, ignoreCase = true)

    companion object {
        const val STATUS_REPORTED = "Reported"
        const val STATUS_ACKNOWLEDGED = "Acknowledged"
        const val STATUS_RESOLVED = "Resolved"
    }
}