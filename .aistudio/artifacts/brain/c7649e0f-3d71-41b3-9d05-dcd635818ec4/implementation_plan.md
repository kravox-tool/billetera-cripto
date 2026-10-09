# Optimización de Insets del Sistema y Fluidez Instantánea en el Menú de Tarjetas

Plan de optimización para corregir el solapamiento de la barra de estado del sistema Android y eliminar por completo la lentitud al abrir el menú de creación de tarjetas cripto.

### User Review & Critical Decisions

> [!IMPORTANT]
> Confirmaciones obtenidas en la fase de clarificación:
> - **Espaciado superior**: Aplicar margen automático con las áreas seguras (`WindowInsetsCompat.Type.systemBars()`) directamente en el contenedor nativo del WebView.
> - **Transición del menú**: Apertura instantánea y fluida eliminando `backdrop-filter: blur(...)` y optimizando capas GPU compuestas.

- **Confirmed Decision 1**: Margen superior automático gestionado por `OnApplyWindowInsetsListener` en `MainActivity.kt`, garantizando que la barra de estado y cámara nunca colisionen con el encabezado.
- **Confirmed Decision 2**: Eliminación del efecto de desenfoque de fondo pesado en CSS (`backdrop-filter`) reemplazándolo por una superposición oscura ligera de alto contraste acelerada por hardware, logrando apertura inmediata (<16 ms / 120 FPS).

---

### 1. Overview & Core Concept

- **Problema**: 
  1. En `MainActivity.kt`, el listener de insets sobrescribía el padding a `(0, 0, 0, 0)`, provocando que el encabezado de la app quedara debajo de los iconos del reloj, batería y red del sistema.
  2. En `style.css`, el modal utilizaba `backdrop-filter: blur(8px)`, lo que fuerza recomposiciones masivas de píxeles en el compositor de la GPU en WebView móvil, generando caída de fotogramas y retraso perceptible al tocar el botón de creación.
- **Solución**:
  1. Conectar los insets nativos de Android (`insets.top` e `insets.bottom`) al padding real del WebView.
  2. Sustituir `backdrop-filter: blur(8px)` por `background: rgba(5, 8, 15, 0.85)` con `opacity` y `transform` acelerados por hardware en capas aisladas.

---

### 2. User Experience & Visual Design

- **Encabezado y Barra de Estado**:
  - El encabezado "Billetera Cripto" desciende automáticamente por debajo de la barra de estado con separación visual limpia y legible.
  - La barra de navegación inferior del sistema respeta el margen inferior para que los botones y tarjetas no queden tapados por los gestos de Android.
- **Apertura de Modales (Crear / QR)**:
  - Apertura instantánea sin congelamientos ni tirones (0 ms de latencia percibida).
  - Animación suave de entrada mediante transformaciones puras en GPU (`translateY(0)` y `opacity: 1`).
  - Cero costo de rasterizado en segundo plano al evitar repintados por desenfoque.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Insets aplicados a nivel nativo en Android vs. solo CSS**:
  - *Chosen Approach*: Aplicar `view.setPadding(0, insets.top, 0, insets.bottom)` en `MainActivity.kt`.
  - *Why*: En WebViews de Android, `env(safe-area-inset-top)` dentro de CSS suele evaluar a `0px` si la ventana está configurada en modo full-bleed. Aplicar el padding en el contenedor WebView nativo garantiza compatibilidad del 100% en todos los dispositivos Android.
- **Decision 2: Optimización del fondo del modal**:
  - *Chosen Approach*: Fondo semitransparente oscuro `rgba(5, 8, 15, 0.85)` sin desenfoque gaussiano dinámico.
  - *Why*: El filtro gaussiano en capas completas de pantalla en WebViews causa caídas severas de FPS. El fondo oscuro puro preserva la estética cyberpunk con rendimiento a 120 FPS.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Android OS Window                    │
│   [ Status Bar: Clock, WiFi, Battery ] (insets.top)   │
└──────────────────────────┬─────────────────────────────┘
                           ▼ Padding aplicado en WebView
┌────────────────────────────────────────────────────────┐
│               Hardware-Accelerated WebView             │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Top Navigation Header ("Billetera Cripto")       │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Total Balance Card ($0.00 USD)                   │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Modal Sheet (Instant GPU Composite Layer)        │  │
│  │ - Opacity & TranslateY Transitions (120 FPS)     │  │
│  │ - Clean Alpha Overlay (No backdrop-filter blur)  │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────┬─────────────────────────────┘
                           ▼ Padding inferior seguro
┌────────────────────────────────────────────────────────┐
│      [ System Gesture / 3-Button Navigation Bar ]      │
└────────────────────────────────────────────────────────┘
```

#### Plan de Modificaciones

1. **`MainActivity.kt`**:
   - Actualizar el `OnApplyWindowInsetsListener` para aplicar `view.setPadding(0, insets.top, 0, insets.bottom)`.
2. **`app/src/main/assets/ui/style.css`**:
   - Eliminar `backdrop-filter: blur(8px)`.
   - Optimizar `.modal-overlay` con fondo opaco semitransparente ultra ligero.
   - Afinar las transiciones con `will-change: opacity, transform`.
3. **`app/src/main/assets/ui/app.js`**:
   - Optimizar `openCardModal` para evitar cálculos redundantes al abrir.
4. **Verificación**:
   - Compilación con `compile_applet` y ejecución de tests con `testDebugUnitTest`.
