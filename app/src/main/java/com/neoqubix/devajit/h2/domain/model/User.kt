package com.neoqubix.devajit.h2.domain.model

enum class Role(val value: String) {
    ADMIN("admin"),
    MANAGER("manager");

    companion object {
        // Anything unknown is treated as the least-privileged role
        fun from(value: String?): Role = entries.find { it.value == value } ?: MANAGER
    }
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val role: Role = Role.MANAGER,
    val cartId: String? = null,
    val createdAt: Long? = null
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
}
