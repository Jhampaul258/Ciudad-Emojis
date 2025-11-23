import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import presentation.navigation.Navigation
import utils.AppColors

@Composable
fun App() {
    // Detecta si el sistema está en modo oscuro
    val isDark = isSystemInDarkTheme()

    // Elige la paleta correcta
    val colorScheme = if (isDark) AppColors.DarkColors else AppColors.LightColors
    MaterialTheme (colorScheme = colorScheme) {
        Navigation()
    }
}