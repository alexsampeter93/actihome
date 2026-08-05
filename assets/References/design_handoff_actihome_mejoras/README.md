# Handoff: ActiHome — Mejoras de UI sobre la app existente

## Overview
5 propuestas de mejora sobre pantallas ya construidas de ActiHome (app de alquiler/intercambio de alojamientos "por temporada"): Catálogo, Detalle de alojamiento, Reseñas, Intercambio (estado vacío) y Perfil & Ajustes. No es una app nueva — son mejoras puntuales de jerarquía visual y contenido sobre pantallas reales que el usuario ya tiene implementadas (ver capturas de referencia que el usuario puede adjuntar aparte).

## About the Design Files
Los archivos de esta carpeta son **referencias de diseño en HTML** — mockups estáticos que muestran el look deseado, no código de producción. La tarea es **recrear estos diseños dentro del entorno/código real de ActiHome** (su stack actual — probablemente una app de escritorio/web ya existente), respetando sus componentes, estado y datos reales, no copiar el HTML tal cual.

## Fidelity
**Alta fidelidad (hifi)** en estilo visual (colores, tipografía, espaciado) — pero el **contenido es de ejemplo** (nombres, precios, fechas) tomado de las capturas que compartió el usuario. Implementar con los datos reales de cada alojamiento/usuario/reserva.

## Screens / Views

### 1A — Catálogo (tarjeta de listado)
- **Propósito**: listar alojamientos disponibles, permitir comparar precio/rating de un vistazo.
- **Layout**: tarjeta en fila — imagen 280×170px a la izquierda, contenido a la derecha en columna.
- **Cambios sobre el original**:
  - Precio en `Spectral serif, 26px, color #3E7D3B` (antes texto normal pequeño) — debe ser el elemento más prominente tras el título.
  - Avatar circular de 22px con iniciales del anfitrión (color por usuario, ej. `#c9a15c` para M, `#9c6fc9` para L) junto al rating, reemplaza el texto suelto "de Marcos".
  - Badge estacional `Ideal en {estación}` — pill verde `#3E7D3B`, texto blanco 8px uppercase, esquina superior derecha de la imagen. Se calcula server/client-side comparando la estación actual con una etiqueta de "estación ideal" por alojamiento (nueva propiedad de datos).
  - Chips de amenidades reducidos a 2-3 más relevantes en vez de todas.

### 1B — Detalle de alojamiento
- **Propósito**: dar toda la información necesaria para decidir reservar, sin salir de la página.
- **Layout**: galería arriba (grid 2 columnas: 1 foto grande 1.4fr + 2 fotos apiladas 1fr, 280px alto), debajo grid de 2 columnas (contenido 1fr + sidebar 300px).
- **Componentes nuevos vs. original**:
  - Galería de fotos (antes solo 1 imagen). Última miniatura con overlay `rgba(0,0,0,.45)` + "+N fotos" si hay más.
  - Tarjeta de anfitrión: avatar 30px + nombre + rating + "responde en < 1 día" (dato nuevo a trackear o mockear con un valor fijo si no se registra aún).
  - Cita de reseña destacada: la reseña con mejor rating o más reciente, en `Spectral italic 18px`, con atribución debajo en uppercase 10.5px.
  - Sidebar con caja de precio + botón "Reservar" sticky, y mini-calendario del mes (7 columnas L-D) con días ocupados en `background: rgba(0,0,0,.08)` + `text-decoration: line-through`.

### 1C — Reseñas
- **Propósito**: dar una vista agregada antes de la lista larga.
- **Layout**: caja de resumen (grid 140px + 1fr) arriba de la lista.
- **Componentes nuevos**:
  - Media grande `Spectral 40px` a la izquierda con separador vertical.
  - Histograma de 5→1 estrellas: barras finas 5px de alto, `background rgba(0,0,0,.08)` con relleno `#3E7D3B` proporcional al % de reseñas de esa puntuación.
  - 2-3 tags calculados de las sub-notas por categoría (ubicación/servicio/wifi/comida/limpieza): la de mayor media con "↑" y la de menor con "↓".
  - Avatar circular de 26px por reseña (iniciales del autor).

