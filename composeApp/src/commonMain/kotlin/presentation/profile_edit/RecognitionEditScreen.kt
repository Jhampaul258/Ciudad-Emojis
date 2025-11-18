package presentation.profile_edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
//import androidx.compose.material.icons.filled.RemoveCircleOutline
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
import presentation.components.LoadingButton

// Usamos Voyager Screen para la navegación
data class RecognitionEditScreen(val directorId: String) : Screen {

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

        // --- Estados para los campos del formulario (Listas) ---
        var festivales by remember { mutableStateOf<List<String>>(emptyList()) }
        var premios by remember { mutableStateOf<List<String>>(emptyList()) }

        var isSaving by remember { mutableStateOf(false) }

        // Efecto para cargar los datos del director una sola vez
        LaunchedEffect(directorId) {
            isLoadingData = true
            try {
                directorRepository.getDirector(directorId).collect { director ->
                    if (director != null) {
                        initialDirectorData = director
                        // Inicializa los estados del formulario con los datos cargados
                        festivales = director.festivales
                        premios = director.premios
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
                    title = { Text("Reconocimientos y Experiencias") },
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
                            .verticalScroll(rememberScrollState()), // Permite scroll
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- Editor para la lista de FESTIVALES ---
                        DynamicListEditor(
                            title = "Festivales",
                            items = festivales,
                            onAddItem = { newItem ->
                                // Añade el nuevo item a la lista de estado
                                if (newItem.isNotBlank()) {
                                    festivales = festivales + newItem
                                }
                            },
                            onRemoveItem = { index ->
                                // Remueve el item por índice
                                festivales = festivales.filterIndexed { i, _ -> i != index }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Editor para la lista de PREMIOS ---
                        DynamicListEditor(
                            title = "Premios",
                            items = premios,
                            onAddItem = { newItem ->
                                if (newItem.isNotBlank()) {
                                    premios = premios + newItem
                                }
                            },
                            onRemoveItem = { index ->
                                premios = premios.filterIndexed { i, _ -> i != index }
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- Botón Guardar ---
                        LoadingButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    val updatedDirector = initialDirectorData!!.copy(
                                        // Guarda las listas actualizadas
                                        festivales = festivales,
                                        premios = premios
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
                        Spacer(modifier = Modifier.height(16.dp)) // Espacio al final
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
 * Un Composable reutilizable para editar una lista de Strings.
 * Muestra un campo de texto para añadir nuevos items y una lista de items existentes con botones para eliminarlos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicListEditor(
    title: String,
    items: List<String>,
    onAddItem: (String) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // --- Campo para añadir nuevos items ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = { Text("Añadir $title") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText)
                        newItemText = "" // Limpia el campo después de añadir
                    }
                }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Añadir $title", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Lista de items existentes ---
            if (items.isNotEmpty()) {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onRemoveItem(index) }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Eliminar item", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = "Aún no has añadido ningún $title.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}