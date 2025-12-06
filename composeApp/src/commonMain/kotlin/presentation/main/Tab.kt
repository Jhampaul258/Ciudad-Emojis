//@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.main
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import data.DirectorRepository
import data.FirebaseAuthRepository
import domain.model.Director
import presentation.components.GuestRestrictionScreen
import presentation.guide.UploadGuideScreen
import presentation.settings.SettingsScreen

object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Inicio"
            val icon = rememberVectorPainter(Icons.Default.Home)
            return remember { TabOptions(index = 0u, title = title, icon = icon) }
        }

    @Composable override fun Content() { Navigator(HomeScreen()) }
}

object UploadTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Subir"
            val icon = rememberVectorPainter(Icons.Default.Add)
            return remember { TabOptions(index = 1u, title = title, icon = icon) }
        }

    @Composable override fun Content() { // 1. Verificar Autenticación
        val authRepository = remember { FirebaseAuthRepository() }
        val directorRepository = remember { DirectorRepository() }

        val userId = authRepository.getCurrentUserId()
        var currentUser by remember { mutableStateOf<Director?>(null) }

        // Cargar datos del usuario actual
        LaunchedEffect(userId) {
            if (userId != null) {
                directorRepository.getDirector(userId).collect { currentUser = it }
            }
        }

        // Lógica de Acceso
        when {
            userId == null -> {
                GuestRestrictionScreen(title = "Subir Contenido", message = "Regístrate para subir contenido.")
            }
            currentUser?.isBlocked == true -> { // <--- BLOQUEO
                GuestRestrictionScreen(
                    title = "Cuenta Bloqueada",
                    message = "Tu cuenta ha sido suspendida por un administrador. No puedes subir contenido."
                )
            }
            else -> {
                Navigator(UploadGuideScreen())
            }
        }
    }
}

object HistoryTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Perfil"
            val icon = rememberVectorPainter(Icons.Default.Settings)
            return remember { TabOptions(index = 2u, title = title, icon = icon) }
        }

    @Composable override fun Content() { val authRepository = remember { FirebaseAuthRepository() }
        val isGuest = authRepository.getCurrentUserId() == null

        if (isGuest) {
            GuestRestrictionScreen(
                title = "Mi Perfil",
                message = "Inicia sesión para gestionar tu perfil y ver tus estadísticas."
            )
        } else {
            SettingsScreen.Content()
        }
    }
}
