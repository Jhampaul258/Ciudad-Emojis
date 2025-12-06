package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import domain.model.Director
import domain.model.Pelicula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class DirectorRepository {
    private val firestore = Firebase.firestore
    private val directorsCollection = firestore.collection("directors")

    fun getDirector(directorId: String) = flow {
        val director = directorsCollection.document(directorId).get().data<Director>()
        emit(director)
    }

    suspend fun createOrUpdateDirector(director: Director) {
        directorsCollection.document(director.uid).set(director)
    }
    fun getAllDirectors(): Flow<List<Director>> {
        return directorsCollection
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.data<Director>() } }
    }


}