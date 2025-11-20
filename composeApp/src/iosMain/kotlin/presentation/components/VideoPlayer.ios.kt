package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun YouTubePlayer(videoId: String, modifier: Modifier) {
}
//package presentation.components
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.interop.UIKitView
//import platform.Foundation.NSURL
//import platform.Foundation.NSURLRequest
//import platform.WebKit.WKWebView
//import platform.WebKit.WKWebViewConfiguration
//
//@Composable
//actual fun YouTubePlayer(
//    videoId: String,
//    modifier: Modifier
//) {
//    val embedUrl = "https://www.youtube.com/embed/$videoId?playsinline=1"
//
//    UIKitView(
//        factory = {
//            val config = WKWebViewConfiguration().apply {
//                allowsInlineMediaPlayback = true
//                // mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
//            }
//            WKWebView(frame = platform.CoreGraphics.CGRectZero.readValue(), configuration = config)
//        },
//        modifier = modifier,
//        update = { webView ->
//            val url = NSURL(string = embedUrl)
//            val request = NSURLRequest(uRL = url)
//            webView.loadRequest(request)
//        }
//    )
//}