# Guía: Actualización Reactiva entre Pantallas con ViewModel y mutableStateListOf

## La pregunta a responder

> ¿Cómo lograste que al sumar stock en la Pantalla 3, la Pantalla 2 y la Pantalla 4
> se actualicen automáticamente sin perder los datos?

---

## 1. El problema que resuelve esta arquitectura

En una app Android tradicional (sin ViewModel ni estado reactivo), cada pantalla
vive de forma aislada. Si el usuario modifica un dato en la Pantalla 3, las
Pantallas 2 y 4 no se enteran porque cada una tiene su propia copia local de los
datos. Al volver, habría que recargar manualmente.

En StockPro esto no ocurre. El motivo es una combinación de dos piezas clave:
**ViewModel compartido** + **mutableStateListOf**.

---

## 2. Pieza 1 — El ViewModel como fuente única de verdad

### ¿Qué es un ViewModel?

Un `ViewModel` es una clase que sobrevive a los cambios de ciclo de vida de
Android (rotaciones de pantalla, navegación entre pantallas) y mantiene el estado
de la aplicación. No es destruida cuando el usuario navega de una pantalla a otra.

### Cómo se crea el ViewModel en StockPro

**`MainActivity.kt`**
```kotlin
class MainActivity : ComponentActivity() {

    // Se crea UNA SOLA instancia del ViewModel para toda la Activity
    private val viewModel: StockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockProTheme {
                // El ViewModel se pasa al NavGraph y desde ahí a TODAS las pantallas
                NavGraph(viewModel = viewModel)
            }
        }
    }
}
```

**`NavGraph.kt`**
```kotlin
@Composable
fun NavGraph(viewModel: StockViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "ingreso") {

        // Pantalla 2 recibe el MISMO viewModel
        composable("catalogo/{nombre}") { backStackEntry ->
            CatalogoInventarioScreen(
                viewModel = viewModel,   // <-- mismo objeto
                ...
            )
        }

        // Pantalla 3 recibe el MISMO viewModel
        composable("edicion/{id}") { backStackEntry ->
            EdicionStockScreen(
                viewModel = viewModel,   // <-- mismo objeto
                ...
            )
        }

        // Pantalla 4 recibe el MISMO viewModel
        composable("reporte") {
            ReporteFinancieroScreen(
                viewModel = viewModel,   // <-- mismo objeto
                ...
            )
        }
    }
}
```

**Conclusión de esta pieza:** Las tres pantallas comparten exactamente la misma
instancia del ViewModel. No hay tres copias de la lista — hay una sola, y todas
las pantallas la leen.

---

## 3. Pieza 2 — mutableStateListOf: la lista que Compose "observa"

### ¿Qué es mutableStateListOf?

Es una lista especial de Jetpack Compose que implementa el sistema de
**Snapshot State**. Cuando su contenido cambia, Compose detecta
automáticamente qué pantallas (Composables) leyeron esa lista, y las
**redibuja** con los nuevos valores. No hace falta llamar a `notifyDataSetChanged()`
ni ninguna otra función manual.

### Dónde se declara en StockPro

**`StockViewModel.kt`**
```kotlin
class StockViewModel : ViewModel() {

    // Esta lista es OBSERVABLE por Compose.
    // Cada vez que cambia, cualquier Composable que la haya leído se redibuja.
    val productos = mutableStateListOf(
        Producto("P001", "Cemento Portland",  ..., precio = 12.50, stockActual = 80),
        Producto("P002", "Varilla de Acero",  ..., precio = 8.75,  stockActual = 3),
        Producto("P003", "Pintura Látex",     ..., precio = 18.00, stockActual = 0),
        Producto("P004", "Tubo PVC 4\"",      ..., precio = 6.20,  stockActual = 12),
        Producto("P005", "Cable AWG 12",      ..., precio = 45.00, stockActual = 2),
        Producto("P006", "Madera Pino 2x4",   ..., precio = 5.80,  stockActual = 0),
        Producto("P007", "Cerámica 60x60",    ..., precio = 22.00, stockActual = 7),
        Producto("P008", "Alambre Recocido",  ..., precio = 3.50,  stockActual = 4)
    )
    ...
}
```

---

## 4. El mecanismo completo: qué ocurre al presionar "+"

### Paso 1 — El usuario presiona "+" en la Pantalla 3

**`EdicionStockScreen.kt`**
```kotlin
FilledIconButton(
    onClick = {
        // Se llama a la función del ViewModel pasando el nuevo stock
        viewModel.actualizarStock(producto.id, producto.stockActual + 1)
    }
) {
    Icon(Icons.Filled.Add, contentDescription = "Aumentar stock")
}
```

### Paso 2 — El ViewModel modifica la lista reactiva

