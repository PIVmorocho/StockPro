# StockPro — Documentación Completa del Sistema

## ¿Qué es StockPro?

StockPro es una aplicación Android para el control de inventario de bodega.
Permite a un operario revisar el catálogo de productos, actualizar el stock
de forma individual y consultar un reporte financiero con el capital total invertido.

---

## Tecnologías utilizadas

| Tecnología | Versión | Para qué se usa |
|---|---|---|
| Kotlin | 2.2.10 | Lenguaje principal |
| Jetpack Compose | BOM 2026.02.01 | UI declarativa |
| Material3 | (vía BOM) | Componentes visuales (Card, Button, etc.) |
| Navigation Compose | 2.8.7 | Navegación entre pantallas |
| ViewModel (Lifecycle) | 2.6.1 | Estado que sobrevive a la navegación |
| Android Gradle Plugin | 9.2.1 | Sistema de compilación |
| Min SDK | 24 (Android 7.0) | Dispositivos soportados |

---

## Arquitectura: MVVM

El proyecto sigue el patrón **MVVM (Model – View – ViewModel)**:

```
┌──────────────┐     lee/escribe     ┌─────────────────┐     contiene     ┌──────────┐
│    VIEW      │ ──────────────────► │   VIEWMODEL     │ ───────────────► │  MODEL   │
│  (Pantallas  │                     │  (StockViewModel)│                  │(Producto)│
│  Composable) │ ◄────────────────── │                  │                  │          │
└──────────────┘  recompone al       └─────────────────┘                  └──────────┘
                  detectar cambios
```

- **Model**: define la estructura de los datos.
- **ViewModel**: contiene la lógica y el estado. Las pantallas no calculan nada.
- **View**: muestra los datos y reacciona a los cambios automáticamente.

---

## Estructura de archivos

```
app/src/main/java/com/examen/stockpro/
│
├── MainActivity.kt                        ← Punto de entrada Android
│
├── model/
│   └── Producto.kt                        ← Data class del producto
│
├── viewmodel/
│   └── StockViewModel.kt                  ← Lógica + estado reactivo
│
├── navigation/
│   └── NavGraph.kt                        ← Rutas de navegación
│
└── ui/
    ├── screens/
    │   ├── IngresoOperarioScreen.kt        ← Pantalla 1
    │   ├── CatalogoInventarioScreen.kt     ← Pantalla 2
    │   ├── EdicionStockScreen.kt           ← Pantalla 3
    │   └── ReporteFinancieroScreen.kt      ← Pantalla 4
    └── theme/
        ├── Color.kt                        ← Colores Material3
        ├── Theme.kt                        ← Tema de la app
        └── Type.kt                         ← Tipografía
```

---

## Capa Model — `Producto.kt`

```kotlin
data class Producto(
    val id: String,          // Identificador único (ej: "P001")
    val nombre: String,      // Nombre del producto
    val descripcion: String, // Descripción detallada
    val precio: Double,      // Precio unitario en dólares
    val stockActual: Int     // Cantidad en bodega
)
```

**Puntos clave:**
- Es una `data class`: immutable por diseño (todos los campos son `val`).

  Esto es lo que permite que el sistema reactivo de Compose detecte el cambio.

---

## Capa ViewModel — `StockViewModel.kt`

Es el corazón de la aplicación. Contiene todos los datos y toda la lógica.
**Las pantallas nunca calculan nada — solo muestran lo que el ViewModel provee.**

### Estado reactivo

```kotlin
val productos = mutableStateListOf(
    Producto("P001", "Cemento Portland", "...", 12.50, 80),
    Producto("P002", "Varilla de Acero", "...",  8.75,  3),
    Producto("P003", "Pintura Látex",    "...", 18.00,  0),
    Producto("P004", "Tubo PVC 4\"",     "...",  6.20, 12),
    Producto("P005", "Cable AWG 12",     "...", 45.00,  2),
    Producto("P006", "Madera Pino 2x4",  "...",  5.80,  0),
    Producto("P007", "Cerámica 60x60",   "...", 22.00,  7),
    Producto("P008", "Alambre Recocido", "...",  3.50,  4)
)
```

`mutableStateListOf` es una lista observable: cuando cambia, Compose redibuja
automáticamente todas las pantallas que la estén leyendo.

### Funciones del ViewModel

| Función | Qué hace | Usada en |
|---|---|---|
| `obtenerProducto(id)` | Busca un producto por ID | Pantalla 3 |
| `actualizarStock(id, nuevaCantidad)` | Reemplaza el elemento en la lista | Pantalla 3 |
| `calcularValorTotalInventario()` | Suma `precio × stock` de todos | Pantalla 4 |
| `obtenerProductosEnRiesgo()` | Filtra productos con stock < 5 | Pantalla 2 |
| `totalProductosEnCero()` | Cuenta productos con stock = 0 | Pantalla 4 |



---

## Capa Navegación — `NavGraph.kt`

Define las 4 rutas de la aplicación. El ViewModel se crea **una sola vez** en
`MainActivity` y se pasa a todas las pantallas a través del NavGraph.

