package presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarController(
    val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    var isError by mutableStateOf(false)
        private set

    fun showSuccess(message: String) {
        isError = false
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun showError(message: String) {
        isError = true
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
fun rememberSnackbarController(): SnackbarController {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    return remember(snackbarHostState, coroutineScope) {
        SnackbarController(snackbarHostState, coroutineScope)
    }
}

@Composable
fun ReusableSnackbarHost(
    controller: SnackbarController,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = controller.snackbarHostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = if (controller.isError) MaterialTheme.colorScheme.errorContainer else Color(0xFF4CAF50),
            contentColor = if (controller.isError) MaterialTheme.colorScheme.onErrorContainer else Color.White
        )
    }
}