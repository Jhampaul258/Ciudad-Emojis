package presentation.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import data.FirebaseAuthRepository
import presentation.auth.LoginScreen
import presentation.main.MainScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Navigation() {
    val authRepository = FirebaseAuthRepository()
    Navigator(
        screen = if (authRepository.getCurrentUserId() != null) MainScreen else LoginScreen
    ) { navigator ->
        FadeTransition(navigator)
    }
}