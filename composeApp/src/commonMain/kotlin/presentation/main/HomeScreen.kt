// Archivo: src/commonMain/kotlin/presentation/main/HomeScreen.kt
@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.PeliculaRepository
import domain.model.Pelicula

@Composable
fun HomeScreen() {

    // 1. Inicializa el repositorio
    val peliculaRepository = remember { PeliculaRepository() }

    // 2. Recolecta el Flow como un State.
    // 'collectAsState' convierte el Flow en tiempo real en un State de Compose.
    // Empezamos con 'initial = null' para representar el estado de "carga inicial".
    val peliculasState by peliculaRepository.getAllPeliculasStream().collectAsState(initial = null)

    // 'peliculas' será null (cargando), lista vacía (sin datos), o lista llena.
    val peliculas = peliculasState

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Estrenos") }
//            )
//        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // --- Caso 1: Cargando (el estado inicial es null) ---
                peliculas == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // --- Caso 2: Lista vacía ---
                peliculas.isEmpty() -> {
                    Text(
                        text = "Aún no se ha subido ninguna película. \n¡Sé el primero desde la pestaña 'Subir'!",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // --- Caso 3: Tenemos películas ---
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Título de la sección (estilo Netflix)
                        item {
                            Text(
                                "Recién Añadido",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // El "feed" de películas
                        items(peliculas, key = { it.id }) { pelicula ->
                            PeliculaCard(
                                pelicula = pelicula,
                                onClick = {
                                    // TODO: Navegar a la pantalla de detalle de la película
                                }
                            )
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
                // --- IMPORTANTE: Cargar la imagen ---
                // Aquí necesitas una librería de carga de imágenes KMP como Kamel-image o Coil
                // (si usas 'compose-imageloader' o similar).

                // KamelImage(
                //    resource = asyncPainterResource(data = pelicula.caratulaUrl),
                //    contentDescription = pelicula.titulo,
                //    contentScale = ContentScale.Crop,
                //    modifier = Modifier.fillMaxSize()
                // )

                // --- Placeholder mientras no tengas la librería ---
                if (pelicula.caratulaUrl.isBlank()) {
                    Text("Sin Carátula", style = MaterialTheme.typography.bodySmall)
                } else {
                    // TODO: Reemplazar este Text con el cargador de imagen
                    Text("Cargar: ${pelicula.caratulaUrl.take(20)}...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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