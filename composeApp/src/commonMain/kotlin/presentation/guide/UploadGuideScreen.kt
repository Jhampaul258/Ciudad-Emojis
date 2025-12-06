package presentation.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import domain.model.Pelicula
import kotlinx.coroutines.launch
import presentation.components.LoadingButton
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import presentation.detail.PeliculaDetailScreen // <--- Importante

data class UploadGuideScreen(val peliculaToEdit: Pelicula? = null) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { UploadViewModel(peliculaToEdit) }
        val state by viewModel.uiState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()

        // --- EFECTO DE NAVEGACIÓN (CAMBIO CLAVE) ---
        LaunchedEffect(state.successPelicula) {
            state.successPelicula?.let { peliculaGuardada ->
                // 1. Mostramos mensaje
                snackbarController.showSuccess("Contenido guardado correctamente")

                // 2. Navegamos al detalle de la película
                // Usamos 'replace' para que al volver atrás no regresemos al formulario de subida, sino a la lista anterior.
                navigator.replace(PeliculaDetailScreen(peliculaGuardada))

                // 3. Reseteamos el estado (por si acaso volvemos a esta pantalla)
                viewModel.resetSuccessState()
            }
        }

        LaunchedEffect(state.error) {
            if (state.error != null) {
                snackbarController.showError(state.error!!)
                viewModel.clearError()
            }
        }

        // ... (El resto del Scaffold y UI se mantiene IDÉNTICO a tu versión anterior) ...
        // Solo asegúrate de que el botón use viewModel.submitContent()

        val genresList = listOf("Acción", "Aventura", "Comedia", "Drama", "Terror", "Ciencia Ficción", "Fantasía", "Musical", "Suspenso", "Romance", "Documental", "Animación", "Crimen", "Misterio")
        var isGenreExpanded by remember { mutableStateOf(false) }
        var isSeriesDropdownExpanded by remember { mutableStateOf(false) }

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
            if (state.isLoading && state.directorId.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = state.videoUrl,
                        onValueChange = { viewModel.onVideoUrlChange(it) },
                        label = { Text("URL del Video (YouTube)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        singleLine = true,
                        enabled = !state.isLoading
                    )
                    Spacer(Modifier.height(24.dp))

                    OutlinedCard(modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (state.thumbnailPreview.isNotEmpty()) {
                                AsyncImage(model = state.thumbnailPreview, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.Gray)
                                    Text("Vista previa", color = Color.Gray)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("¿Es un episodio de Serie?", style = MaterialTheme.typography.titleMedium)
                            Text("Agrupar capítulos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = state.esSerie, onCheckedChange = { viewModel.onEsSerieChange(it) }, enabled = !state.isLoading)
                    }
                    Spacer(Modifier.height(16.dp))

                    if (state.esSerie) {
                        ExposedDropdownMenuBox(expanded = isSeriesDropdownExpanded, onExpandedChange = { isSeriesDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = state.nombreSerie,
                                onValueChange = { viewModel.onNombreSerieChange(it); isSeriesDropdownExpanded = true },
                                label = { Text("Nombre de la Serie") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,

                                trailingIcon = { Icon(if (isSeriesDropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null) }
                            )
                            if (state.existingSeries.isNotEmpty()) {
                                ExposedDropdownMenu(expanded = isSeriesDropdownExpanded, onDismissRequest = { isSeriesDropdownExpanded = false }) {
                                    state.existingSeries.filter { it.contains(state.nombreSerie, ignoreCase = true) }.forEach { serieName ->
                                        DropdownMenuItem(text = { Text(serieName) }, onClick = { viewModel.onNombreSerieChange(serieName); isSeriesDropdownExpanded = false })
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = state.numeroCapitulo,
                            onValueChange = { viewModel.onNumeroCapituloChange(it) },
                            label = { Text("N° Capítulo") },
                            singleLine = true,

                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = state.titulo,
                        onValueChange = { viewModel.onTituloChange(it) },
                        singleLine = true,

                        label = { Text(if (state.esSerie) "Nombre del Episodio" else "Título de la Película") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.anio,
                            onValueChange = { viewModel.onAnioChange(it) },
                            label = { Text("Año") },
                            singleLine = true,

                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        ExposedDropdownMenuBox(expanded = isGenreExpanded, onExpandedChange = { isGenreExpanded = !isGenreExpanded }, modifier = Modifier.weight(2f)) {
                            OutlinedTextField(
                                value = state.genero,
                                onValueChange = {}, readOnly = true,
                                label = { Text("Género") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenreExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = isGenreExpanded, onDismissRequest = { isGenreExpanded = false }) {
                                genresList.forEach { selection ->
                                    DropdownMenuItem(text = { Text(selection) }, onClick = { viewModel.onGeneroChange(selection); isGenreExpanded = false })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.sinopsis,
                        onValueChange = { viewModel.onSinopsisChange(it) },
                        label = { Text("Sinopsis") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        singleLine = true,

                        )
                    Spacer(Modifier.height(32.dp))

                    LoadingButton(
                        onClick = { viewModel.submitContent() },
                        text = if (peliculaToEdit != null) "Guardar Cambios" else "Publicar",
                        isLoading = state.isLoading
                    )
                }
            }
        }
    }
}