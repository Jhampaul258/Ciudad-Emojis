package utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Función 'expect' multiplataforma para decodificar un ByteArray en un ImageBitmap.
 * Cada plataforma (Android, Desktop, iOS) proveerá su implementación 'actual'.
 */
@Composable
expect fun ByteArray.toKmpImageBitmap(): ImageBitmap?
