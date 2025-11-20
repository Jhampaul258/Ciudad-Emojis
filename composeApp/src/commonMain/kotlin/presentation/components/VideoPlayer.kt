package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun YouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier
)