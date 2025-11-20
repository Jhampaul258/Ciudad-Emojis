package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.awt.Desktop
import java.net.URI

@Composable
actual fun YouTubePlayer(
    videoId: String,
    modifier: Modifier
) {
    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable {
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
            imageVector = Icons.Default.PlayCircleOutline,
            contentDescription = "Reproducir en Navegador",
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.5f)
        )
        Text(
            "Click para ver en navegador",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}