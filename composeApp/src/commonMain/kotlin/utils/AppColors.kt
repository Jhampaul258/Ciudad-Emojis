package utils

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AppColors {
    // Colores Base
    val BlackHigh = Color(0xFF121212) // Tu negro estilo cine
    val White = Color(0xFFFFFFFF)
    val Purple = Color(0xFF6200EE) // Ejemplo de primario

    // --- ESQUEMA MODO CLARO ---
    val LightColors = lightColorScheme(
        primary = Purple,
        background = Color(0xFFF5F5F5), // Gris muy claro
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1C1B1F), // Texto casi negro
        onSurface = Color(0xFF1C1B1F),
        secondary = Color(0xFF625B71)
    )

    // --- ESQUEMA MODO OSCURO ---
    val DarkColors = darkColorScheme(
        primary = Color(0xFFBB86FC), // Versión clara del primario para contraste
        background = BlackHigh,      // Tu fondo oscuro
        surface = Color(0xFF1E1E1E), // Un poco más claro que el fondo para tarjetas
        onBackground = Color(0xFFE6E1E5), // Texto casi blanco
        onSurface = Color(0xFFE6E1E5),
        secondary = Color(0xFFCCC2DC)
    )
}