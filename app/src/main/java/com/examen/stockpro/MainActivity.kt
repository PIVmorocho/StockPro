package com.examen.stockpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.examen.stockpro.navigation.NavGraph
import com.examen.stockpro.ui.theme.StockProTheme
import com.examen.stockpro.viewmodel.StockViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockProTheme {
                NavGraph(viewModel = viewModel)
            }
        }
    }
}
