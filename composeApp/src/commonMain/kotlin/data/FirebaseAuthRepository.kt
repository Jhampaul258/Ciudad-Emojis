package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import domain.AuthRepository
import domain.model.Director
import domain.model.UserProfile


class FirebaseAuthRepository : AuthRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            auth.currentUser?.updatePassword(newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    // En FirebaseAuthRepository.kt

    // En FirebaseAuthRepository.kt

    override suspend fun signInWithGoogle(idToken: String, accessToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = accessToken)
            val result = auth.signInWithCredential(credential)

            result.user?.let { user ->
                val userDocRef = firestore.collection("directors").document(user.uid)

                val userDoc = userDocRef.get()

                if (!userDoc.exists) {
                    val newDirectorProfile = Director(
                        uid = user.uid,
                        name = user.displayName ?: "Director",
                        email = user.email ?: "",
                        fotoPerfilUrl = user.photoURL ?: ""

                    )

                    userDocRef.set(newDirectorProfile)
                } else {
                    println("Director ${user.uid} ya existe en Firestore. No se sobrescriben datos.")
                }
                // -------------------------
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun signOutGoogle(): Result<Unit> = signOutGooglePlatform()

    override suspend fun logout() {
        auth.signOut()
    }
}

expect suspend fun signOutGooglePlatform(): Result<Unit>
