package presentation.main

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.PeliculaRepository
import domain.model.Pelicula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,

    val groupedContent: List<Pelicula> = emptyList(),
    val seriesList: List<Pelicula> = emptyList(),
    val moviesList: List<Pelicula> = emptyList(),
    val featuredContent: Pelicula? = null,


    val searchQuery: String = "",
    val selectedGenre: String = "Todos",
    val availableGenres: List<String> = emptyList()
)

class HomeViewModel(
    private val peliculaRepository: PeliculaRepository = PeliculaRepository()
) : ScreenModel {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedGenre = MutableStateFlow("Todos")

    val state: StateFlow<HomeUiState> = combine(
        peliculaRepository.getAllPeliculasStream(),
        _searchQuery,
        _selectedGenre
    ) { allPeliculas, query, genre ->

        if (allPeliculas.isEmpty()) {
            HomeUiState(isLoading = false)
        } else {
            val genres = listOf("Todos") + allPeliculas.map { it.genero }.distinct().sorted()

            val filteredList = allPeliculas.filter { pelicula ->
                val matchesSearch = pelicula.titulo.contains(query, ignoreCase = true) ||
                        pelicula.directorName.contains(query, ignoreCase = true)||
                        (pelicula.esSerie && pelicula.nombreSerie.contains(query, ignoreCase = true)) // <--- ¡ESTO FALTABA!
                val matchesGenre = genre == "Todos" || pelicula.genero.equals(genre, ignoreCase = true)

                matchesSearch && matchesGenre
            }

            val grouped = filteredList
                .groupBy { if (it.esSerie && it.nombreSerie.isNotBlank()) it.nombreSerie else it.id }
                .map { (_, videos) ->
                    if (videos.first().esSerie) {
                        videos.minByOrNull { it.numeroCapitulo } ?: videos.first()
                    } else {
                        videos.first()
                    }
                }
                .sortedByDescending { it.anio }

            HomeUiState(
                isLoading = false,
                groupedContent = grouped,
                seriesList = grouped.filter { it.esSerie },
                moviesList = grouped.filter { !it.esSerie },

                featuredContent = if (query.isNotEmpty() || genre != "Todos") grouped.firstOrNull() else allPeliculas.firstOrNull(),
                searchQuery = query,
                selectedGenre = genre,
                availableGenres = genres
            )
        }
    }.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    // Eventos de la UI
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onGenreSelected(genre: String) { _selectedGenre.value = genre }
}