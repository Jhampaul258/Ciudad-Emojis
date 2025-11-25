package presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import domain.model.Pelicula
import presentation.detail.PeliculaDetailScreen
import utils.getYoutubeThumbnail

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { HomeViewModel() }
        val state by viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // --- 1. BARRA DE BÚSQUEDA Y FILTROS (FIJA ARRIBA) ---
                SearchAndFilterSection(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChanged,
                    selectedGenre = state.selectedGenre,
                    genres = state.availableGenres,
                    onGenreSelected = viewModel::onGenreSelected
                )

                // --- 2. CONTENIDO SCROLLABLE ---
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.groupedContent.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No se encontraron resultados", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Si hay búsqueda activa, ocultamos el Hero gigante para mostrar resultados directos
                        // O puedes dejarlo si prefieres que siempre se vea
                        if (state.searchQuery.isEmpty() && state.selectedGenre == "Todos") {
                            state.featuredContent?.let { featured ->
                                item {
                                    FeaturedHeader(
                                        pelicula = featured,
                                        onPlayClick = { if (featured.videoUrl.isNotBlank()) uriHandler.openUri(featured.videoUrl) },
                                        onInfoClick = { navigator.push(PeliculaDetailScreen(featured)) }
                                    )
                                }
                            }
                        }

                        // Secciones
                        if (state.seriesList.isNotEmpty()) {
                            item {
                                SectionTitle("Series")
                                ContentRow(state.seriesList) { navigator.push(PeliculaDetailScreen(it)) }
                            }
                        }

                        if (state.moviesList.isNotEmpty()) {
                            item {
                                SectionTitle("Películas")
                                ContentRow(state.moviesList) { navigator.push(PeliculaDetailScreen(it)) }
                            }
                        }

                        // Todos los resultados (útil cuando filtras)
                        item {
                            SectionTitle(if (state.searchQuery.isNotEmpty()) "Resultados de búsqueda" else "Todo el catálogo")
                            ContentRow(state.groupedContent) { navigator.push(PeliculaDetailScreen(it)) }
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- NUEVO COMPONENTE: BUSCADOR Y FILTROS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedGenre: String,
    genres: List<String>,
    onGenreSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background) // Fondo para que no sea transparente al scrollear
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Barra de Búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Buscar película, serie o director...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Borrar")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )

        Spacer(Modifier.height(12.dp))

        // Lista de Géneros (Chips)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                FilterChip(
                    selected = genre == selectedGenre,
                    onClick = { onGenreSelected(genre) },
                    label = { Text(genre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

// --- COMPONENTES EXISTENTES (Sin cambios, solo asegúrate de tenerlos) ---

@Composable
fun FeaturedHeader(pelicula: Pelicula, onPlayClick: () -> Unit, onInfoClick: () -> Unit) {
    val thumb = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }
    Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
        AsyncImage(model = thumb, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.5f), MaterialTheme.colorScheme.background), startY = 200f)))
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = pelicula.titulo, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(text = "${pelicula.genero} • ${pelicula.anio}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onPlayClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background), shape = RoundedCornerShape(4.dp)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Reproducir", fontWeight = FontWeight.Bold)
                }
                Button(onClick = onInfoClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), contentColor = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(4.dp)) {
                    Icon(Icons.Default.Info, null); Spacer(Modifier.width(4.dp)); Text("Info")
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
fun ContentRow(items: List<Pelicula>, onItemClick: (Pelicula) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { pelicula -> PosterCard(pelicula, onItemClick) }
    }
}

@Composable
fun PosterCard(pelicula: Pelicula, onClick: (Pelicula) -> Unit) {
    val thumb = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }
    val displayText = if (pelicula.esSerie && pelicula.nombreSerie.isNotBlank()) pelicula.nombreSerie else pelicula.titulo
    Column(modifier = Modifier.width(120.dp).clickable { onClick(pelicula) }) {
        Card(shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            AsyncImage(model = thumb, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).background(Color.DarkGray), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(4.dp))
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}