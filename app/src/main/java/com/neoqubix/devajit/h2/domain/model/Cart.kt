package com.neoqubix.devajit.h2.domain.model

enum class CartStatus(val value: String, val label: String) {
    ACTIVE("active", "Active"),
    INACTIVE("inactive", "Inactive");

    companion object {
        fun from(value: String?): CartStatus = entries.find { it.value == value } ?: INACTIVE
    }
}

data class Cart(
    val id: String,
    val name: String,
    val location: String = "",
    val managerId: String? = null,
    val status: CartStatus = CartStatus.ACTIVE,
    val createdAt: Long? = null
) {
    val isActive: Boolean get() = status == CartStatus.ACTIVE
}