**`StockViewModel.kt`**
```kotlin
fun actualizarStock(id: String, nuevaCantidad: Int) {
    val index = productos.indexOfFirst { it.id == id }
    if (index != -1) {
        // .copy() crea un nuevo objeto Producto con el stock actualizado.
        // Al asignar a productos[index], el SnapshotStateList detecta el cambio
        // y notifica a todos los Composables que leen esta lista.
        productos[index] = productos[index].copy(stockActual = nuevaCantidad)
    }
}
```

> **¿Por qué `.copy()` y no modificar directamente el campo?**
> Porque `Producto` es una `data class` con campos `val` (inmutables).
> La única forma de "cambiar" un campo es crear un nuevo objeto con los
> mismos valores excepto el que se quiere cambiar. Esto es intencional:
> el SnapshotStateList detecta el cambio porque se reemplazó el elemento
> en la posición `index`, no porque se mutó internamente un campo.

### Paso 3 — Compose notifica a los Composables suscritos

El sistema de Snapshot de Compose registra qué Composables leyeron `productos`
durante su última ejecución (composición). Al detectar que `productos[index]`
cambió, programa la recomposición de todas las funciones `@Composable` que
leyeron esa lista.

---

## 5. Por qué la Pantalla 2 se actualiza

**`CatalogoInventarioScreen.kt`**
```kotlin
@Composable
fun CatalogoInventarioScreen(viewModel: StockViewModel, ...) {

    // Esta línea LEE viewModel.productos (el SnapshotStateList).
    // Compose registra esta lectura.
    // Cuando productos cambie, esta pantalla se recompone automáticamente.
    val lista: List<Producto> = if (mostrarCriticos)
        viewModel.obtenerProductosEnRiesgo()   // también lee viewModel.productos
    else
        viewModel.productos                     // lectura directa

    ...

    LazyColumn {
        items(lista, key = { it.id }) { producto ->
            Text(
                text = producto.stockActual.toString(),
                // Si stock < 5, texto rojo — también se actualiza solo
                color = if (producto.stockActual < 5) Color.Red
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

Cuando el usuario regresa a la Pantalla 2, el Composable vuelve a ejecutarse
y lee `viewModel.productos` — que ya tiene el stock actualizado. El color rojo
del stock aparece o desaparece según el nuevo valor, sin código adicional.

---

## 6. Por qué la Pantalla 4 se actualiza

**`ReporteFinancieroScreen.kt`**
```kotlin
@Composable
fun ReporteFinancieroScreen(viewModel: StockViewModel, ...) {

    // Estas dos funciones leen viewModel.productos internamente.
    // Al ejecutarse en un contexto Composable, Compose las suscribe a la lista.
    val valorTotal  = viewModel.calcularValorTotalInventario()
    val totalEnCero = viewModel.totalProductosEnCero()

    ...

    Text(text = "$${String.format("%,.2f", valorTotal)}")   // Capital total
    Text(text = totalEnCero.toString())                      // Productos en cero
}
```

**`StockViewModel.kt`** — las funciones que leen la lista:
```kotlin
// Suma precio * stock de TODOS los productos
fun calcularValorTotalInventario(): Double =
    productos.sumOf { it.precio * it.stockActual }

// Cuenta los que tienen stock == 0
fun totalProductosEnCero(): Int =
    productos.count { it.stockActual == 0 }
```

Cuando el usuario navega a la Pantalla 4, estas funciones se ejecutan dentro
del Composable y leen `productos` — que ya contiene el stock modificado en la
Pantalla 3. El capital total y el conteo de ceros ya reflejan los cambios.

---

## 7. Diagrama del flujo completo

```
┌─────────────────────────────────────────────────────────┐
│                    MainActivity                          │
│  val viewModel: StockViewModel by viewModels()           │
│  (una sola instancia, vive toda la sesión)               │
└───────────────────────┬─────────────────────────────────┘
                        │ se pasa a NavGraph
                        ▼
┌─────────────────────────────────────────────────────────┐
│                    StockViewModel                        │
│  val productos = mutableStateListOf(...)  ← FUENTE      │
│                                             ÚNICA        │
│  fun actualizarStock(id, nuevaCantidad) {               │
│      productos[index] = productos[index]                 │
│                         .copy(stockActual = nuevaCantidad)│
│  }                      ↑                               │
└─────────────────────────┼───────────────────────────────┘
                          │ SnapshotStateList notifica cambio
          ┌───────────────┼──────────────────┐
          ▼               ▼                  ▼
   Pantalla 3       Pantalla 2          Pantalla 4
   (edita stock)    (lee productos)     (lee productos)
   ESCRIBE          SE REDIBUJA         SE REDIBUJA
                    automáticamente     automáticamente
```

---

## 8. Resumen en una oración

> El `ViewModel` garantiza que **los datos no se pierden** al navegar, y el
> `mutableStateListOf` garantiza que **cualquier pantalla que lea la lista
> se redibuje automáticamente** cuando sus datos cambian — sin código extra,
> sin callbacks, sin `notifyDataSetChanged()`.
