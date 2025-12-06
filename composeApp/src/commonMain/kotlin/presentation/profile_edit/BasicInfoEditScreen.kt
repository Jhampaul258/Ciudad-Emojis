package presentation.profile_edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Importa el icono correcto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.DirectorRepository
import domain.model.Director
import kotlinx.coroutines.launch
import presentation.components.LoadingButton // Reutilizamos tu LoadingButton

// Usamos Voyager Screen para la navegación
data class BasicInfoEditScreen(val directorId: String) : Screen {

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
        var name by remember { mutableStateOf("") }
        var ciudadOrigen by remember { mutableStateOf("") }
        var universidad by remember { mutableStateOf("") }
        var carrera by remember { mutableStateOf("") }
        var fotoPerfilUrl by remember { mutableStateOf("") } // Solo para mostrarla

        var isSaving by remember { mutableStateOf(false) }

        LaunchedEffect(directorId) {
            isLoadingData = true
            try {
                directorRepository.getDirector(directorId).collect { director ->
                    if (director != null) {
                        initialDirectorData = director
                        name = director.name
                        ciudadOrigen = director.ciudadOrigen
                        universidad = director.universidad
                        carrera = director.carrera
                        fotoPerfilUrl = director.fotoPerfilUrl
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
                    title = { Text("Información Básica") },
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
                        Text(errorMessage ?: "Error", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                        // --- Sección Foto de Perfil ---
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                // Aquí usarías Coil/Kamel: Image(painterResource(fotoPerfilUrl), ...)
                                Text("Foto") // Placeholder
                            }
                            FloatingActionButton(
                                onClick = { /* TODO: Lógica para seleccionar/cambiar foto */ },
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Cambiar foto", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Campos del Formulario ---
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = ciudadOrigen,
                            onValueChange = { ciudadOrigen = it },
                            label = { Text("Ciudad de origen (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = universidad,
                            onValueChange = { universidad = it },
                            label = { Text("Nombre de la Universidad") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = carrera,
                            onValueChange = { carrera = it },
                            label = { Text("Carrera") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- Botón Guardar ---
                        LoadingButton( // Usamos tu LoadingButton existente
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    // Usamos !! porque ya comprobamos que initialDirectorData no es null
                                    val updatedDirector = initialDirectorData!!.copy(
                                        name = name,
                                        ciudadOrigen = ciudadOrigen,
                                        universidad = universidad,
                                        carrera = carrera
                                        // fotoPerfilUrl se actualizaría si implementas el cambio
                                    )
                                    try {
                                        // Asegúrate que el método en DirectorRepository use 'uid'
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
                else -> { // Caso improbable donde initialDirectorData es null y no hay error
                    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("No se pudo cargar la información.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}