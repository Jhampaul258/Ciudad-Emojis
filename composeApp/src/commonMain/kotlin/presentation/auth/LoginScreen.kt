package presentation.auth

import ciudademojis.composeapp.generated.resources.Res
import ciudademojis.composeapp.generated.resources.ic_google_logo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.FirebaseAuthRepository
import domain.GoogleSignInHandler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import presentation.components.CustomTextField
import presentation.components.LoadingButton
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import presentation.main.MainScreen
import utils.AppColors
import utils.translateError
object LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = remember { FirebaseAuthRepository() }
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var triggerGoogleSignIn by remember { mutableStateOf(false) }

        GoogleSignInHandler(
            triggerSignIn = triggerGoogleSignIn,
            onResult = { result ->
                triggerGoogleSignIn = false
                scope.launch {
                    isLoading = true
                    try {
                        if (result.idToken != null && result.accessToken != null) {
                            val firebaseResult = authRepository.signInWithGoogle(result.idToken, result.accessToken)
                            if (firebaseResult.isSuccess) {
                                isLoading = false
                                navigator.replaceAll(MainScreen)
                            } else {
                                val errorMessage = translateError(firebaseResult.exceptionOrNull()?.message)
                                snackbarController.showError(errorMessage)
                                isLoading = false
                            }
                        } else {
                            snackbarController.showError(result.error ?: "Error al obtener tokens de Google.")
                            isLoading = false
                        }
                    } finally {
                        if (isLoading) {
                            isLoading = false
                        }
                    }
                }
            }
        )
        Scaffold(
            snackbarHost = { ReusableSnackbarHost(controller = snackbarController) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bienvenido a Ciudad Emojis", style = MaterialTheme.typography.headlineMedium)


                Spacer(modifier = Modifier.height(15.dp))


                Spacer(modifier = Modifier.height(15.dp))
                OutlinedButton(
                    onClick = {
                        if (!isLoading) {
                            triggerGoogleSignIn = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), // Borde más sutil
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
//                        Icon(
//                            painter = painterResource(Res.drawable.ic_google_logo),
//                            contentDescription = "Logo de Google",
//                            modifier = Modifier.size(24.dp),
//                            tint = Color.Unspecified
//                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Iniciar sesión con Google", color = MaterialTheme.colorScheme.onBackground,)
                    }
                }
                Spacer(modifier = Modifier.height(25.dp))

            }
        }
    }
}