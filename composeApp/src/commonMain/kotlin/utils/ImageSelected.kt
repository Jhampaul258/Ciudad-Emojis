package utils

import androidx.compose.runtime.Composable

// --- ImageSource enum ha sido ELIMINADO ---

/**
 * Un Composable 'expect' que proporciona una función para lanzar
 * el selector de galería de imágenes.
 *
 * @param onImageSelected Callback que se invoca con los bytes de la imagen
 * y un nombre de archivo (ej: "photo.jpg") si la selección es exitosa.
 * Se invoca con null si el usuario cancela.
 * @return Una función lambda `() -> Unit`. Llama a esta función
 * para abrir la galería.
 */
@Composable
expect fun rememberImagePicker(
    onImageSelected: (FileData?) -> Unit
): () -> Unit // <-- CAMBIADO: Ya no devuelve (ImageSource) -> Unit