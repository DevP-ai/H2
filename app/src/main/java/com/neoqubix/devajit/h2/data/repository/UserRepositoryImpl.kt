package com.neoqubix.devajit.h2.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.neoqubix.devajit.h2.data.firebase.Collections
import com.neoqubix.devajit.h2.data.firebase.Fields
import com.neoqubix.devajit.h2.data.firebase.snapshots
import com.neoqubix.devajit.h2.data.firebase.toUserProfile
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val users get() = firestore.collection(Collections.USERS)

    override fun observeUser(uid: String): Flow<UserProfile?> =
        users.document(uid).snapshots().map { if (it.exists()) it.toUserProfile() else null }

    override fun observeManagers(): Flow<List<UserProfile>> =
        users.whereEqualTo(Fields.ROLE, Role.MANAGER.value).snapshots()
            .map { snapshot -> snapshot.documents.map { it.toUserProfile() }.sortedBy { it.name.lowercase() } }

    override suspend fun updateOwnProfile(uid: String, name: String, phone: String): Result<Unit> = runCatching {
        users.document(uid).update(
            mapOf(
                Fields.NAME to name.trim(),
                Fields.PHONE to phone.trim(),
                Fields.UPDATED_AT to FieldValue.serverTimestamp()
            )
        ).awaitOrQueued()
        Unit
    }
}
