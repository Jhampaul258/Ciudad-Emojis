package presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        val peliculaRepository = remember { PeliculaRepository() }

        // Obtenemos todas las películas
        val allPeliculas by peliculaRepository.getAllPeliculasStream().collectAsState(initial = null)

        // Filtramos las listas (Series vs Películas)
        // Usamos 'remember' para no recalcular en cada frame
        val seriesList = remember(allPeliculas) { allPeliculas?.filter { it.esSerie } ?: emptyList() }
        val moviesList = remember(allPeliculas) { allPeliculas?.filter { !it.esSerie } ?: emptyList() }

        // La "Destacada" será la primera de la lista general (la más reciente)
        val featuredContent = remember(allPeliculas) { allPeliculas?.firstOrNull() }

        Scaffold(
//            containerColor = Color(0xFF121212) // Fondo oscuro estilo cine
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (allPeliculas == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (allPeliculas!!.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay contenido disponible", color = MaterialTheme.colorScheme.onBackground,)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding()), // Respetar BottomBar
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // --- 1. SECCIÓN DESTACADA (HERO) ---
                    if (featuredContent != null) {
                        item {
                            FeaturedHeader(
                                pelicula = featuredContent,
                                onPlayClick = {
                                    if (featuredContent.videoUrl.isNotBlank()) {
                                        uriHandler.openUri(featuredContent.videoUrl)
                                    }
                                },
                                onInfoClick = { navigator.push(PeliculaDetailScreen(featuredContent)) }
                            )
                        }
                    }

                    // --- 2. SECCIÓN SERIES ---
                    if (seriesList.isNotEmpty()) {
                        item {
                            SectionTitle("Series")
                            ContentRow(seriesList) { selected ->
                                navigator.push(PeliculaDetailScreen(selected))
                            }
                        }
                    }

                    // --- 3. SECCIÓN PELÍCULAS ---
                    if (moviesList.isNotEmpty()) {
                        item {
                            SectionTitle("Películas")
                            ContentRow(moviesList) { selected ->
                                navigator.push(PeliculaDetailScreen(selected))
                            }
                        }
                    }

                    // --- 4. ÚLTIMOS ESTRENOS (TODOS) ---
                    item {
                        SectionTitle("Últimos Agregados")
                        ContentRow(allPeliculas!!) { selected ->
                            navigator.push(PeliculaDetailScreen(selected))
                        }
                        Spacer(Modifier.height(80.dp)) // Espacio extra al final
                    }
                }
            }
        }
    }
}

// --- COMPONENTES UI ---

@Composable
fun FeaturedHeader(
    pelicula: Pelicula,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val thumb = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp) // Altura grande e imponente
    ) {
        // Imagen de fondo
        AsyncImage(
            model = thumb,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Degradado Oscuro (Vignette)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
//                            Color(0xFF121212).copy(alpha = 0.5f),
//                            Color(0xFF121212)
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background // Termina en el color de fondo sólido
                        ),
                        startY = 200f
                    )
                )
        )

        // Contenido (Texto y Botones)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = pelicula.titulo,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
//                color = Color.White,
                color = MaterialTheme.colorScheme.onBackground, // Color de texto dinámico
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Tags (Género • Año)
            Text(
                text = "${pelicula.genero} • ${pelicula.anio}",
                style = MaterialTheme.typography.bodyMedium,
//                color = Color.LightGray
                color = MaterialTheme.colorScheme.onBackground, // Color de texto dinámico
            )

            Spacer(Modifier.height(16.dp))

            // Botones de Acción
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Botón Play (Blanco, destacado)
                Button(
                    onClick = onPlayClick,
//                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.onBackground,),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.7f), contentColor = Color.White),

                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Reproducir", fontWeight = FontWeight.Bold)
                }

                // Botón Info (Gris oscuro)
                Button(
                    onClick = onInfoClick,
//                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), contentColor = MaterialTheme.colorScheme.onBackground),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.7f), contentColor = Color.White),

                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.Info, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Info")
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ContentRow(
    items: List<Pelicula>,
    onItemClick: (Pelicula) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { pelicula ->
            PosterCard(pelicula, onItemClick)
        }
    }
}

@Composable
fun PosterCard(
    pelicula: Pelicula,
    onClick: (Pelicula) -> Unit
) {
    val thumb = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }

    Column(
        modifier = Modifier
            .width(120.dp) // Ancho fijo para posters
            .clickable { onClick(pelicula) }
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Formato vertical (Poster)
                    .background(MaterialTheme.colorScheme.onBackground),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = pelicula.titulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}