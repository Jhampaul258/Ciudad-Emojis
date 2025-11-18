package presentation.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YouTubePlayer(
    modifier: Modifier,
    videoUrl: String
) {
    // Esta llamada ahora usará la función 'rememberVideoId' de commonMain
    val videoId = rememberVideoId(videoUrl)

    val htmlData = """
        <html style="margin:0;padding:0;">
        <body style="margin:0;padding:0; background-color:black;">
        <div style="position:relative; width:100%; height:0; padding-bottom:56.25%;">
            <iframe
                style="position:absolute; top:0; left:0; width:100%; height:100%;"
                src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&controls=1&fs=1"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen
            ></iframe>
        </div>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                loadData(htmlData, "text/html", "utf-8")
            }
        },
        modifier = modifier
    )
}

// ¡LA FUNCIÓN DUPLICADA 'rememberVideoId' HA SIDO ELIMINADA DE AQUÍ!
