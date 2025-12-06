package presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale // <--- IMPORTANTE
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage // <--- IMPORTANTE
import data.DirectorRepository
import data.FirebaseAuthRepository
import domain.model.Director
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import presentation.auth.LoginScreen
import presentation.profile_edit.BasicInfoEditScreen
import presentation.profile_edit.DigitalPresenceEditScreen
import presentation.profile_edit.DirectorContentScreen
import presentation.profile_edit.PresentationEditScreen
import presentation.profile_edit.RecognitionEditScreen
import presentation.profile_edit.SupportEditScreen

object SettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val parentNavigator = navigator.parent ?: navigator

        val authRepository = remember { FirebaseAuthRepository() }
        val directorRepository = remember { DirectorRepository() }
        val scope = rememberCoroutineScope()

        val directorId = authRepository.getCurrentUserId()
        var director by remember { mutableStateOf<Director?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(directorId) {
            if (directorId != null) {
                isLoading = true
                try {
                    directorRepository.getDirector(directorId).collect {
                        director = it
                        isLoading = false
                    }
                } catch (e: Exception) {
                    println("Error al cargar director: ${e.message}")
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }

        // Lógica de completado (sin cambios)
        val isBasicInfoComplete = director != null && director!!.name.isNotBlank() && director!!.universidad.isNotBlank()
        val isPresentationComplete = director != null && director!!.biografia.isNotBlank()
        val isRecognitionComplete = director != null && (director!!.festivales.isNotEmpty() || director!!.premios.isNotEmpty())
        val isDigitalPresenceComplete = director != null && (director!!.email.isNotBlank() || director!!.canalYoutube.isNotBlank() || director!!.redesSociales.isNotEmpty())
        val isSupportComplete = director != null && (director!!.yapeNumero.isNotBlank() || director!!.plinNumero.isNotBlank())

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Mi Perfil") })
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (directorId == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No se pudo cargar el perfil.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    LogoutButton(parentNavigator, authRepository, scope)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileHeader(director)

                    Spacer(Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileMenuItem("Información Básica", isBasicInfoComplete) { parentNavigator.push(BasicInfoEditScreen(directorId)) }
                        ProfileMenuItem("Presentación del director", isPresentationComplete) { parentNavigator.push(PresentationEditScreen(directorId)) }
                        ProfileMenuItem("Reconocimientos y experiencias", isRecognitionComplete) { parentNavigator.push(RecognitionEditScreen(directorId)) }
                        ProfileMenuItem("Presencia Digital/ redes sociales", isDigitalPresenceComplete) { parentNavigator.push(DigitalPresenceEditScreen(directorId)) }
                        ProfileMenuItem("Apoyo y Financiamiento", isSupportComplete) { parentNavigator.push(SupportEditScreen(directorId)) }
                        ProfileMenuItem("Mis Películas y Series", isSupportComplete) { parentNavigator.push(DirectorContentScreen()) }
                    }

                    Spacer(Modifier.height(16.dp))
                    LogoutButton(parentNavigator, authRepository, scope)
                }
            }
        }
    }
}

/**
 * Header del perfil actualizado para mostrar la foto de Google
 */
@Composable
private fun ProfileHeader(director: Director?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // VERIFICAMOS SI HAY FOTO DE PERFIL
            if (!director?.fotoPerfilUrl.isNullOrBlank()) {
                AsyncImage(
                    model = director!!.fotoPerfilUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Si no hay foto, mostramos la inicial como antes
                Text(
                    text = director?.name?.firstOrNull()?.toString() ?: "P",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = director?.name ?: "Usuario",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileMenuItem(text: String, isComplete: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = if (isComplete) "Completado" else "Pendiente",
                tint = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LogoutButton(navigator: cafe.adriel.voyager.navigator.Navigator, authRepository: FirebaseAuthRepository, scope: CoroutineScope) {
    Button(
        onClick = {
            scope.launch {
                authRepository.logout()
                authRepository.signOutGoogle()
                navigator.replaceAll(LoginScreen)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Text("Cerrar Sesión")
    }
}