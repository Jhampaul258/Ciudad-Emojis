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
                // --- LÓGICA MODIFICADA ---
                // 1. Referencia a la colección "directors", no "users"
                val userDocRef = firestore.collection("directors").document(user.uid)

                // 2. Comprueba si el documento YA EXISTE
                val userDoc = userDocRef.get()

                // 3. SOLO escribe los datos SI el documento NO existe (primera vez)
                if (!userDoc.exists) {
                    // 4. Crea un objeto 'Director' en lugar de 'UserProfile'
                    val newDirectorProfile = Director(
                        uid = user.uid,
                        name = user.displayName ?: "Director",
                        email = user.email ?: "",
                        // Mapea photoURL a fotoPerfilUrl
                        fotoPerfilUrl = user.photoURL ?: ""
                        // Todos los demás campos de Director (biografia, universidad, etc.)
                        // usarán sus valores por defecto (ej: "" o emptyList()),
                        // ¡lo cual es perfecto para un perfil nuevo!
                    )

                    // 5. Guarda el nuevo 'Director' en Firestore
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
