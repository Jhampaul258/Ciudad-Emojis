package utils

import androidx.compose.runtime.Composable

@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (FileData?) -> Unit
) {
}