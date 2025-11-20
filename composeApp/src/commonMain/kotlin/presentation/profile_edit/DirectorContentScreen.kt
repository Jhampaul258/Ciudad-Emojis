package presentation.profile_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import data.FirebaseAuthRepository
import data.PeliculaRepository
import domain.model.Pelicula
import presentation.guide.UploadGuideScreen
import utils.getYoutubeThumbnail

class DirectorContentScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = remember { FirebaseAuthRepository() }
        val peliculaRepository = remember { PeliculaRepository() }

        var myContent by remember { mutableStateOf<List<Pelicula>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        // Estado para las pestañas (0 = Películas, 1 = Series)
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Películas", "Series")

        LaunchedEffect(Unit) {
            val directorId = authRepository.getCurrentUserId()
            if (directorId != null) {
                peliculaRepository.getPeliculasByDirector(directorId).collect {
                    myContent = it
                    isLoading = false
                }
            } else {
                isLoading = false
            }
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
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {

                // --- Pestañas ---
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            icon = {
                                Icon(if (index == 0) Icons.Default.Info else Icons.Default.Face, null)
                            }
                        )
                    }
                }

                // --- Lista Filtrada ---
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Filtramos según la pestaña seleccionada
                    val filteredContent = myContent.filter {
                        if (selectedTabIndex == 0) !it.esSerie else it.esSerie
                    }

                    if (filteredContent.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Build, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No has subido ${tabs[selectedTabIndex].lowercase()} aún.",
                                    color = Color.Gray
                                )
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
                                    onEditClick = {
                                        // Navegamos a la pantalla de edición con los datos
                                        navigator.push(UploadGuideScreen(peliculaToEdit = item))
                                    }
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
fun DirectorContentItem(pelicula: Pelicula, onEditClick: () -> Unit) {
    // Calculamos la miniatura en vivo basándonos en el link del video
    val thumbnailUrl = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) // Altura fija para la tarjeta
                .clickable { onEditClick() }
        ) {
            // --- Imagen Miniatura (Desde YouTube) ---
            Box(
                modifier = Modifier
                    .width(140.dp) // Más ancho para formato 16:9 de YouTube
                    .fillMaxHeight()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Si no hay link válido, mostramos icono
                    Icon(Icons.Default.Build, null, tint = Color.White)
                }
            }

            // --- Textos ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
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
                Text(
                    text = "${pelicula.anio} • ${pelicula.genero}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // --- Botón Editar ---
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Función auxiliar para obtener la URL de la miniatura de YouTube.
 * (Duplicada aquí para asegurar que la lista funcione independiente)
 */
