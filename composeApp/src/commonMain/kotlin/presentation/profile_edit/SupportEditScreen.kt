package presentation.profile_edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Info
//import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.DirectorRepository
import domain.model.Director
import kotlinx.coroutines.launch
import presentation.components.LoadingButton // Reutilizamos tu LoadingButton
import presentation.profile_edit.KmpImagePreview
import presentation.profile_edit.PreviewNotAvailable

import utils.AppColors
import utils.FileData
import utils.FilePicker
import utils.rememberImagePicker
import utils.toKmpImageBitmap

// Usamos Voyager Screen para la navegación
data class SupportEditScreen(val directorId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val directorRepository = remember { DirectorRepository() }
        val scope = rememberCoroutineScope()

        // Estado para cargar los datos iniciales
        var initialDirectorData by remember { mutableStateOf<Director?>(null) }
        var isLoadingData by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // --- Estados para los campos del formulario ---
        var yapeNumero by remember { mutableStateOf("") }
        var plinNumero by remember { mutableStateOf("") }
        var yapeQrUrl by remember { mutableStateOf("") } // URL existente

        var showFilePicker by remember { mutableStateOf(false) }

        var newQrImageBytes by remember { mutableStateOf<FileData?>(null) }
        val imagePickerLauncher = rememberImagePicker { fileData ->
            // El callback cuando se selecciona una imagen
            newQrImageBytes = fileData
        }
        FilePicker(
            show = showFilePicker,
            // ¡CORREGIDO! Solo muestra estos tipos de archivo
            fileExtensions = listOf("pdf", "jpg", "png"),
            onFileSelected = { fileData ->
                newQrImageBytes = fileData
                showFilePicker = false
            }
        )

        var isSaving by remember { mutableStateOf(false) }

        // Efecto para cargar los datos del director
        LaunchedEffect(directorId) {
            isLoadingData = true
            try {
                directorRepository.getDirector(directorId).collect { director ->
                    if (director != null) {
                        initialDirectorData = director
                        // Inicializa los estados del formulario
                        yapeNumero = director.yapeNumero
                        plinNumero = director.plinNumero
                        yapeQrUrl = director.yapeQrUrl // Carga la URL de la imagen existente
                        errorMessage = null
                    } else {
                        errorMessage = "No se encontró la información del director."
                    }
                    isLoadingData = false
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar datos: ${e.message}"
                isLoadingData = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Apoyo y Financiamiento") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when {
                isLoadingData -> {
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: "Error", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
                initialDirectorData != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- Campos de texto simples ---
                        OutlinedTextField(
                            value = yapeNumero,
                            onValueChange = { yapeNumero = it },
                            label = { Text("Número Yape (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Yape") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = plinNumero,
                            onValueChange = { plinNumero = it },
                            label = { Text("Número Plin (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Plin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Sección de Subida de Imagen QR ---
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Código QR de Yape", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))

                                // --- Vista previa de la imagen ---
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (newQrImageBytes != null) {
                                        KmpImagePreview(
                                            bytes = newQrImageBytes!!.bytes,
                                            modifier = Modifier.matchParentSize() // ocupa todo el Box

                                        )
//                                        Icon(Icons.Default.Add, contentDescription = "Nueva imagen seleccionada", modifier = Modifier.size(100.dp))
                                    } else if (yapeQrUrl.isNotBlank()) {
                                        // TODO: Aquí usarías Kamel o Coil para cargar la 'yapeQrUrl'
                                        // p.ej. AsyncImage(model = yapeQrUrl, ...)
                                        Icon(Icons.Default.Add, contentDescription = "QR existente", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Text("Sin QR", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {imagePickerLauncher()},
                                    modifier = Modifier.fillMaxWidth().height(56.dp), // <- Modificado
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.black
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Seleccionar de Galería") // <-- Texto actualizado
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- Botón Guardar ---
                        LoadingButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    errorMessage = null // Limpiar error previo
                                    var finalUrl = yapeQrUrl // Empezamos con la URL que ya teníamos

                                    // --- LÓGICA DE SUBIDA A FIREBASE STORAGE ---
                                    if (newQrImageBytes != null) {


//                                        errorMessage = "La subida de archivos no está implementada."
//                                        isSaving = false
                                        return@launch
                                    }


                                    val updatedDirector = initialDirectorData!!.copy(
                                        yapeNumero = yapeNumero,
                                        plinNumero = plinNumero,
                                        yapeQrUrl = finalUrl // Guarda la URL (nueva o la antigua)
                                    )
                                    try {
                                        directorRepository.createOrUpdateDirector(updatedDirector)
                                        navigator.pop()
                                    } catch (e: Exception) {
                                        errorMessage = "Error al guardar: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            text = "Guardar Cambios",
                            isLoading = isSaving
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("No se pudo cargar la información.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * Vista previa de imagen KMP que decodifica un ByteArray.
 */

// Actualizado para tomar un texto opcional
@Composable
private fun PreviewNotAvailable(text: String = "Vista previa no disponible") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        Text(
            "El archivo se procesará correctamente",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
@Composable
private fun KmpImagePreview(bytes: ByteArray, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = bytes.toKmpImageBitmap()

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Vista previa de imagen",
            modifier = modifier.fillMaxSize() .padding(8.dp),
            contentScale = ContentScale.FillBounds
        )
    } else {
        PreviewNotAvailable(text = "No se pudo cargar la vista previa")
    }
}