package domain

import androidx.compose.runtime.Composable

/**
 * Representa el resultado del flujo de inicio de sesión con Google.
 * @param idToken El token de ID de Google si el inicio de sesión fue exitoso, null si falló o fue cancelado.
 * @param error Mensaje de error si algo salió mal.
 */
data class GoogleSignInResult(val idToken: String?,val accessToken: String?, val error: String?)

/**
 * Una función Composable que espera una implementación de plataforma
 * para manejar el flujo de inicio de sesión con Google.
 *
 * @param triggerSignIn Un estado booleano. Cuando cambia a `true`, se inicia el flujo.
 * @param onResult Callback que se llama con el resultado (token o error) una vez que el flujo termina.
 */
@Composable
expect fun GoogleSignInHandler(
    triggerSignIn: Boolean,
    onResult: (GoogleSignInResult) -> Unit
)