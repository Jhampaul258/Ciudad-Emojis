package utils

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@Composable
actual fun ByteArray.toKmpImageBitmap(): ImageBitmap? {
    return remember(this) {
        try {
            BitmapFactory.decodeByteArray(this, 0, this.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
