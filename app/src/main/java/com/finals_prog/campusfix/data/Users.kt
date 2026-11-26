package com.finals_prog.campusfix.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class Users(
    @DocumentId val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: String = "",
    val avatarUrl: String? = null,
    val avatarResId: Int = 0
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    // Role checks ignore case, spaces and underscores: "Maintenance Staff", "maintenance_staff", ...
    @get:Exclude
    val isMaintenanceStaff: Boolean
        get() = normalizedRole() in setOf("maintenancestaff", "maintenance")

    @get:Exclude
    val isAdmin: Boolean
        get() = normalizedRole() == "admin"

    private fun normalizedRole() = role.lowercase().filter { it.isLetter() }
}