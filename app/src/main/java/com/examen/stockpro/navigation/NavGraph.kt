package com.examen.stockpro.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.examen.stockpro.ui.screens.CatalogoInventarioScreen
import com.examen.stockpro.ui.screens.EdicionStockScreen
import com.examen.stockpro.ui.screens.IngresoOperarioScreen
import com.examen.stockpro.ui.screens.ReporteFinancieroScreen
import com.examen.stockpro.viewmodel.StockViewModel

@Composable
fun NavGraph(viewModel: StockViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "ingreso") {

        composable("ingreso") {
            IngresoOperarioScreen(
                onIngresar = { nombre ->
                    navController.navigate("catalogo/${Uri.encode(nombre)}")
                }
            )
        }

        composable(
            route = "catalogo/{nombre}",
            arguments = listOf(navArgument("nombre") { type = NavType.StringType })
        ) { backStackEntry ->
            val nombre = Uri.decode(backStackEntry.arguments?.getString("nombre") ?: "")
            CatalogoInventarioScreen(
                nombre = nombre,
                viewModel = viewModel,
                onProductoClick = { id ->
                    navController.navigate("edicion/$id")
                },
                onVerReporte = {
                    navController.navigate("reporte")
                }
            )
        }

        composable(
            route = "edicion/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            EdicionStockScreen(
                productoId = id,
                viewModel = viewModel,
                onGuardar = { navController.popBackStack() }
            )
        }

        composable("reporte") {
            ReporteFinancieroScreen(
                viewModel = viewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
