package presentation.guide

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.DirectorRepository
import data.FirebaseAuthRepository
import data.PeliculaRepository
import domain.model.Pelicula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import utils.getYoutubeThumbnail

// Estado único de la UI (Single Source of Truth)
data class UploadUiState(
    // Campos del formulario
    val titulo: String = "",
    val anio: String = "",
    val genero: String = "",
    val sinopsis: String = "",
    val videoUrl: String = "",
    val esSerie: Boolean = false,
    val nombreSerie: String = "",
    val numeroCapitulo: String = "1",

    // Estado derivado
    val thumbnailPreview: String = "",

    // Listas y datos de control
    val existingSeries: List<String> = emptyList(), // Para el autocomplete
    val directorId: String = "",
    val directorName: String = "",

    // Estados de carga/error
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class UploadViewModel(
    private val peliculaToEdit: Pelicula?, // Null si es nuevo, Objeto si es edición
    private val peliculaRepository: PeliculaRepository = PeliculaRepository(),
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository(),
    private val directorRepository: DirectorRepository = DirectorRepository()
) : ScreenModel {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Obtener usuario actual
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                // Cargar nombre del director
                directorRepository.getDirector(uid).collect { director ->
                    _uiState.update { it.copy(directorId = uid, directorName = director?.name ?: "") }
                }

                // Cargar lista de series para el autocomplete
                peliculaRepository.getPeliculasByDirector(uid).collect { peliculas ->
                    val series = peliculas
                        .filter { it.esSerie && it.nombreSerie.isNotBlank() }
                        .map { it.nombreSerie }
                        .distinct()
                        .sorted()
                    _uiState.update { it.copy(existingSeries = series) }
                }
            }

            // 2. Si estamos editando, rellenar el formulario
            if (peliculaToEdit != null) {
                _uiState.update { state ->
                    state.copy(
                        titulo = peliculaToEdit.titulo,
                        anio = peliculaToEdit.anio.toString(),
                        genero = peliculaToEdit.genero,
                        sinopsis = peliculaToEdit.sinopsis,
                        videoUrl = peliculaToEdit.videoUrl,
                        esSerie = peliculaToEdit.esSerie,
                        nombreSerie = peliculaToEdit.nombreSerie,
                        numeroCapitulo = peliculaToEdit.numeroCapitulo.toString(),
                        thumbnailPreview = getYoutubeThumbnail(peliculaToEdit.videoUrl)
                    )
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // --- Setters para la UI ---
    fun onTituloChange(value: String) { _uiState.update { it.copy(titulo = value) } }
    fun onAnioChange(value: String) { _uiState.update { it.copy(anio = value) } }
    fun onGeneroChange(value: String) { _uiState.update { it.copy(genero = value) } }
    fun onSinopsisChange(value: String) { _uiState.update { it.copy(sinopsis = value) } }

    fun onVideoUrlChange(value: String) {
        _uiState.update {
            it.copy(
                videoUrl = value,
                thumbnailPreview = getYoutubeThumbnail(value) // Actualizar preview automáticamente
            )
        }
    }

    fun onEsSerieChange(value: Boolean) { _uiState.update { it.copy(esSerie = value) } }
    fun onNombreSerieChange(value: String) { _uiState.update { it.copy(nombreSerie = value) } }
    fun onNumeroCapituloChange(value: String) {
        if (value.all { it.isDigit() }) _uiState.update { it.copy(numeroCapitulo = value) }
    }

    // --- Lógica de Guardado ---
    fun submitContent() {
        val state = _uiState.value

        // 1. Validaciones
        if (state.titulo.isBlank() || state.videoUrl.isBlank() || state.genero.isBlank()) {
            _uiState.update { it.copy(error = "Título, Video y Género son obligatorios") }
            return
        }
        if (state.esSerie && (state.nombreSerie.isBlank() || state.numeroCapitulo.isBlank())) {
            _uiState.update { it.copy(error = "Completa los datos de la serie") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 2. Construir objeto
            val pelicula = Pelicula(
                id = peliculaToEdit?.id ?: "", // Mantener ID si es edición
                directorId = state.directorId,
                directorName = state.directorName,
                titulo = state.titulo,
                anio = state.anio.toIntOrNull() ?: 2024,
                genero = state.genero,
                sinopsis = state.sinopsis,
                videoUrl = state.videoUrl,
                caratulaUrl = state.thumbnailPreview, // Usamos la generada de YouTube
                esSerie = state.esSerie,
                nombreSerie = if (state.esSerie) state.nombreSerie.trim() else "",
                numeroCapitulo = if (state.esSerie) state.numeroCapitulo.toIntOrNull() ?: 1 else 0
            )

            // 3. Guardar en Firebase
            val result = if (peliculaToEdit != null) {
                peliculaRepository.updatePelicula(pelicula)
            } else {
                peliculaRepository.createPelicula(pelicula)
            }

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Error desconocido") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}