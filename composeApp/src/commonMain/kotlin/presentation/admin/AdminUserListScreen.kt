package presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import data.DirectorRepository
import domain.model.Director
import kotlinx.coroutines.launch

class AdminUserListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val directorRepository = remember { DirectorRepository() }
        val scope = rememberCoroutineScope()

        // Estado de la lista
        var allDirectors by remember { mutableStateOf<List<Director>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        // Cargar todos los directores
        LaunchedEffect(Unit) {
            directorRepository.getAllDirectors().collect {
                allDirectors = it
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Administrar Usuarios") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(allDirectors) { director ->
                        UserAdminItem(
                            director = director,
                            onBlockToggle = { isBlocked ->
                                scope.launch {
                                    // Actualizar solo el campo isBlocked en Firebase
                                    val updated = director.copy(isBlocked = isBlocked)
                                    directorRepository.createOrUpdateDirector(updated)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun UserAdminItem(director: Director, onBlockToggle: (Boolean) -> Unit) {
    ListItem(
        leadingContent = {
            if (director.fotoPerfilUrl.isNotBlank()) {
                AsyncImage(
                    model = director.fotoPerfilUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.size(40.dp).background(Color.Gray, CircleShape))
            }
        },
        headlineContent = { Text(director.name) },
        supportingContent = {
            Text(if (director.isAdmin) "ADMINISTRADOR" else director.email,
                color = if(director.isAdmin) MaterialTheme.colorScheme.primary else Color.Gray)
        },
        trailingContent = {
            // No permitimos bloquear a otros admins para evitar problemas
            if (!director.isAdmin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (director.isBlocked) "Bloqueado" else "Activo", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = director.isBlocked,
                        onCheckedChange = { onBlockToggle(it) },
                        thumbContent = {
                            Icon(
                                imageVector = if (director.isBlocked) Icons.Default.Close else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }
        }
    )
}