package presentation.guide
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import data.DirectorRepository
import data.FirebaseAuthRepository
import data.PeliculaRepository
import domain.model.Director
import domain.model.Pelicula
import kotlinx.coroutines.launch
import presentation.components.LoadingButton
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import utils.translateError
// Importación necesaria para KeyboardOptions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add

object UploadGuideScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        // --- Repositorios y Estado ---
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()

        val authRepository = remember { FirebaseAuthRepository() }
        val directorRepository = remember { DirectorRepository() }
        val peliculaRepository = remember { PeliculaRepository() }

        // --- Estado del formulario ---
        var titulo by remember { mutableStateOf("") }
        var anio by remember { mutableStateOf("") }
        var genero by remember { mutableStateOf("") }
        var sinopsis by remember { mutableStateOf("") }
        var videoUrl by remember { mutableStateOf("") }
        var esSerie by remember { mutableStateOf(false) }

        var caratulaUrl by remember { mutableStateOf("") }
        var newCaratulaBytes by remember { mutableStateOf<ByteArray?>(null) }

        var isLoading by remember { mutableStateOf(false) }
        var directorInfo by remember { mutableStateOf<Director?>(null) }

        // --- Estados de Validación ---
        var isTituloError by remember { mutableStateOf(false) }
        var isAnioError by remember { mutableStateOf(false) }
        var isGeneroError by remember { mutableStateOf(false) }
        var isSinopsisError by remember { mutableStateOf(false) }
        var isVideoUrlError by remember { mutableStateOf(false) }

        // Cargar datos del director (ID y Nombre)
        LaunchedEffect(Unit) {
            val directorId = authRepository.getCurrentUserId()
            if (directorId != null) {
                directorRepository.getDirector(directorId).collect {
                    directorInfo = it
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Subir Película o Serie") })
            },
            snackbarHost = { ReusableSnackbarHost(controller = snackbarController) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (directorInfo == null) {
                    CircularProgressIndicator()
                    Text("Cargando información de director...", modifier = Modifier.padding(top = 8.dp))
                } else {
                    // --- Selector de Carátula (Placeholder) ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (newCaratulaBytes != null) {
                            Text("Nueva carátula seleccionada")
                        } else {
                            Icon(Icons.Default.Add, "Subir carátula", modifier = Modifier.size(50.dp))
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch { snackbarController.showError("Selector de imagen no implementado.") }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Seleccionar Carátula")
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Campos de Texto con Validación ---
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = {
                            titulo = it
                            isTituloError = false // Resetea el error al escribir
                        },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isTituloError, // Muestra el estado de error
                        supportingText = { // Mensaje de ayuda si hay error
                            if (isTituloError) Text("El título es obligatorio")
                        }
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = anio,
                            onValueChange = {
                                anio = it
                                isAnioError = false
                            },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isAnioError,
                            supportingText = {
                                if (isAnioError) Text("Requerido")
                            }
                        )
                        OutlinedTextField(
                            value = genero,
                            onValueChange = {
                                genero = it
                                isGeneroError = false
                            },
                            label = { Text("Género") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            isError = isGeneroError,
                            supportingText = {
                                if (isGeneroError) Text("Requerido")
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = sinopsis,
                        onValueChange = {
                            sinopsis = it
                            isSinopsisError = false
                        },
                        label = { Text("Sinopsis") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        isError = isSinopsisError,
                        supportingText = {
                            if (isSinopsisError) Text("La sinopsis es obligatoria")
                        }
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = {
                            videoUrl = it
                            isVideoUrlError = false
                        },
                        label = { Text("URL del Video (YouTube, Vimeo...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isVideoUrlError,
                        supportingText = {
                            if (isVideoUrlError) Text("La URL del video es obligatoria")
                        }
                    )
                    Spacer(Modifier.height(16.dp))

                    // --- Switch para "esSerie" ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("¿Es una serie?", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = esSerie,
                            onCheckedChange = { esSerie = it }
                        )
                    }

                    if (esSerie) {
                        Text(
                            "La subida de episodios se habilitará pronto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // --- Botón de Guardar con Lógica de Validación ---
                    LoadingButton(
                        onClick = {
                            // --- INICIO DE LA VALIDACIÓN ---
                            isTituloError = titulo.isBlank()
                            isAnioError = anio.isBlank()
                            isGeneroError = genero.isBlank()
                            isSinopsisError = sinopsis.isBlank()
                            isVideoUrlError = videoUrl.isBlank()

                            val hasError = isTituloError || isAnioError || isGeneroError || isSinopsisError || isVideoUrlError

                            if (hasError) {
                                scope.launch {
                                    snackbarController.showError("Por favor, revisa los campos marcados en rojo.")
                                }
                                return@LoadingButton // Detiene la ejecución aquí
                            }
                            // --- FIN DE LA VALIDACIÓN ---

                            // Si pasa la validación, continúa con el guardado...
                            scope.launch {
                                isLoading = true
                                var finalCaratulaUrl = ""

                                if (newCaratulaBytes != null) {
                                    // TODO: Lógica de subida de imagen
                                    snackbarController.showError("La subida de carátula no está implementada.")
                                    isLoading = false
                                    return@launch
                                }

                                val nuevaPelicula = Pelicula(
                                    directorId = directorInfo!!.uid,
                                    directorName = directorInfo!!.name,
                                    titulo = titulo,
                                    anio = anio.toIntOrNull() ?: 2024,
                                    genero = genero,
                                    sinopsis = sinopsis,
                                    videoUrl = videoUrl,
                                    caratulaUrl = finalCaratulaUrl,
                                    esSerie = esSerie
                                )

                                val result = peliculaRepository.createPelicula(nuevaPelicula)
                                if (result.isSuccess) {
                                    snackbarController.showSuccess("¡Película subida con éxito!")
                                    // Limpiar formulario
                                    titulo = ""
                                    anio = ""
                                    genero = ""
                                    sinopsis = ""
                                    videoUrl = ""
                                    esSerie = false
                                    newCaratulaBytes = null
                                } else {
                                    val error = translateError(result.exceptionOrNull()?.message)
                                    snackbarController.showError(error)
                                }
                                isLoading = false
                            }
                        },
                        text = "Subir Película",
                        isLoading = isLoading,
//                        enabled = !isLoading && directorInfo != null
                    )
                }
            }
        }
    }
}