package presentation.profile_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import domain.model.Pelicula
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import presentation.guide.UploadGuideScreen
import kotlinx.coroutines.launch
import utils.getYoutubeThumbnail

class DirectorContentScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        // 1. Inyectamos ViewModel
        val viewModel = rememberScreenModel { DirectorContentViewModel() }
        val state by viewModel.uiState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()

        // Estado para Tabs
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Películas", "Series")

        // Estado para el Diálogo de Eliminación
        var peliculaToDelete by remember { mutableStateOf<Pelicula?>(null) }

        // Manejo de Mensajes (Éxito/Error)
        LaunchedEffect(state.successMessage) {
            state.successMessage?.let {
                snackbarController.showSuccess(it)
                viewModel.clearMessages()
            }
        }
        LaunchedEffect(state.error) {
            state.error?.let {
                snackbarController.showError(it)
                viewModel.clearMessages()
            }
        }

        // --- DIÁLOGO DE CONFIRMACIÓN ---
        if (peliculaToDelete != null) {
            AlertDialog(
                onDismissRequest = { peliculaToDelete = null },
                title = { Text("¿Eliminar contenido?") },
                text = { Text("¿Estás seguro de que quieres eliminar '${peliculaToDelete?.titulo}'? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePelicula(peliculaToDelete!!)
                            peliculaToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { peliculaToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mi Contenido") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            },
            snackbarHost = { ReusableSnackbarHost(snackbarController) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {

                // Tabs
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            icon = { Icon(if (index == 0) Icons.Default.Build else Icons.Default.PlayArrow, null) }
                        )
                    }
                }

                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    // Filtrar contenido
                    val filteredContent = state.myContent.filter {
                        if (selectedTabIndex == 0) !it.esSerie else it.esSerie
                    }

                    if (filteredContent.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No has subido ${tabs[selectedTabIndex].lowercase()} aún.", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredContent) { item ->
                                DirectorContentItem(
                                    pelicula = item,
                                    onEditClick = { navigator.push(UploadGuideScreen(peliculaToEdit = item)) },
                                    onDeleteClick = { peliculaToDelete = item } // Activamos el diálogo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectorContentItem(
    pelicula: Pelicula,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit // Nuevo callback
) {
    val thumbnailUrl = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable { onEditClick() }
        ) {
            // Imagen
            Box(
                modifier = Modifier.width(140.dp).fillMaxHeight().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = thumbnailUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                }
            }

            // Textos
            Column(
                modifier = Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = pelicula.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                // Mostrar Info extra si es serie
                val extraInfo = if(pelicula.esSerie) " • ${pelicula.nombreSerie}" else ""

                Text(
                    text = "${pelicula.anio} • ${pelicula.genero}$extraInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botones de Acción (Columna vertical para Edit y Delete)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

