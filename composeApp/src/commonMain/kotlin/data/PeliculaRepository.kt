package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import domain.model.Pelicula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PeliculaRepository {
    private val firestore = Firebase.firestore
    private val peliculasCollection = firestore.collection("peliculas")

    /**
     * Crea una nueva película y DEVUELVE el objeto con el ID generado.
     */
    suspend fun createPelicula(pelicula: Pelicula): Result<Pelicula> { // <--- Cambio de tipo de retorno
        return try {
            val newDocRef = peliculasCollection.document
            val peliculaWithId = pelicula.copy(id = newDocRef.id)

            newDocRef.set(peliculaWithId)

            Result.success(peliculaWithId) // <--- Devolvemos el objeto completo
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePelicula(pelicula: Pelicula): Result<Pelicula> { // <--- Cambio de tipo para consistencia
        return try {
            if (pelicula.id.isEmpty()) return Result.failure(Exception("ID no válido"))
            peliculasCollection.document(pelicula.id).set(pelicula)
            Result.success(pelicula) // Devolvemos la misma película actualizada
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... (El resto de funciones: getPeliculasByDirector, getAllPeliculasStream se mantienen igual)
    fun getPeliculasByDirector(directorId: String) = kotlinx.coroutines.flow.flow {
        val snapshot = peliculasCollection.where { "directorId" equalTo directorId }.get()
        val peliculas = snapshot.documents.map { it.data<Pelicula>() }
        emit(peliculas)
    }

    fun getAllPeliculasStream(): Flow<List<Pelicula>> {
        return peliculasCollection
            .orderBy("anio", Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.data<Pelicula>() } }
    }
    /**
     * Verifica si una URL ya existe en TODA la base de datos (global).
     * Retorna true si existe, false si está libre.
     */
    suspend fun existsVideoUrl(videoUrl: String): Boolean {
        return try {
            val snapshot = peliculasCollection.where { "videoUrl" equalTo videoUrl }.get()
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false // Si falla la conexión, asumimos false para no bloquear (o manejar error)
        }
    }

    /**
     * Verifica si ya existe un capítulo específico para una serie de un director.
     */
    suspend fun existsChapter(directorId: String, nombreSerie: String, numeroCapitulo: Int): Boolean {
        return try {
            val snapshot = peliculasCollection
                .where { "directorId" equalTo directorId }
                .where { "nombreSerie" equalTo nombreSerie }
                .where { "numeroCapitulo" equalTo numeroCapitulo }
                .get()
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    suspend fun deletePelicula(peliculaId: String): Result<Unit> {
        return try {
            peliculasCollection.document(peliculaId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}