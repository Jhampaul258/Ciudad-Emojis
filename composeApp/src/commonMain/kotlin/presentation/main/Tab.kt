@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.main
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import presentation.guide.HistoryScreen
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

    @Composable override fun Content() { HomeScreen() }
}

object UploadTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Subir"
            val icon = rememberVectorPainter(Icons.Default.Upload)
            return remember { TabOptions(index = 1u, title = title, icon = icon) }
        }

    @Composable override fun Content() { UploadGuideScreen.Content() }
}

object HistoryTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Perfil"
            val icon = rememberVectorPainter(Icons.Default.Settings)
            return remember { TabOptions(index = 2u, title = title, icon = icon) }
        }

    @Composable override fun Content() { SettingsScreen.Content() }
}
