package utils

import androidx.compose.runtime.Composable


data class FileData(val bytes: ByteArray, val fileName: String)

@Composable
expect fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (FileData?) -> Unit
)
