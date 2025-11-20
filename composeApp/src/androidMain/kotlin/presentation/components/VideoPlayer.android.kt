package presentation.components

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun YouTubePlayer(
    videoId: String,
    modifier: Modifier
) {
    // Copiamos el HTML exacto del ejemplo, inyectando tu ID de video
    val videoHtml = "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/$videoId?si=eNAb0A2l3iqBIsNo\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>"

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // Configuración de layout para que ocupe el espacio del Box en Compose
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 1. Habilitamos JavaScript (Igual que en el ejemplo)
                settings.javaScriptEnabled = true

                // (Opcional pero recomendado para Compose) Evita problemas de renderizado
                settings.domStorageEnabled = true

                // 2. Usamos WebChromeClient (Igual que en el ejemplo)
                webChromeClient = WebChromeClient()

                // 3. Cargamos la data (Igual que en el ejemplo)
                // Nota: loadData a veces requiere codificación base64 para ciertos caracteres,
                // pero aquí usamos el método simple tal cual el ejemplo.
                loadData(videoHtml, "text/html", "utf-8")
            }
        }
    )
}