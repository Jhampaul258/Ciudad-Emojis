package presentation.profile_edit

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import data.FirebaseAuthRepository
import data.PeliculaRepository
import domain.model.Pelicula
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DirectorContentUiState(
    val isLoading: Boolean = true,
    val myContent: List<Pelicula> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class DirectorContentViewModel(
    private val peliculaRepository: PeliculaRepository = PeliculaRepository(),
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository()
) : ScreenModel {

    private val _uiState = MutableStateFlow(DirectorContentUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                // Escuchamos en tiempo real, así si borramos, la lista se actualiza sola
                peliculaRepository.getPeliculasByDirector(userId).collect { content ->
                    _uiState.update { it.copy(myContent = content, isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deletePelicula(pelicula: Pelicula) {
        screenModelScope.launch {
            val result = peliculaRepository.deletePelicula(pelicula.id)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "Eliminado correctamente") }
            } else {
                _uiState.update { it.copy(error = "Error al eliminar: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}