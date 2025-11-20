package presentation.profile_edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage // <--- IMPORTANTE: Coil 3 para cargar URLs
import data.BlobUploadRepository
import data.DirectorRepository
import domain.model.Director
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import presentation.components.LoadingButton
import utils.AppColors
import utils.FileData
import utils.FilePicker
import utils.rememberImagePicker
import utils.toKmpImageBitmap

data class SupportEditScreen(val directorId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val directorRepository = remember { DirectorRepository() }

        // --- Configuración para subida de imágenes (Vercel) ---
        val httpClient = remember {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; prettyPrint = true })
                }
            }
        }
        val jsonParser = remember { Json { ignoreUnknownKeys = true } }
        val blobUploadRepository = remember { BlobUploadRepository(httpClient, jsonParser) }

        // --- Estados de Datos ---
        var initialDirectorData by remember { mutableStateOf<Director?>(null) }
        var isLoadingData by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // --- Campos del Formulario ---
        var yapeNumero by remember { mutableStateOf("") }
        var plinNumero by remember { mutableStateOf("") }
        var yapeQrUrl by remember { mutableStateOf("") }

        var showFilePicker by remember { mutableStateOf(false) }
        var newQrImageBytes by remember { mutableStateOf<FileData?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        val imagePickerLauncher = rememberImagePicker { fileData ->
            newQrImageBytes = fileData
        }

        // FilePicker manual (si se usa)
        FilePicker(
            show = showFilePicker,
            fileExtensions = listOf("jpg", "png", "jpeg"),
            onFileSelected = { fileData ->
                newQrImageBytes = fileData
                showFilePicker = false
            }
        )

        // Cargar datos iniciales
        LaunchedEffect(directorId) {
            isLoadingData = true
            try {
                directorRepository.getDirector(directorId).collect { director ->
                    if (director != null) {
                        initialDirectorData = director
                        yapeNumero = director.yapeNumero
                        plinNumero = director.plinNumero
                        yapeQrUrl = director.yapeQrUrl // URL guardada en Firestore
                        errorMessage = null
                    } else {
                        errorMessage = "No se encontró la información."
                    }
                    isLoadingData = false
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                isLoadingData = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Apoyo y Financiamiento") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when {
                isLoadingData -> Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                errorMessage != null -> Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                initialDirectorData != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = yapeNumero,
                            onValueChange = { yapeNumero = it },
                            label = { Text("Número Yape") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = plinNumero,
                            onValueChange = { plinNumero = it },
                            label = { Text("Número Plin") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(Modifier.height(24.dp))

                        // --- TARJETA DE IMAGEN QR ---
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Código QR de Yape", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        // 1. Prioridad: Mostrar imagen nueva seleccionada (bytes locales)
                                        newQrImageBytes != null -> {
                                            KmpImagePreview(
                                                bytes = newQrImageBytes!!.bytes,
                                                modifier = Modifier.matchParentSize()
                                            )
                                        }
                                        // 2. Mostrar imagen guardada (URL remota) usando Coil
                                        yapeQrUrl.isNotBlank() -> {
                                            AsyncImage(
                                                model = yapeQrUrl,
                                                contentDescription = "QR Guardado",
                                                modifier = Modifier.matchParentSize(),
                                                contentScale = ContentScale.Crop,
                                                onLoading = {
                                                    // Opcional: mostrar loading mientras carga
                                                },
                                                onError = {
                                                    println("Error cargando imagen QR: ${it.result.throwable}")
                                                }
                                            )
                                        }
                                        // 3. Estado vacío
                                        else -> {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Add, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                                Text("Sin QR", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { imagePickerLauncher() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.black)
                                ) {
                                    Icon(Icons.Filled.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Subir Imagen")
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        LoadingButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    errorMessage = null
                                    var finalUrl = yapeQrUrl

                                    // --- Subida a Vercel ---
                                    if (newQrImageBytes != null) {
                                        val result = blobUploadRepository.uploadFile(newQrImageBytes!!)
                                        if (result.isSuccess) {
                                            finalUrl = result.getOrThrow()
                                        } else {
                                            errorMessage = "Error subiendo imagen: ${result.exceptionOrNull()?.message}"
                                            isSaving = false
                                            return@launch
                                        }
                                    }

                                    // --- Guardado en Firestore ---
                                    val updated = initialDirectorData!!.copy(
                                        yapeNumero = yapeNumero,
                                        plinNumero = plinNumero,
                                        yapeQrUrl = finalUrl
                                    )

                                    try {
                                        directorRepository.createOrUpdateDirector(updated)
                                        navigator.pop()
                                    } catch (e: Exception) {
                                        errorMessage = "Error guardando datos: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            text = "Guardar Cambios",
                            isLoading = isSaving
                        )
                    }
                }
            }
        }
    }
}

// Funciones auxiliares para previsualización local
@Composable
private fun PreviewNotAvailable(text: String = "Vista previa no disponible") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(Icons.Outlined.Info, null, Modifier.size(32.dp), Color.Gray)
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun KmpImagePreview(bytes: ByteArray, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = bytes.toKmpImageBitmap()
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Preview",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        PreviewNotAvailable()
    }
}