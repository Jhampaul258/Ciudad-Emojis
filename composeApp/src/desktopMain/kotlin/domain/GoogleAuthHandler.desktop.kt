package domain

import androidx.compose.runtime.Composable

@Composable
actual fun GoogleSignInHandler(
    triggerSignIn: Boolean,
    onResult: (GoogleSignInResult) -> Unit
) {
}