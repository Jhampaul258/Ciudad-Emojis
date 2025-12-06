//@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.main
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import data.FirebaseAuthRepository
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
        val isGuest = authRepository.getCurrentUserId() == null

        if (isGuest) {
            // 2. Si es invitado, mostramos la restricción
            GuestRestrictionScreen(
                title = "Subir Contenido",
                message = "Para subir películas o series, necesitas registrarte como director."
            )
        } else {
            // 3. Si es usuario, mostramos el contenido normal
            Navigator(UploadGuideScreen())
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
