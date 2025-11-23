package presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
//import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import data.DirectorRepository
import data.PeliculaRepository
import domain.model.Director
import domain.model.Pelicula
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import utils.getYoutubeThumbnail
import kotlinx.coroutines.launch

data class PeliculaDetailScreen(val pelicula: Pelicula) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()

        // --- Repositorios ---
        val peliculaRepository = remember { PeliculaRepository() }
        val directorRepository = remember { DirectorRepository() }

        // --- Estado ---
        val allPeliculasState by peliculaRepository.getAllPeliculasStream().collectAsState(initial = emptyList())
        var directorData by remember { mutableStateOf<Director?>(null) }

        // Cargar datos del director para obtener el QR y números
        LaunchedEffect(pelicula.directorId) {
            directorRepository.getDirector(pelicula.directorId).collect {
                directorData = it
            }
        }

        // Lógica de Recomendación
        val recomendaciones = remember(allPeliculasState, pelicula) {
            // 1. PRIORIDAD: Si es serie, buscar sus episodios hermanos
            if (pelicula.esSerie && pelicula.nombreSerie.isNotBlank()) {
                val episodios = allPeliculasState.filter {
                    it.esSerie &&
                            it.nombreSerie.equals(pelicula.nombreSerie, ignoreCase = true) &&
                            it.id != pelicula.id // No mostrar el video actual
                }.sortedBy { it.numeroCapitulo } // Ordenar: Cap 1, Cap 2...

                if (episodios.isNotEmpty()) return@remember episodios
            }

            // 2. SECUNDARIO: Películas del mismo director
            val delDirector = allPeliculasState.filter {
                it.directorId == pelicula.directorId && it.id != pelicula.id
            }

            // 3. FALLBACK: Sugerencias aleatorias
            if (delDirector.isNotEmpty()) delDirector else allPeliculasState.filter { it.id != pelicula.id }.shuffled().take(5)
        }

        val tituloSeccion = remember(pelicula, recomendaciones) {
            when {
                pelicula.esSerie && recomendaciones.any { it.nombreSerie == pelicula.nombreSerie } -> "Más episodios de ${pelicula.nombreSerie}"
                recomendaciones.any { it.directorId == pelicula.directorId } -> "Más de ${pelicula.directorName}"
                else -> "Te podría gustar"
            }
        }

        val headerThumbnail = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { ReusableSnackbarHost(snackbarController) },
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // 1. HEADER CON IMAGEN
                item {
                    Box(Modifier.fillMaxWidth().height(300.dp)) {
                        AsyncImage(
                            model = headerThumbnail,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                    startY = 300f
                                )
                            )
                        )
                        IconButton(
                            onClick = { if (pelicula.videoUrl.isNotBlank()) uriHandler.openUri(pelicula.videoUrl) },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), CircleShape),
                        ) {
                            Icon(Icons.Default.PlayArrow, "Ver", Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // 2. INFORMACIÓN
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Si es serie, mostramos "Serie • Cap X"
                        val infoExtra = if (pelicula.esSerie) " • Cap. ${pelicula.numeroCapitulo}" else ""

                        Text(pelicula.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${pelicula.anio}", color = MaterialTheme.colorScheme.primary)
                            Text(" • ${pelicula.genero}$infoExtra", color = MaterialTheme.colorScheme.secondary)
                            if (pelicula.esSerie) {
                                Spacer(Modifier.width(8.dp))
                                AssistChip(onClick = {}, label = { Text("Serie") }, modifier = Modifier.height(24.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Dirigido por ${pelicula.directorName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Divider(Modifier.padding(vertical = 16.dp))
                        Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(pelicula.sinopsis, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(32.dp))
                    }
                }



                // 4. RECOMENDACIONES
                if (recomendaciones.isNotEmpty()) {
                    item {
                        Text(tituloSeccion, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    items(recomendaciones) { itemRecomendado ->
                        RecommendationItem(
                            pelicula = itemRecomendado,
                            isSeriesContext = pelicula.esSerie && itemRecomendado.nombreSerie == pelicula.nombreSerie,
                            onClick = { navigator.push(PeliculaDetailScreen(itemRecomendado)) }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
                // 3. SECCIÓN DE DONACIÓN (NUEVA)
                if (directorData != null && (directorData!!.yapeQrUrl.isNotEmpty() || directorData!!.yapeNumero.isNotEmpty() || directorData!!.plinNumero.isNotEmpty())) {
                    item {
                        DonationCard(
                            director = directorData!!,
                            onCopyClick = { text ->
                                scope.launch { snackbarController.showSuccess("Copiado: $text") }
                            }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DonationCard(director: Director, onCopyClick: (String) -> Unit) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Apoya a ${director.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            // QR Code Image
            if (director.yapeQrUrl.isNotBlank()) {
                AsyncImage(
                    model = director.yapeQrUrl,
                    contentDescription = "QR Yape",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White), // Fondo blanco para que el QR lea bien
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
            }

            // Números de Yape/Plin
            if (director.yapeNumero.isNotBlank()) {
                DonationNumberRow("Yape", director.yapeNumero) {
                    clipboardManager.setText(AnnotatedString(director.yapeNumero))
                    onCopyClick(director.yapeNumero)
                }
            }

            if (director.plinNumero.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                DonationNumberRow("Plin", director.plinNumero) {
                    clipboardManager.setText(AnnotatedString(director.plinNumero))
                    onCopyClick(director.plinNumero)
                }
            }
        }
    }
}

@Composable
fun DonationNumberRow(label: String, number: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.Build, "Copiar", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RecommendationItem(pelicula: Pelicula,isSeriesContext: Boolean, onClick: () -> Unit) {
    val thumb = remember(pelicula.videoUrl) { getYoutubeThumbnail(pelicula.videoUrl) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(90.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            // Si estamos viendo una serie, resaltamos el número de capítulo
            if (isSeriesContext) {
                Text("Capítulo ${pelicula.numeroCapitulo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }

            Text(pelicula.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)

            if (!isSeriesContext) {
                Spacer(Modifier.height(4.dp))
                Text(pelicula.directorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }

            // Información sutil abajo
            Text(if (isSeriesContext) "${pelicula.anio}" else "${pelicula.anio} • ${pelicula.genero}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)        }
    }
}