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

data class UploadUiState(
    val titulo: String = "",
    val anio: String = "",
    val genero: String = "",
    val sinopsis: String = "",
    val videoUrl: String = "",
    val esSerie: Boolean = false,
    val nombreSerie: String = "",
    val numeroCapitulo: String = "1",
    val thumbnailPreview: String = "",

    val existingSeries: List<String> = emptyList(),
    val directorId: String = "",
    val directorName: String = "",

    val isLoading: Boolean = false,
    val error: String? = null,

    // CAMBIO: En lugar de un booleano, guardamos la película creada para poder navegar a ella
    val successPelicula: Pelicula? = null
)

class UploadViewModel(
    private val peliculaToEdit: Pelicula?,
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

            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                directorRepository.getDirector(uid).collect { director ->
                    _uiState.update { it.copy(directorId = uid, directorName = director?.name ?: "") }
                }

                peliculaRepository.getPeliculasByDirector(uid).collect { peliculas ->
                    val series = peliculas
                        .filter { it.esSerie && it.nombreSerie.isNotBlank() }
                        .map { it.nombreSerie }
                        .distinct()
                        .sorted()
                    _uiState.update { it.copy(existingSeries = series) }
                }
            }

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

    // --- Setters (Igual que antes) ---
    fun onTituloChange(v: String) { _uiState.update { it.copy(titulo = v) } }
    fun onAnioChange(v: String) { _uiState.update { it.copy(anio = v) } }
    fun onGeneroChange(v: String) { _uiState.update { it.copy(genero = v) } }
    fun onSinopsisChange(v: String) { _uiState.update { it.copy(sinopsis = v) } }
    fun onVideoUrlChange(v: String) { _uiState.update { it.copy(videoUrl = v, thumbnailPreview = getYoutubeThumbnail(v)) } }
    fun onEsSerieChange(v: Boolean) { _uiState.update { it.copy(esSerie = v) } }
    fun onNombreSerieChange(v: String) { _uiState.update { it.copy(nombreSerie = v) } }
    fun onNumeroCapituloChange(v: String) { if (v.all { it.isDigit() }) _uiState.update { it.copy(numeroCapitulo = v) } }

    fun submitContent() {
        val state = _uiState.value
        if (state.titulo.isBlank() || state.videoUrl.isBlank() || state.genero.isBlank()) {
            _uiState.update { it.copy(error = "Campos obligatorios vacíos") }
            return
        }
        // Validación de URL de YouTube (Regex)
        if (getYoutubeThumbnail(state.videoUrl).isEmpty()) {
            _uiState.update { it.copy(error = "El enlace no es un video válido de YouTube") }
            return
        }

        if (state.esSerie && (state.nombreSerie.isBlank() || state.numeroCapitulo.isBlank())) {
            _uiState.update { it.copy(error = "Completa el nombre de la serie y el capítulo") }
            return
        }

        // Validación de Año lógico
        val anioInt = state.anio.toIntOrNull()
        if (anioInt == null || anioInt < 1900 || anioInt > 2030) { // Ajusta el año según criterio
            _uiState.update { it.copy(error = "Ingresa un año válido (ej: 2024)") }
            return
        }
        if (state.sinopsis.length > 1000) {
            _uiState.update { it.copy(error = "La sinopsis es demasiado larga (máx 1000 caracteres)") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val shouldCheckUrl = peliculaToEdit == null || peliculaToEdit.videoUrl != state.videoUrl

            if (shouldCheckUrl) {
                val urlExists = peliculaRepository.existsVideoUrl(state.videoUrl)
                if (urlExists) {
                    _uiState.update { it.copy(isLoading = false, error = "Este video ya ha sido registrado previamente.") }
                    return@launch
                }
            }

            // B. Validar Capítulo Duplicado (Por Serie y Director)
            if (state.esSerie) {
                val capNumero = state.numeroCapitulo.toIntOrNull() ?: 0
                // Solo verificamos si cambiamos de capítulo/serie o si es nuevo
                val shouldCheckChapter = peliculaToEdit == null ||
                        peliculaToEdit.nombreSerie != state.nombreSerie ||
                        peliculaToEdit.numeroCapitulo != capNumero

                if (shouldCheckChapter) {
                    val chapterExists = peliculaRepository.existsChapter(
                        directorId = state.directorId,
                        nombreSerie = state.nombreSerie.trim(),
                        numeroCapitulo = capNumero
                    )

                    if (chapterExists) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "El capítulo $capNumero de '${state.nombreSerie}' ya existe.")
                        }
                        return@launch
                    }
                }
            }
            val pelicula = Pelicula(
                // ... tus datos ...
                id = peliculaToEdit?.id ?: "",
                directorId = state.directorId,
                directorName = state.directorName,
                titulo = state.titulo,
                anio = anioInt, // Usamos el int validado
                genero = state.genero,
                sinopsis = state.sinopsis,
                videoUrl = state.videoUrl,
                caratulaUrl = state.thumbnailPreview,
                esSerie = state.esSerie,
                nombreSerie = if (state.esSerie) state.nombreSerie.trim() else "",
                numeroCapitulo = if (state.esSerie) state.numeroCapitulo.toIntOrNull() ?: 1 else 0
            )

            val result = if (peliculaToEdit != null) {
                peliculaRepository.updatePelicula(pelicula)
            } else {
                peliculaRepository.createPelicula(pelicula)
            }

            if (result.isSuccess) {
                val savedPelicula = result.getOrThrow()
                // CAMBIO IMPORTANTE: Limpiamos campos y guardamos la película resultante para navegar
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successPelicula = savedPelicula, // Esto disparará la navegación
                        // Limpiar formulario
                        titulo = "", anio = "", genero = "", sinopsis = "", videoUrl = "", thumbnailPreview = ""
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun resetSuccessState() { _uiState.update { it.copy(successPelicula = null) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
}