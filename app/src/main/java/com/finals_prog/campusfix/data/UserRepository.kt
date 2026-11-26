package com.finals_prog.campusfix.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserRepository {

    private val users get() = FirebaseFirestore.getInstance().collection("users")

    suspend fun getUser(uid: String): Users? =
        users.document(uid).get().await().takeIf { it.exists() }?.toObject(Users::class.java)

    /**
     * Every user with the maintenance staff role, sorted by name.
     * The role is matched in code (case/spacing-insensitive, see Users.isMaintenanceStaff) so it
     * works whatever exact string was saved. With a large user base, switch to a
     * `whereEqualTo("role", ...)` query instead.
     */
    suspend fun getMaintenanceStaff(): List<Users> =
        users.get().await().documents
            .mapNotNull { it.toObject(Users::class.java) }
            .filter { it.isMaintenanceStaff }
            .sortedBy { it.fullName.lowercase() }
}