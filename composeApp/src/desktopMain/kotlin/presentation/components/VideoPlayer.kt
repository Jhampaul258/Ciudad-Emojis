package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
actual fun YouTubePlayer(
    modifier: Modifier,
    videoUrl: String
) {
    // En lugar de un reproductor, mostramos un 'placeholder' que abre el navegador.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                // Abre la URL de YouTube en el navegador por defecto
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(videoUrl))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircleFilled,
            contentDescription = "Reproducir video",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(64.dp)
        )
    }
}