### 1D — Intercambio (estado vacío)
- **Propósito**: explicar el mecanismo de intercambio y motivar a publicar, no solo bloquear con un botón.
- **Layout**: título → 3 pasos en grid de 3 columnas → caja "intercambios abiertos ahora mismo" (lista con bullets verdes) → CTA final centrado.
- **Contenido**: los 3 pasos son fijos (Publica tu alojamiento / Propón un intercambio / El otro anfitrión acepta). La lista de "intercambios abiertos" debe alimentarse de intercambios reales pendientes de otros usuarios (requiere endpoint que liste propuestas abiertas públicas, sin exponer datos privados del solicitante más allá del alojamiento y qué busca).

### 1E — Perfil & Ajustes
- **Propósito**: unificar "Editar perfil" y "Ajustes" (antes 2 pantallas separadas con formulario plano) en una sola vista organizada por tarjetas temáticas.
- **Layout**: cabecera con avatar 64px circular + nombre + rol/ciudad + botón "Cambiar a admin" a la derecha. Debajo, 2 tarjetas: "Datos personales" (grid 2×2 de campos, mostrados como texto con línea inferior — clic para editar inline) y "Preferencias" (filas con label izquierda / control derecha: selector de estación, toggle lista/cuadrícula, switch de partículas decorativas).
- **Interacción**: switch de partículas es un toggle real (`background #3E7D3B` cuando activo, círculo blanco que se desplaza), no un checkbox de formulario.
- Enlaces de "Cambiar contraseña" y "Cerrar sesión" quedan como acciones de texto simple debajo, sin necesitar el menú desplegable largo que existía antes.

## Interactions & Behavior
- Los toggles/switches (vista de catálogo, partículas) cambian de estado al click y persisten en preferencias de usuario.
- El botón "Reservar" en 1B lleva al flujo de reserva ya existente.
- Los días ocupados del calendario en 1B son de solo lectura (no seleccionables) en esta vista de detalle — solo informativos.
- Las 2 filas de 1A y la lista de 1D navegan a sus pantallas de detalle correspondientes al click.

## State Management
- Catálogo: necesita conocer la estación activa del usuario para calcular qué tarjetas llevan el badge "Ideal en {estación}".
- Detalle: necesita el listado de reservas del mes para pintar el mini-calendario de disponibilidad, y la reseña destacada (mayor rating o más reciente) para la cita.
- Reseñas: necesita agregación de las sub-notas por categoría para el histograma y los tags de "lo mejor/lo peor".
- Intercambio: necesita un listado de intercambios abiertos de otros usuarios (público, sin datos sensibles) además del estado propio.

## Design Tokens
- Verde header/oscuro: `#1F2A18`
- Verde acento (precio, badges, CTAs secundarios): `#3E7D3B`
- Fondo crema (paleta primavera): `#F3F1E6`
- Texto principal: `#232B1B`
- Texto muted/labels: `#6f6656`
- Bordes sutiles: `rgba(0,0,0,.08)` a `rgba(0,0,0,.15)`
- Partículas decorativas: `#E39FC0` a baja opacidad (.4–.6), símbolos `✳` y `•`
- Tipografía de títulos/precios: `Spectral` (serif, Google Fonts, pesos 400/500/600, cursiva 400/500 para citas)
- Tipografía de cuerpo/UI: `Segoe UI` / system-ui sans-serif
- Radios: la app no usa esquinas redondeadas (todo a escuadra) excepto avatares y switches (circulares/pill) — mantener esa convención

## Assets
- Fotos de alojamientos y avatar de usuario están como placeholders (`<image-slot>` en el archivo de diseño) — sustituir por las fotos reales de cada alojamiento/usuario.
- No hay iconografía compleja: solo texto, formas geométricas simples y símbolos unicode (✳, •, ★, ●) coloreados vía CSS.

## Files
- `ActiHome Mejoras.dc.html` — fuente del diseño (Design Component, no ejecutable fuera de este entorno).
- `ActiHome Mejoras (standalone).html` — versión autocontenida: ábrela en cualquier navegador para ver los 5 mockups tal cual se diseñaron.
