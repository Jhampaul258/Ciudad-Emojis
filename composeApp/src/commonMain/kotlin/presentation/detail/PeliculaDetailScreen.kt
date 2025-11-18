package presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.DirectorRepository
import data.PeliculaRepository
import domain.model.Director
import domain.model.Pelicula
import presentation.components.YouTubePlayer

// Hacemos que la Screen sea un 'data class' para pasar el objeto 'Pelicula'
// (Asegúrate que Pelicula es @Serializable, lo cual ya hiciste)
data class PeliculaDetailScreen(val pelicula: Pelicula) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Repositorios
        val directorRepository = remember { DirectorRepository() }
        val peliculaRepository = remember { PeliculaRepository() }

        // Estados para los datos dinámicos
        var director by remember { mutableStateOf<Director?>(null) }
        var recommendations by remember { mutableStateOf<List<Pelicula>>(emptyList()) }

        // Cargamos los datos extra (QR del director y sus otras películas)
        LaunchedEffect(pelicula.directorId) {
            // Cargar datos del director
            directorRepository.getDirector(pelicula.directorId).collect {
                director = it
            }

            // Cargar recomendaciones (si no es una serie)
            if (!pelicula.esSerie) {
                peliculaRepository.getPeliculasByDirector(pelicula.directorId).collect { allFilms ->
                    // Filtramos la película actual de las recomendaciones
                    recommendations = allFilms.filter { it.id != pelicula.id }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(pelicula.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // --- 1. Reproductor de YouTube ---
                item {
                    YouTubePlayer(
                        videoUrl = pelicula.videoUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f) // Aspect ratio estándar de video
                            .background(Color.Black)
                    )
                }

                // --- 2. Título y Sinopsis ---
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(pelicula.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${pelicula.directorName} • ${pelicula.anio} • ${pelicula.genero}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Sinopsis", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(pelicula.sinopsis, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // --- 3. Lógica: Episodios o Recomendaciones ---
                if (pelicula.esSerie) {
                    // --- 4. Episodios ---
                    item {
                        SectionTitle("Episodios")
                    }
                    if (pelicula.episodios.isEmpty()) {
                        item {
                            Text("No hay episodios disponibles.", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else {
                        // Aquí podrías usar 'items(pelicula.episodios) { ... }'
                        // Por ahora, solo mostramos un placeholder
                        item { Text("Implementar lista de episodios aquí.", modifier = Modifier.padding(horizontal = 16.dp)) }
                    }

                } else {
                    // --- 5. Recomendaciones ---
                    item {
                        SectionTitle("Más de ${pelicula.directorName}")
                    }
                    if (recommendations.isEmpty()) {
                        item {
                            Text("No hay otras películas de este director.", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recommendations) { film ->
                                    RecommendationCard(film) {
                                        // Navegar a la otra película (reemplaza la pantalla actual)
                                        navigator.replace(PeliculaDetailScreen(film))
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 6. QR de Yape ---
                item {
                    SectionTitle("Apoya al Director")
                }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (director == null) {
                            CircularProgressIndicator()
                        } else if (director!!.yapeQrUrl.isNotBlank()) {
                            // --- Cargar Imagen QR ---
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                // TODO: Usar Kamel o Coil para cargar 'director.yapeQrUrl'
                                Text("Cargar QR: ${director!!.yapeQrUrl.take(20)}...")
                            }
                            if (director!!.yapeNumero.isNotBlank()) {
                                Text("Yape: ${director!!.yapeNumero}", style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            Text("Este director aún no ha configurado su Yape.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
        )
    }

    @Composable
    private fun RecommendationCard(film: Pelicula, onClick: () -> Unit) {
        Card(
            modifier = Modifier.width(140.dp).clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: Cargar 'film.caratulaUrl'
                    Text(film.titulo.first().toString(), style = MaterialTheme.typography.headlineLarge)
                }
                Text(
                    film.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}