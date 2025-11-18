package presentation.profile_edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.DirectorRepository
import domain.model.Director
import kotlinx.coroutines.launch
import presentation.components.LoadingButton // Reutilizamos tu LoadingButton

// Usamos Voyager Screen para la navegación
data class PresentationEditScreen(val directorId: String) : Screen {

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

        // Estados para los campos del formulario
        var biografia by remember { mutableStateOf("") }
        var inspiraciones by remember { mutableStateOf("") }
        var lema by remember { mutableStateOf("") }

        var isSaving by remember { mutableStateOf(false) }

        // Efecto para cargar los datos del director una sola vez
        LaunchedEffect(directorId) {
            isLoadingData = true
            try {
                directorRepository.getDirector(directorId).collect { director ->
                    if (director != null) {
                        initialDirectorData = director
                        // Inicializa los estados del formulario con los datos cargados
                        biografia = director.biografia
                        inspiraciones = director.inspiraciones
                        lema = director.lema
                        errorMessage = null // Limpia errores previos si carga bien
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
                    title = { Text("Presentación del Director") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) { // Navegar atrás con Voyager
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
                initialDirectorData != null -> { // Solo muestra el form si los datos iniciales se cargaron
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()), // Permite scroll
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // --- Campos del Formulario ---
                        OutlinedTextField(
                            value = biografia,
                            onValueChange = { biografia = it },
                            label = { Text("Biografía") },
                            modifier = Modifier.fillMaxWidth().height(150.dp), // Campo más alto
                            // singleLine = false (por defecto)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = inspiraciones,
                            onValueChange = { inspiraciones = it },
                            label = { Text("Inspiraciones") },
                            modifier = Modifier.fillMaxWidth().height(100.dp), // Campo más alto
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = lema,
                            onValueChange = { lema = it },
                            label = { Text("Lema o frase") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- Botón Guardar ---
                        LoadingButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    // Usamos !! porque ya comprobamos que initialDirectorData no es null
                                    val updatedDirector = initialDirectorData!!.copy(
                                        biografia = biografia,
                                        inspiraciones = inspiraciones,
                                        lema = lema
                                    )
                                    try {
                                        directorRepository.createOrUpdateDirector(updatedDirector)
                                        navigator.pop() // Vuelve a la pantalla anterior si guarda bien
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
                    }
                }
                else -> { // Caso improbable
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("No se pudo cargar la información.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}