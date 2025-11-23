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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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

data class UploadGuideScreen(val peliculaToEdit: Pelicula? = null) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        // 1. Inyectamos el ViewModel con el parámetro opcional
        val viewModel = rememberScreenModel { UploadViewModel(peliculaToEdit) }

        // 2. Observamos el estado
        val state by viewModel.uiState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()

        // Listas fijas para UI
        val genresList = listOf("Acción", "Aventura", "Comedia", "Drama", "Terror", "Ciencia Ficción", "Fantasía", "Musical", "Suspenso", "Romance", "Documental", "Animación", "Crimen", "Misterio")

        // Estados UI efímeros (solo visuales, no de negocio)
        var isGenreExpanded by remember { mutableStateOf(false) }
        var isSeriesDropdownExpanded by remember { mutableStateOf(false) }

        // Reacción a eventos del ViewModel (Éxito/Error)
        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) {
                snackbarController.showSuccess(if (peliculaToEdit != null) "Actualizado con éxito" else "Publicado con éxito")
                if (peliculaToEdit != null) navigator.pop() // Volver si es edición
                // Si es nuevo, el formulario se queda limpio (podrías resetearlo en VM si quisieras)
            }
        }

        LaunchedEffect(state.error) {
            if (state.error != null) {
                snackbarController.showError(state.error!!)
                viewModel.clearError()
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

            if (state.isLoading && state.directorId.isEmpty()) {
                // Carga inicial
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

                    // --- URL VIDEO ---
                    OutlinedTextField(
                        value = state.videoUrl,
                        onValueChange = { viewModel.onVideoUrlChange(it) },
                        label = { Text("URL del Video (YouTube)") },
                        placeholder = { Text("https://youtube.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Build, null) },
                        singleLine = true,
                        enabled = !state.isLoading
                    )
                    Spacer(Modifier.height(24.dp))

                    // --- PREVIEW CARÁTULA ---
                    OutlinedCard(modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (state.thumbnailPreview.isNotEmpty()) {
                                AsyncImage(model = state.thumbnailPreview, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, null, tint = Color.Gray)
                                    Text("Vista previa automática", color = Color.Gray)
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
                            Text("Agrupar capítulos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = state.esSerie, onCheckedChange = { viewModel.onEsSerieChange(it) }, enabled = !state.isLoading)
                    }
                    Spacer(Modifier.height(16.dp))

                    // --- CAMPOS SERIE ---
                    if (state.esSerie) {
                        // Nombre Serie (Autocomplete)
                        ExposedDropdownMenuBox(
                            expanded = isSeriesDropdownExpanded,
                            onExpandedChange = { isSeriesDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = state.nombreSerie,
                                onValueChange = {
                                    viewModel.onNombreSerieChange(it)
                                    isSeriesDropdownExpanded = true
                                },
                                label = { Text("Nombre de la Serie") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { Icon(if (isSeriesDropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null) }
                            )

                            if (state.existingSeries.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = isSeriesDropdownExpanded,
                                    onDismissRequest = { isSeriesDropdownExpanded = false }
                                ) {
                                    state.existingSeries.filter { it.contains(state.nombreSerie, ignoreCase = true) }.forEach { serieName ->
                                        DropdownMenuItem(
                                            text = { Text(serieName) },
                                            onClick = {
                                                viewModel.onNombreSerieChange(serieName)
                                                isSeriesDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = state.numeroCapitulo,
                            onValueChange = { viewModel.onNumeroCapituloChange(it) },
                            label = { Text("N° Capítulo") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                    }

                    // --- TÍTULO ---
                    OutlinedTextField(
                        value = state.titulo,
                        onValueChange = { viewModel.onTituloChange(it) },
                        label = { Text(if (state.esSerie) "Nombre del Episodio" else "Título de la Película") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    // --- AÑO Y GÉNERO ---
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.anio,
                            onValueChange = { viewModel.onAnioChange(it) },
                            label = { Text("Año") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Género Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isGenreExpanded,
                            onExpandedChange = { isGenreExpanded = !isGenreExpanded },
                            modifier = Modifier.weight(2f)
                        ) {
                            OutlinedTextField(
                                value = state.genero,
                                onValueChange = {}, readOnly = true,
                                label = { Text("Género") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenreExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = isGenreExpanded, onDismissRequest = { isGenreExpanded = false }) {
                                genresList.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection) },
                                        onClick = { viewModel.onGeneroChange(selection); isGenreExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // --- SINOPSIS ---
                    OutlinedTextField(
                        value = state.sinopsis,
                        onValueChange = { viewModel.onSinopsisChange(it) },
                        label = { Text("Sinopsis") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                    Spacer(Modifier.height(32.dp))

                    // --- BOTÓN ACCIÓN ---
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