package presentation.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.DirectorRepository
import data.PeliculaRepository
import domain.model.Director
import domain.model.Pelicula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import utils.getYoutubeThumbnail

// Estado único de la pantalla: La UI solo necesita leer esto para saber qué pintar
data class PeliculaDetailUiState(
    val director: Director? = null,
    val recomendaciones: List<Pelicula> = emptyList(),
    val tituloSeccion: String = "",
    val headerThumbnail: String = "",
    val isLoading: Boolean = true
)

class PeliculaDetailViewModel(
    private val pelicula: Pelicula, // La película actual que se está viendo
    private val peliculaRepository: PeliculaRepository = PeliculaRepository(),
    private val directorRepository: DirectorRepository = DirectorRepository()
) : ScreenModel {

    private val _uiState = MutableStateFlow(PeliculaDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            // 1. Generar la miniatura inmediatamente (Lógica de presentación pura)
            _uiState.update {
                it.copy(headerThumbnail = getYoutubeThumbnail(pelicula.videoUrl))
            }

            // 2. Cargar datos del Director (Lógica de datos)
            launch {
                directorRepository.getDirector(pelicula.directorId).collect { director ->
                    _uiState.update { it.copy(director = director) }
                }
            }

            // 3. Cargar y Calcular recomendaciones (Lógica de negocio)
            launch {
                peliculaRepository.getAllPeliculasStream().collect { allPeliculas ->
                    calculateRecommendations(allPeliculas)
                }
            }
        }
    }

    private fun calculateRecommendations(allPeliculas: List<Pelicula>) {
        val recomendaciones: List<Pelicula>
        val titulo: String

        // A. Si es serie, buscar otros episodios de la misma serie
        val episodiosHermanos = if (pelicula.esSerie && pelicula.nombreSerie.isNotBlank()) {
            allPeliculas.filter {
                it.esSerie &&
                        it.nombreSerie.equals(pelicula.nombreSerie, ignoreCase = true) &&
                        it.id != pelicula.id
            }.sortedBy { it.numeroCapitulo }
        } else emptyList()

        if (episodiosHermanos.isNotEmpty()) {
            recomendaciones = episodiosHermanos
            titulo = "Más episodios de ${pelicula.nombreSerie}"
        } else {
            // B. Si no es serie, buscar otras obras del mismo director
            val delDirector = allPeliculas.filter {
                it.directorId == pelicula.directorId && it.id != pelicula.id
            }

            if (delDirector.isNotEmpty()) {
                recomendaciones = delDirector
                titulo = "Más de ${pelicula.directorName}"
            } else {
                // C. Fallback: Sugerencias aleatorias de otros
                recomendaciones = allPeliculas
                    .filter { it.id != pelicula.id }
                    .shuffled()
                    .take(5)
                titulo = "Te podría gustar"
            }
        }

        // Actualizamos el estado una sola vez con todo listo
        _uiState.update {
            it.copy(
                isLoading = false,
                recomendaciones = recomendaciones,
                tituloSeccion = titulo
            )
        }
    }
}