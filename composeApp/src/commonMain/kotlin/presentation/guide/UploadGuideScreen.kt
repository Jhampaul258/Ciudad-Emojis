package presentation.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Link
//import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import utils.getYoutubeThumbnail

data class UploadGuideScreen(val peliculaToEdit: Pelicula? = null) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()

        // --- Repositorios ---
        val authRepository = remember { FirebaseAuthRepository() }
        val directorRepository = remember { DirectorRepository() }
        val peliculaRepository = remember { PeliculaRepository() }

        // --- Estado del formulario ---
        var titulo by remember { mutableStateOf(peliculaToEdit?.titulo ?: "") }
        var anio by remember { mutableStateOf(peliculaToEdit?.anio?.toString() ?: "") }
        var genero by remember { mutableStateOf(peliculaToEdit?.genero ?: "") }
        var sinopsis by remember { mutableStateOf(peliculaToEdit?.sinopsis ?: "") }

        // El campo más importante ahora:
        var videoUrl by remember { mutableStateOf(peliculaToEdit?.videoUrl ?: "") }

        var esSerie by remember { mutableStateOf(peliculaToEdit?.esSerie ?: false) }

        // Calculamos la miniatura dinámicamente basada en la URL del video
        val dynamicThumbnailUrl = remember(videoUrl) { getYoutubeThumbnail(videoUrl) }

        var isLoading by remember { mutableStateOf(false) }
        var directorInfo by remember { mutableStateOf<Director?>(null) }
        var isTituloError by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val directorId = authRepository.getCurrentUserId()
            if (directorId != null) {
                directorRepository.getDirector(directorId).collect { directorInfo = it }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (peliculaToEdit != null) "Editar Contenido" else "Subir Contenido") },
                    navigationIcon = {
                        if (peliculaToEdit != null) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    }
                )
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
                } else {
                    // --- CAMPO URL DE VIDEO (Ahora va primero porque define la imagen) ---
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("URL del Video (YouTube)") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))

                    // --- VISTA PREVIA DE LA CARÁTULA (Automática) ---
                    Text("Vista previa de Carátula", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))

                    OutlinedCard(
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (dynamicThumbnailUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = dynamicThumbnailUrl,
                                    contentDescription = "Carátula de YouTube",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Capa oscura pequeña para que se lea el texto si la imagen es clara
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Pega un link de YouTube para ver la imagen", color = Color.Gray)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Resto del Formulario ---
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it; isTituloError = false },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = isTituloError
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = anio,
                            onValueChange = { anio = it },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = genero,
                            onValueChange = { genero = it },
                            label = { Text("Género") },
                            modifier = Modifier.weight(2f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = sinopsis,
                        onValueChange = { sinopsis = it },
                        label = { Text("Sinopsis") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("¿Es una serie?", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = esSerie, onCheckedChange = { esSerie = it })
                    }

                    Spacer(Modifier.height(32.dp))

                    // --- Botón Guardar ---
                    LoadingButton(
                        onClick = {
                            if (titulo.isBlank() || videoUrl.isBlank()) {
                                scope.launch { snackbarController.showError("Título y Video son obligatorios") }
                                isTituloError = titulo.isBlank()
                                return@LoadingButton
                            }

                            scope.launch {
                                isLoading = true

                                // Usamos la URL generada automáticamente
                                val finalCaratulaUrl = getYoutubeThumbnail(videoUrl)

                                val peliculaToSave = Pelicula(
                                    id = peliculaToEdit?.id ?: "",
                                    directorId = directorInfo!!.uid,
                                    directorName = directorInfo!!.name,
                                    titulo = titulo,
                                    anio = anio.toIntOrNull() ?: 2024,
                                    genero = genero,
                                    sinopsis = sinopsis,
                                    videoUrl = videoUrl,
                                    caratulaUrl = finalCaratulaUrl, // Guardamos la URL de img.youtube.com
                                    esSerie = esSerie
                                )

                                val result = if (peliculaToEdit != null) {
                                    peliculaRepository.updatePelicula(peliculaToSave)
                                } else {
                                    peliculaRepository.createPelicula(peliculaToSave)
                                }

                                if (result.isSuccess) {
                                    snackbarController.showSuccess(if (peliculaToEdit != null) "Actualizado con éxito" else "Subido con éxito")
                                    if (peliculaToEdit == null) {
                                        // Limpiar formulario
                                        titulo = ""; anio = ""; genero = ""; sinopsis = ""; videoUrl = ""
                                    } else {
                                        navigator.pop()
                                    }
                                } else {
                                    snackbarController.showError(translateError(result.exceptionOrNull()?.message))
                                }
                                isLoading = false
                            }
                        },
                        text = if (peliculaToEdit != null) "Guardar Cambios" else "Publicar",
                        isLoading = isLoading
                    )
                }
            }
        }
    }

    /**
     * Extrae el ID y construye la URL de la miniatura de alta calidad.
     */

}