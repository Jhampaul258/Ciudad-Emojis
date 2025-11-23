package presentation.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

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
import utils.getYoutubeThumbnail
import utils.translateError

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

        // Género
        var genero by remember { mutableStateOf(peliculaToEdit?.genero ?: "") }
        var isGenreExpanded by remember { mutableStateOf(false) }
        val genresList = listOf("Acción", "Aventura", "Comedia", "Drama", "Terror", "Ciencia Ficción", "Fantasía", "Musical", "Suspenso", "Romance", "Documental", "Animación", "Crimen", "Misterio")

        var sinopsis by remember { mutableStateOf(peliculaToEdit?.sinopsis ?: "") }
        var videoUrl by remember { mutableStateOf(peliculaToEdit?.videoUrl ?: "") }

        // --- ESTADOS PARA SERIES ---
        var esSerie by remember { mutableStateOf(peliculaToEdit?.esSerie ?: false) }
        var nombreSerie by remember { mutableStateOf(peliculaToEdit?.nombreSerie ?: "") }
        var numeroCapitulo by remember { mutableStateOf(peliculaToEdit?.numeroCapitulo?.toString() ?: "1") }

        // Lista de series existentes del director (para el autocompletado)
        var existingSeriesNames by remember { mutableStateOf<List<String>>(emptyList()) }
        var isSeriesDropdownExpanded by remember { mutableStateOf(false) }

        val dynamicThumbnailUrl = remember(videoUrl) { getYoutubeThumbnail(videoUrl) }

        var isLoading by remember { mutableStateOf(false) }
        var directorInfo by remember { mutableStateOf<Director?>(null) }
        var isTituloError by remember { mutableStateOf(false) }

        // Cargar datos
        LaunchedEffect(Unit) {
            val directorId = authRepository.getCurrentUserId()
            if (directorId != null) {
                // 1. Cargar info del director
                directorRepository.getDirector(directorId).collect { directorInfo = it }

                // 2. Cargar sus películas para encontrar nombres de series existentes
                peliculaRepository.getPeliculasByDirector(directorId).collect { peliculas ->
                    existingSeriesNames = peliculas
                        .filter { it.esSerie && it.nombreSerie.isNotBlank() }
                        .map { it.nombreSerie }
                        .distinct() // Eliminar duplicados
                        .sorted()
                }
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
                    // --- URL VIDEO ---
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("URL del Video (YouTube)") },
                        placeholder = { Text("https://youtube.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(24.dp))

                    // --- CARÁTULA ---
                    OutlinedCard(modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (dynamicThumbnailUrl.isNotEmpty()) {
                                AsyncImage(model = dynamicThumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Gray)
                                    Text("Vista previa", color = Color.Gray)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // --- SWITCH SERIE ---
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("¿Es un episodio de Serie?", style = MaterialTheme.typography.titleMedium)
                            Text("Activa esto para agrupar capítulos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = esSerie, onCheckedChange = { esSerie = it })
                    }
                    Spacer(Modifier.height(16.dp))

                    // --- CAMPOS ESPECÍFICOS DE SERIE ---
                    if (esSerie) {
                        // 1. Nombre de la Serie (Autocomplete)
                        ExposedDropdownMenuBox(
                            expanded = isSeriesDropdownExpanded,
                            onExpandedChange = { isSeriesDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = nombreSerie,
                                onValueChange = {
                                    nombreSerie = it
                                    isSeriesDropdownExpanded = true
                                },
                                label = { Text("Nombre de la Serie") },
                                placeholder = { Text("Ej: Stranger Things") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = {
                                    Icon(if (isSeriesDropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                                }
                            )

                            // Mostrar sugerencias solo si hay coincidencias
                            if (existingSeriesNames.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = isSeriesDropdownExpanded,
                                    onDismissRequest = { isSeriesDropdownExpanded = false }
                                ) {
                                    existingSeriesNames.filter { it.contains(nombreSerie, ignoreCase = true) }.forEach { serieName ->
                                        DropdownMenuItem(
                                            text = { Text(serieName) },
                                            onClick = {
                                                nombreSerie = serieName
                                                isSeriesDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // 2. Número de Capítulo
                        OutlinedTextField(
                            value = numeroCapitulo,
                            onValueChange = { if (it.all { char -> char.isDigit() }) numeroCapitulo = it },
                            label = { Text("Número de Capítulo") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                    }

                    // --- TÍTULO (Del capítulo o película) ---
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it; isTituloError = false },
                        label = { Text(if (esSerie) "Nombre del Episodio" else "Título de la Película") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = isTituloError
                    )
                    Spacer(Modifier.height(16.dp))

                    // --- AÑO y GÉNERO ---
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = anio,
                            onValueChange = { anio = it },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        ExposedDropdownMenuBox(
                            expanded = isGenreExpanded,
                            onExpandedChange = { isGenreExpanded = !isGenreExpanded },
                            modifier = Modifier.weight(2f)
                        ) {
                            OutlinedTextField(
                                value = genero,
                                onValueChange = {}, readOnly = true,
                                label = { Text("Género") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenreExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = isGenreExpanded, onDismissRequest = { isGenreExpanded = false }) {
                                genresList.forEach { selection ->
                                    DropdownMenuItem(text = { Text(selection) }, onClick = { genero = selection; isGenreExpanded = false })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = sinopsis,
                        onValueChange = { sinopsis = it },
                        label = { Text("Sinopsis") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                    Spacer(Modifier.height(32.dp))

                    // --- BOTÓN GUARDAR ---
                    LoadingButton(
                        onClick = {
                            if (titulo.isBlank() || videoUrl.isBlank() || genero.isBlank()) {
                                isTituloError = titulo.isBlank()
                                scope.launch { snackbarController.showError("Completa los campos obligatorios") }
                                return@LoadingButton
                            }
                            if (esSerie && (nombreSerie.isBlank() || numeroCapitulo.isBlank())) {
                                scope.launch { snackbarController.showError("Indica el nombre de la serie y el n° de capítulo") }
                                return@LoadingButton
                            }

                            scope.launch {
                                isLoading = true
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
                                    caratulaUrl = finalCaratulaUrl,
                                    esSerie = esSerie,
                                    nombreSerie = if (esSerie) nombreSerie.trim() else "", // Nuevo campo
                                    numeroCapitulo = if (esSerie) numeroCapitulo.toIntOrNull() ?: 1 else 0 // Nuevo campo
                                )

                                val result = if (peliculaToEdit != null) peliculaRepository.updatePelicula(peliculaToSave) else peliculaRepository.createPelicula(peliculaToSave)

                                if (result.isSuccess) {
                                    snackbarController.showSuccess("Guardado con éxito")
                                    if (peliculaToEdit == null) { // Reset
                                        titulo = ""; videoUrl = ""; sinopsis = ""; nombreSerie = ""; numeroCapitulo = "1"
                                    } else navigator.pop()
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


}