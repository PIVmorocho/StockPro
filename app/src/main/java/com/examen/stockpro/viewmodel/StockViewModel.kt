package com.examen.stockpro.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.examen.stockpro.model.Producto

class StockViewModel : ViewModel() {

    val productos = mutableStateListOf(
        Producto("P001", "Cemento Portland", "Saco de 50 kg, ideal para construcción general y obras civiles.", 12.50, 80),
        Producto("P002", "Varilla de Acero 1/2\"", "Varilla corrugada de 6 m, alta resistencia para estructuras.", 8.75, 3),
        Producto("P003", "Pintura Látex Blanca", "Galón de 4 L, rendimiento interior/exterior, secado rápido.", 18.00, 0),
        Producto("P004", "Tubo PVC 4\"", "Tubo sanitario de 3 m para desagüe y alcantarillado.", 6.20, 12),
        Producto("P005", "Cable Eléctrico AWG 12", "Rollo de 100 m, conductor de cobre, uso residencial.", 45.00, 2),
        Producto("P006", "Madera Pino 2x4", "Tabla de 2.44 m, madera seca y tratada contra humedad.", 5.80, 0),
        Producto("P007", "Cerámica 60x60 cm", "Caja de 1.44 m², color beige, acabado mate antideslizante.", 22.00, 7),
        Producto("P008", "Alambre Recocido #18", "Rollo de 1 kg para amarre de varillas en obra.", 3.50, 4)
    )

    fun obtenerProducto(id: String): Producto? =
        productos.find { it.id == id }

    fun actualizarStock(id: String, nuevaCantidad: Int) {
        val index = productos.indexOfFirst { it.id == id }
        if (index != -1) {
            productos[index] = productos[index].copy(stockActual = nuevaCantidad)
        }
    }

    fun calcularValorTotalInventario(): Double =
        productos.sumOf { it.precio * it.stockActual }

    fun obtenerProductosEnRiesgo(): List<Producto> =
        productos.filter { it.stockActual < 5 }

    fun totalProductosEnCero(): Int =
        productos.count { it.stockActual == 0 }
}
