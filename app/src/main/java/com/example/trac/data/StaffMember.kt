package com.example.trac.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StaffMember(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    @SerialName("active_tasks") val activeTasks: Int = 0,
    @SerialName("is_available") val isAvailable: Boolean = true
)
