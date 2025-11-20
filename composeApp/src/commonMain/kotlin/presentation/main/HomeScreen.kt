// Archivo: src/commonMain/kotlin/presentation/main/HomeScreen.kt
//@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import data.PeliculaRepository
import domain.model.Pelicula
import presentation.detail.PeliculaDetailScreen
import utils.getYoutubeThumbnail

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        // Obtenemos el navegador local (ahora será el anidado, no el TabNavigator)
        val navigator = LocalNavigator.currentOrThrow

        val peliculaRepository = remember { PeliculaRepository() }
        val peliculasState by peliculaRepository.getAllPeliculasStream().collectAsState(initial = null)
        val peliculas = peliculasState

        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    peliculas == null -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    peliculas.isEmpty() -> {
                        Text(
                            text = "Aún no se ha subido ninguna película.",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text("Recién Añadido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                            items(peliculas, key = { it.id }) { pelicula ->
                                PeliculaCard(
                                    pelicula = pelicula,
                                    onClick = {
                                        // AHORA SÍ funcionará: Navegamos dentro de la pestaña actual
                                        navigator.push(PeliculaDetailScreen(pelicula))
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
/**
 * Un Composable para mostrar una tarjeta de película (estilo Netflix/Facebook).
 */
@Composable
fun PeliculaCard(
    pelicula: Pelicula,
    onClick: () -> Unit
) {
    val thumbnailUrl = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // --- Portada (Carátula) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {

                if (thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = pelicula.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Degradado sutil en la parte inferior de la imagen para mejorar lectura si hubiera texto
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                                    startY = 300f
                                )
                            )
                    )

//                    // Icono de Play superpuesto
//                    Icon(
//                        imageVector = Icons.Default.Star,
//                        contentDescription = "Reproducir",
//                        tint = Color.White.copy(alpha = 0.8f),
//                        modifier = Modifier.size(48.dp)
//                    )
                } else {
                    // Fallback si no hay URL de video válida
                    Text("Sin vista previa", color = Color.White)
                }
            }

            // --- Información (Título, Director) ---
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = pelicula.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = pelicula.anio.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Por: ${pelicula.directorName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pelicula.sinopsis,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}