```
ingreso  ──►  catalogo/{nombre}  ──►  edicion/{id}
                      │
                      └──────────────►  reporte
```

---

## Capa View — Las 4 pantallas

### Pantalla 1 — `IngresoOperarioScreen`

**Función:** Recibir el nombre del operario antes de entrar al sistema.

**Componentes:**
- `OutlinedTextField` para escribir el nombre
- `Button` que solo se habilita si el nombre tiene 3 o más caracteres



**No usa el ViewModel** — solo captura el nombre y lo pasa como parámetro de ruta.

---

### Pantalla 2 — `CatalogoInventarioScreen`

**Función:** Mostrar la lista de productos con filtros y acceso a las otras pantallas.

**Componentes:**
- Encabezado con el nombre del operario (recibido por parámetro de ruta)
- `FilterChip` × 2: "Ver Todo" y "Stock Crítico"
- `LazyColumn` con `Card` por cada producto
- `ExtendedFloatingActionButton` para ir al Reporte



**Alerta visual de stock bajo:**
```kotlin
Text(
    text = producto.stockActual.toString(),
    color = if (producto.stockActual < 5) Color.Red
            else MaterialTheme.colorScheme.onSurface
)
```

---

### Pantalla 3 — `EdicionStockScreen`

**Función:** Ver el detalle de un producto y modificar su stock con botones + / −.

**Componentes:**
- Nombre y descripción del producto
- Número grande con el stock actual (se actualiza en tiempo real)
- `FilledIconButton` con ícono `−` (deshabilitado si stock = 0)
- `FilledIconButton` con ícono `+`
- `Button` "Guardar y Volver" que ejecuta `popBackStack()`

**Cómo obtiene el producto:**
```kotlin
// Lee del ViewModel — si la lista cambia, este valor se actualiza solo
val producto = viewModel.obtenerProducto(productoId)
```


```

**No hay un botón "guardar" real** — los cambios se aplican inmediatamente al
ViewModel. El botón "Guardar y Volver" simplemente hace `popBackStack()` para
volver a la Pantalla 2.

---

### Pantalla 4 — `ReporteFinancieroScreen`

**Función:** Mostrar un resumen financiero del inventario.

**Componentes:**
- `Card` principal con el capital total en dólares
- `Card` secundaria con el total de productos en stock cero (se vuelve roja si > 0)
- `OutlinedButton` para volver al catálogo

**Los cálculos se hacen exclusivamente en el ViewModel:**
```kotlin
// En la pantalla, solo se MUESTRA el resultado
val valorTotal  = viewModel.calcularValorTotalInventario()
val totalEnCero = viewModel.totalProductosEnCero()
```

---



---

## Flujo de uso completo

```
[Inicio]
   │
   ▼
Pantalla 1 — El operario escribe su nombre
   │  (nombre con ≥ 3 caracteres)
   │  navega a "catalogo/{nombre}"
   ▼
Pantalla 2 — Ve la lista de productos
   │  Puede filtrar por "Stock Crítico" (stock < 5 → texto rojo)
   │
   ├─► toca una Card ──► navega a "edicion/{id}"
   │                         ▼
   │                     Pantalla 3 — Edita stock con + / −
   │                         │  "Guardar y Volver" → popBackStack()
   │                         └──────────────────────────────┘
   │
   └─► presiona FAB ──► navega a "reporte"
                            ▼
                        Pantalla 4 — Ve capital total y productos en cero
                            │  "Volver al Catálogo" → popBackStack()
                            └──────────────────────────────────────────┘
```

---

## Dependencias del proyecto (`build.gradle.kts`)

```kotlin
dependencies {
    // Compose BOM: controla las versiones de todos los artefactos Compose
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)          // setContent, by viewModels()
    implementation(libs.androidx.compose.material3)         // Material3 UI
    implementation(libs.androidx.compose.material.icons.extended) // Icons (Add, Remove, List...)
    implementation(libs.androidx.compose.ui)                // Composables base
    implementation(libs.androidx.compose.ui.graphics)       // Color, Brush
    implementation(libs.androidx.compose.ui.tooling.preview)// @Preview
    implementation(libs.androidx.core.ktx)                  // Extensiones Kotlin para Android
    implementation(libs.androidx.lifecycle.runtime.ktx)     // ViewModel lifecycle
    implementation(libs.androidx.navigation.compose)        // NavHost, navController
}
```

---

## Principios de diseño aplicados

| Principio | Cómo se aplica en StockPro |
|---|---|
| **Single Source of Truth** | `productos` en el ViewModel es la única lista. No hay copias locales en las pantallas. |
| **Separación de responsabilidades** | Las pantallas solo muestran. El ViewModel calcula y guarda. |
| **Inmutabilidad** | `Producto` usa `val`. Modificar stock = crear copia con `.copy()`. |
| **Estado reactivo** | `mutableStateListOf` notifica a Compose automáticamente. |
| **ViewModel compartido** | Una instancia para todas las pantallas → datos consistentes. |
