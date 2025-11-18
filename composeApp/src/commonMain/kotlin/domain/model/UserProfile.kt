package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val isVerified: Boolean = false
)