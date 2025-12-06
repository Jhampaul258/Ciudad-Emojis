package presentation.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.DirectorRepository
import data.FirebaseAuthRepository
import data.PeliculaRepository
import domain.model.Director
import domain.model.Pelicula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import utils.getYoutubeThumbnail

data class PeliculaDetailUiState(
    val directorAutor: Director? = null, // El que creó la película
    val recomendaciones: List<Pelicula> = emptyList(),
    val tituloSeccion: String = "",
    val headerThumbnail: String = "",

    // Nuevos estados para Admin/Dueño
    val canDelete: Boolean = false,
    val isDeleted: Boolean = false,

    val isLoading: Boolean = true
)

class PeliculaDetailViewModel(
    private val pelicula: Pelicula,
    private val peliculaRepository: PeliculaRepository = PeliculaRepository(),
    private val directorRepository: DirectorRepository = DirectorRepository(),
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository() // Necesario para saber quién soy
) : ScreenModel {

    private val _uiState = MutableStateFlow(PeliculaDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
        checkPermissions()
    }

    private fun loadData() {
        screenModelScope.launch {
            _uiState.update { it.copy(headerThumbnail = getYoutubeThumbnail(pelicula.videoUrl)) }

            // 1. Cargar datos del Director de la película
            launch {
                directorRepository.getDirector(pelicula.directorId).collect { director ->
                    _uiState.update { it.copy(directorAutor = director) }
                }
            }

            // 2. Recomendaciones
            launch {
                peliculaRepository.getAllPeliculasStream().collect { all ->
                    calculateRecommendations(all)
                }
            }
        }
    }

    // Lógica de Seguridad: ¿Puedo borrar esto?
    private fun checkPermissions() {
        screenModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                // Buscamos mis datos para ver si soy admin
                directorRepository.getDirector(currentUserId).collect { currentUser ->
                    val isAdmin = currentUser?.isAdmin == true
                    val isOwner = currentUserId == pelicula.directorId

                    _uiState.update { it.copy(canDelete = isAdmin || isOwner) }
                }
            }
        }
    }

    fun deletePelicula() {
        screenModelScope.launch {
            peliculaRepository.deletePelicula(pelicula.id)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    private fun calculateRecommendations(allPeliculas: List<Pelicula>) {
        // (Tu lógica de recomendaciones anterior se mantiene igual)
        val recomendaciones: List<Pelicula>
        val titulo: String

        val episodiosHermanos = if (pelicula.esSerie && pelicula.nombreSerie.isNotBlank()) {
            allPeliculas.filter {
                it.esSerie && it.nombreSerie.equals(pelicula.nombreSerie, ignoreCase = true) && it.id != pelicula.id
            }.sortedBy { it.numeroCapitulo }.take(30)
        } else emptyList()

        if (episodiosHermanos.isNotEmpty()) {
            recomendaciones = episodiosHermanos
            titulo = "Más episodios de ${pelicula.nombreSerie}"
        } else {
            val delDirector = allPeliculas
                .filter { it.directorId == pelicula.directorId && it.id != pelicula.id }
                .distinctBy { if (it.esSerie && it.nombreSerie.isNotBlank()) it.nombreSerie else it.id }
                .take(10)

            if (delDirector.isNotEmpty()) {
                recomendaciones = delDirector
                titulo = "Más de ${pelicula.directorName}"
            } else {
                recomendaciones = allPeliculas
                    .filter { it.id != pelicula.id }
                    .distinctBy { if (it.esSerie && it.nombreSerie.isNotBlank()) it.nombreSerie else it.id }
                    .shuffled().take(5)
                titulo = "Te podría gustar"
            }
        }
        _uiState.update { it.copy(isLoading = false, recomendaciones = recomendaciones, tituloSeccion = titulo) }
    }
}