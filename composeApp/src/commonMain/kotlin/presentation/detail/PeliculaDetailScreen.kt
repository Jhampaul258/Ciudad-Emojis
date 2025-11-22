package presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import domain.model.Pelicula
import presentation.components.YouTubePlayer
import utils.YouTubeUtils

//import presentation.components.VideoPlayer

// 1. Convertimos la clase en 'data class' y añadimos 'val pelicula: Pelicula'
data class PeliculaDetailScreen(val pelicula: Pelicula) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            pelicula.titulo,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    ,navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                val videoId = YouTubeUtils.extractVideoId(pelicula.videoUrl)
                // --- Reproductor de Video o Carátula ---
                if (videoId != null) {
                    // Usamos tu componente VideoPlayer existente
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                        YouTubePlayer(
                           videoId = videoId,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }else if (pelicula.videoUrl.isNotBlank()) {
                    // Si hay URL pero no es YouTube (ej: mp4 directo), usamos tu player anterior o un aviso
                    // Por ahora mostraremos la carátula con un icono de error o link
                    AsyncImage(
                        model = pelicula.caratulaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Si no hay video, mostramos la carátula grande
                    AsyncImage(
                        model = pelicula.caratulaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                // --- Información de la Película ---
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${pelicula.anio} • ${pelicula.genero}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (pelicula.esSerie) {
                            AssistChip(onClick = {}, label = { Text("Serie") })
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = pelicula.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Dirigido por: ${pelicula.directorName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(Modifier.padding(vertical = 16.dp))

                    Text(
                        text = "Sinopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = pelicula.sinopsis,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}