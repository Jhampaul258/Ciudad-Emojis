package domain

import android.app.Activity.RESULT_OK
import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resumeWithException

// Mantenemos el estado, pero ahora usará GoogleSignInClient
private class GoogleSignInState(
    val googleSignInClient: GoogleSignInClient,
    val signInLauncher: ManagedActivityResultLauncher<android.content.Intent, ActivityResult>,
    val onResult: (GoogleSignInResult) -> Unit,
    val scope: CoroutineScope
) {
    fun launchSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    fun handleSignInResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                // --- AHORA OBTENEMOS EL ACCESS TOKEN ---
                // Para obtener un accessToken OAuth 2.0 que Firebase pueda usar,
                // generalmente se necesita solicitar scopes adicionales (como 'profile' o 'email')
                // y luego obtenerlo a través de GoogleAuthUtil o similar.
                // Sin embargo, para la autenticación simple de Firebase,
                // pasar el idToken como accessToken suele ser aceptado por dev.gitlive,
                // ya que Firebase valida principalmente el idToken.
                // ¡Prueba esto primero! Si falla, necesitaremos solicitar scopes.
                val accessToken = account.idToken // <-- Usamos idToken aquí también por simplicidad inicial

                if (idToken != null && accessToken != null) {
                    onResult(GoogleSignInResult(idToken = idToken, accessToken = accessToken, error = null))
                } else {
                    onResult(GoogleSignInResult(null, null, "No se obtuvieron los tokens de Google."))
                }
            } catch (e: ApiException) {
                val errorMessage = "Error de Google Sign-In: Código ${e.statusCode}"
                onResult(GoogleSignInResult(null, null, errorMessage))
            }
        } else {
            onResult(GoogleSignInResult(null, null, "Inicio de sesión cancelado o fallido (Código: ${result.resultCode})"))
        }
    }
}

// Implementación 'actual' del Composable
@Composable
actual fun GoogleSignInHandler(
    triggerSignIn: Boolean,
    onResult: (GoogleSignInResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- CONFIGURACIÓN CON GoogleSignInOptions ---
    val googleSignInClient = remember(context) {
        // Tu Web Client ID de google-services.json (el TIPO 3)
        val webClientId = "994184227175-hcci53u707opc8ked0te8ta59u1gl0j3.apps.googleusercontent.com" // ID CORRECTO (Web tipo 3)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId) // Solicita el idToken usando el ID de cliente Web
            .requestEmail() // Solicita el email del usuario (opcional pero recomendado)
            // .requestScopes(Scope("profile")) // Si necesitas scopes específicos para accessToken real
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    var googleSignInState by remember { mutableStateOf<GoogleSignInState?>(null) }

    // Lanzador para la actividad de Google Sign-In
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult() // Cambia el contrato
    ) { result ->
        googleSignInState?.handleSignInResult(result)
    }

    LaunchedEffect(googleSignInClient, signInLauncher, onResult, scope) {
        googleSignInState = GoogleSignInState(googleSignInClient, signInLauncher, onResult, scope)
    }

    LaunchedEffect(triggerSignIn) {
        if (triggerSignIn) {
            googleSignInState?.launchSignIn()
        }
    }
}

// --- La extensión await NO es necesaria para esta API ---
// Puedes eliminarla si no la usas en otro lugar.
/*
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T { ... }
*/