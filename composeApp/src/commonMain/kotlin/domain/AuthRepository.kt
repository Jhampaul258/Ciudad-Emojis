package domain

interface AuthRepository {
    fun getCurrentUserId(): String?
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String, accessToken: String): Result<Unit>
    suspend fun signOutGoogle(): Result<Unit>

    suspend fun logout()

}