package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import domain.model.Pelicula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class PeliculaRepository {
    private val firestore = Firebase.firestore
    // Creamos una colección de alto nivel para todas las películas
    private val peliculasCollection = firestore.collection("peliculas")

    /**
     * Crea una nueva película en Firestore.
     * Firestore generará un ID automático para el documento.
     */
    suspend fun createPelicula(pelicula: Pelicula): Result<Unit> {
        return try {
            // Genera un nuevo documento con un ID único
            val newDocRef = peliculasCollection.document

            // Asigna el ID generado al objeto antes de guardarlo
            val peliculaWithId = pelicula.copy(id = newDocRef.id)

            // Guarda la película en la base de datos
            newDocRef.set(peliculaWithId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todas las películas de un director específico.
     */
    fun getPeliculasByDirector(directorId: String) = flow {
        val snapshot = peliculasCollection
            .where { "directorId" equalTo directorId }
            .get()

        val peliculas = snapshot.documents.map { it.data<Pelicula>() }
        emit(peliculas)
    }

    fun getAllPeliculasStream(): Flow<List<Pelicula>> {
        return peliculasCollection
            // Ordena por "anio" de forma descendente.
            // (Idealmente, aquí usarías un campo "fechaDeSubida" o "timestamp")
            .orderBy("anio", Direction.DESCENDING)
            .snapshots() // <-- ¡Esta es la magia! Escucha en tiempo real.
            .map { snapshot ->
                // Convierte los documentos de Firestore al modelo Pelicula
                snapshot.documents.map { it.data<Pelicula>() }
            }
    }
    suspend fun updatePelicula(pelicula: Pelicula): Result<Unit> {
        return try {
            if (pelicula.id.isEmpty()) return Result.failure(Exception("ID de película no válido"))

            // Sobrescribe el documento existente con los nuevos datos
            peliculasCollection.document(pelicula.id).set(pelicula)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}