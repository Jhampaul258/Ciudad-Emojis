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

    // Contenido filtrado
    val groupedContent: List<Pelicula> = emptyList(),
    val seriesList: List<Pelicula> = emptyList(),
    val moviesList: List<Pelicula> = emptyList(),
    val featuredContent: Pelicula? = null, // La destacada (puede cambiar según filtro)

    // Estados de los filtros
    val searchQuery: String = "",
    val selectedGenre: String = "Todos",
    val availableGenres: List<String> = emptyList() // Lista dinámica de géneros existentes
)

class HomeViewModel(
    private val peliculaRepository: PeliculaRepository = PeliculaRepository()
) : ScreenModel {

    // Estados mutables para los inputs del usuario
    private val _searchQuery = MutableStateFlow("")
    private val _selectedGenre = MutableStateFlow("Todos")

    // Combinamos: Base de Datos + Búsqueda + Género
    val state: StateFlow<HomeUiState> = combine(
        peliculaRepository.getAllPeliculasStream(),
        _searchQuery,
        _selectedGenre
    ) { allPeliculas, query, genre ->

        if (allPeliculas.isEmpty()) {
            HomeUiState(isLoading = false)
        } else {
            // 1. Calcular géneros disponibles dinámicamente
            val genres = listOf("Todos") + allPeliculas.map { it.genero }.distinct().sorted()

            // 2. Aplicar Filtros
            val filteredList = allPeliculas.filter { pelicula ->
                // Filtro por Texto (Título o Director)
                val matchesSearch = pelicula.titulo.contains(query, ignoreCase = true) ||
                        pelicula.directorName.contains(query, ignoreCase = true)||
                        (pelicula.esSerie && pelicula.nombreSerie.contains(query, ignoreCase = true)) // <--- ¡ESTO FALTABA!
                // Filtro por Género
                val matchesGenre = genre == "Todos" || pelicula.genero.equals(genre, ignoreCase = true)

                matchesSearch && matchesGenre
            }

            // 3. Agrupar Series (misma lógica que antes, pero aplicada a la lista filtrada)
            val grouped = filteredList
                .groupBy { if (it.esSerie && it.nombreSerie.isNotBlank()) it.nombreSerie else it.id }
                .map { (_, videos) ->
                    // Elegir el representante (Cap 1 para series, o el único si es peli)
                    if (videos.first().esSerie) {
                        videos.minByOrNull { it.numeroCapitulo } ?: videos.first()
                    } else {
                        videos.first()
                    }
                }
                .sortedByDescending { it.anio }

            // 4. Construir el estado final
            HomeUiState(
                isLoading = false,
                groupedContent = grouped,
                seriesList = grouped.filter { it.esSerie },
                moviesList = grouped.filter { !it.esSerie },
                // Si hay búsqueda/filtro, la destacada es el primer resultado.
                // Si no, es la más reciente global.
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