package presentation.profile_edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
//import androidx.compose.material.icons.filled.Link
//import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.DirectorRepository
import domain.model.Director
import kotlinx.coroutines.launch
import presentation.components.LoadingButton

// Usamos Voyager Screen para la navegación
data class DigitalPresenceEditScreen(val directorId: String) : Screen {

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
        var canalYoutube by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var redesSociales by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

        var isSaving by remember { mutableStateOf(false) }

        // Efecto para cargar los datos del director
        LaunchedEffect(directorId) {
            isLoadingData = true
            try {
                directorRepository.getDirector(directorId).collect { director ->
                    if (director != null) {
                        initialDirectorData = director
                        // Inicializa los estados del formulario con los datos cargados
                        canalYoutube = director.canalYoutube
                        email = director.email
                        redesSociales = director.redesSociales
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
                    title = { Text("Presencia Digital") },
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
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email de Contacto") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = canalYoutube,
                            onValueChange = { canalYoutube = it },
                            label = { Text("Canal de YouTube (URL)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = "URL") }
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Editor para el Map de Redes Sociales ---
                        DynamicMapEditor(
                            title = "Redes Sociales",
                            items = redesSociales,
                            onAddItem = { key, value ->
                                // Añade o actualiza la entrada en el Map
                                if (key.isNotBlank() && value.isNotBlank()) {
                                    redesSociales = redesSociales + (key to value)
                                }
                            },
                            onRemoveItem = { key ->
                                // Remueve la entrada por la clave
                                redesSociales = redesSociales.filterKeys { it != key }
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- Botón Guardar ---
                        LoadingButton(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    val updatedDirector = initialDirectorData!!.copy(
                                        canalYoutube = canalYoutube,
                                        email = email,
                                        redesSociales = redesSociales
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
 * Un Composable reutilizable para editar un Map<String, String>.
 * Muestra campos para una clave (ej: "Instagram") y un valor (ej: "url")
 * y una lista de entradas existentes con botones para eliminarlos.
 */
@Composable
private fun DynamicMapEditor(
    title: String,
    items: Map<String, String>,
    onAddItem: (key: String, value: String) -> Unit,
    onRemoveItem: (key: String) -> Unit
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // --- Campos para añadir nuevas entradas ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    label = { Text("Red (ej: Insta)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text("URL de tu perfil") },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }
            Button(
                onClick = {
                    if (newKey.isNotBlank() && newValue.isNotBlank()) {
                        onAddItem(newKey, newValue)
                        newKey = "" // Limpia los campos
                        newValue = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Añadir red social", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir")
            }


            // --- Lista de entradas existentes ---
            if (items.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }

            items.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onRemoveItem(key) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar red social", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (items.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aún no has añadido ninguna red social.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}