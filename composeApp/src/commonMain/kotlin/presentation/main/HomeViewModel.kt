package presentation.main

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.PeliculaRepository
import domain.model.Pelicula
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Estado de la UI: Define qué puede mostrar la pantalla
data class HomeUiState(
    val isLoading: Boolean = true,
    val groupedContent: List<Pelicula> = emptyList(),
    val seriesList: List<Pelicula> = emptyList(),
    val moviesList: List<Pelicula> = emptyList(),
    val featuredContent: Pelicula? = null
)

class HomeViewModel(
    private val peliculaRepository: PeliculaRepository = PeliculaRepository()
) : ScreenModel {

    // Transformamos el flujo de datos de la base de datos en un estado listo para la UI
    val state: StateFlow<HomeUiState> = peliculaRepository.getAllPeliculasStream()
        .map { allPeliculas ->
            if (allPeliculas.isEmpty()) {
                HomeUiState(isLoading = false)
            } else {
                // 1. Agrupación Maestra (Lógica que estaba en la UI)
                val grouped = allPeliculas
                    .groupBy { if (it.esSerie && it.nombreSerie.isNotBlank()) it.nombreSerie else it.id }
                    .map { (_, videos) ->
                        // 2. De cada grupo, elegimos cuál mostrar:
                        if (videos.first().esSerie) {
                            // Si es serie, buscamos el capítulo con el número más bajo (ej: 1)
                            videos.minByOrNull { it.numeroCapitulo } ?: videos.first()
                        } else {
                            // Si es película, simplemente tomamos la que hay
                            videos.first()
                        }
                    }

                HomeUiState(
                    isLoading = false,
                    groupedContent = grouped,
                    seriesList = grouped.filter { it.esSerie },
                    moviesList = grouped.filter { !it.esSerie },
                    featuredContent = allPeliculas.firstOrNull() // El más reciente sin agrupar
                )
            }
        }
        .stateIn(
            scope = screenModelScope, // Scope atado al ciclo de vida de la pantalla
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )
}