package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
expect fun YouTubePlayer(
    modifier: Modifier = Modifier,
    videoUrl: String,
)

/**
 * Una función de ayuda 'remember' para extraer el ID de video
 * de varias URL de YouTube.
 */
@Composable
internal fun rememberVideoId(youtubeUrl: String): String {
    return remember(youtubeUrl) {
        val patterns = listOf(
            "v=([a-zA-Z0-9_-]+)", // Formato: watch?v=...
            "embed/([a-zA-Z0-9_-]+)", // Formato: embed/...
            "youtu.be/([a-zA-Z0-9_-]+)" // Formato: youtu.be/...
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(youtubeUrl)
            if (match != null && match.groupValues.size > 1) {
                return@remember match.groupValues[1]
            }
        }

        // Si no se encuentra, devuelve una cadena vacía o un ID de video predeterminado
        ""
    }
}