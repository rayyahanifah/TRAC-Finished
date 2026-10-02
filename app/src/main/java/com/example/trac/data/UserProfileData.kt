package com.example.trac.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileData(
    @SerialName("user_id") val userId: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("user_class") val userClass: String = "XI RPL",
    @SerialName("role") val role: String = "Siswa",
    @SerialName("profile_image") val profileImage: String = ""
)
