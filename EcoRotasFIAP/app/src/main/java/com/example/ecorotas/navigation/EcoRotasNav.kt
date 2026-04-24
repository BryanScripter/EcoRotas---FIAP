package com.example.ecorotas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ecorotas.data.Storage
import com.example.ecorotas.ui.screens.ComparacaoScreen
import com.example.ecorotas.ui.screens.HistoricoScreen
import com.example.ecorotas.ui.screens.HomeScreen
import com.example.ecorotas.ui.screens.LoginScreen
import com.example.ecorotas.ui.screens.MapaRotaScreen
import com.example.ecorotas.ui.screens.ResultadoScreen
import com.example.ecorotas.ui.screens.SignUpScreen
import com.example.ecorotas.ui.screens.SplashScreen

// Nomes das telas e helpers pra montar rota com argumentos (origin/destination/distance na URL).
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val NOVA_ROTA = "nova_rota"
    const val COMPARACAO = "comparacao/{origin}/{destination}/{distance}"
    const val RESULTADO = "resultado/{origin}/{destination}/{distance}"
    const val HISTORICO = "historico"

    fun comparacao(origin: String, destination: String, distance: Double) =
        "comparacao/${uriEncode(origin)}/${uriEncode(destination)}/$distance"

    fun resultado(origin: String, destination: String, distance: Double) =
        "resultado/${uriEncode(origin)}/${uriEncode(destination)}/$distance"

    private fun uriEncode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}

// Grafo de navegação: splash -> login ou home; home -> nova rota (mapa), histórico, logout; nova rota -> resultado; etc.
@Composable
fun EcoRotasNav(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val storage = remember(context) { Storage(context.applicationContext) }
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onNavigateToHome = {
                val session = storage.getSession()
                if (session != null) {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                } else {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            })
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                storage = storage,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToSignUp = { navController.navigate(Routes.SIGNUP) }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                storage = storage,
                onSuccess = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.SIGNUP) { inclusive = true } }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            val user = storage.getSession()
            HomeScreen(
                user = user,
                storage = storage,
                onLogout = {
                    storage.clearSession()
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onNovaRota = { navController.navigate(Routes.NOVA_ROTA) },
                onHistorico = { navController.navigate(Routes.HISTORICO) }
            )
        }

        composable(Routes.NOVA_ROTA) {
            MapaRotaScreen(
                onBack = { navController.popBackStack() },
                onVerResultado = { origin, destination, distance ->
                    navController.navigate(Routes.resultado(origin, destination, distance)) {
                        popUpTo(Routes.NOVA_ROTA) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.COMPARACAO,
            arguments = listOf(
                navArgument("origin") { type = NavType.StringType },
                navArgument("destination") { type = NavType.StringType },
                navArgument("distance") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val origin = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("origin") ?: "", "UTF-8")
            val destination = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("destination") ?: "", "UTF-8")
            val distance = (backStackEntry.arguments?.getString("distance") ?: "0").toDoubleOrNull() ?: 0.0
            ComparacaoScreen(
                origin = origin,
                destination = destination,
                distance = distance,
                onBack = { navController.popBackStack() },
                onVerResultado = {
                    navController.navigate(Routes.resultado(origin, destination, distance)) {
                        popUpTo(Routes.NOVA_ROTA) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULTADO,
            arguments = listOf(
                navArgument("origin") { type = NavType.StringType },
                navArgument("destination") { type = NavType.StringType },
                navArgument("distance") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val origin = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("origin") ?: "", "UTF-8")
            val destination = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("destination") ?: "", "UTF-8")
            val distance = (backStackEntry.arguments?.getString("distance") ?: "0").toDoubleOrNull() ?: 0.0
            ResultadoScreen(
                origin = origin,
                destination = destination,
                distance = distance,
                storage = storage,
                onNovaRota = {
                    navController.navigate(Routes.NOVA_ROTA) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.NOVA_ROTA) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HISTORICO) {
            HistoricoScreen(
                storage = storage,
                onBack = { navController.popBackStack() },
                onNovaRota = {
                    navController.navigate(Routes.NOVA_ROTA) {
                        popUpTo(Routes.HISTORICO) { inclusive = true }
                    }
                }
            )
        }
    }
}
