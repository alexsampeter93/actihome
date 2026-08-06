# Créditos de las imágenes

## Fotografías de los alojamientos

Las diez fotografías del catálogo de ejemplo son de **[Unsplash](https://unsplash.com)**.

La [licencia de Unsplash](https://unsplash.com/license) permite usarlas y modificarlas, incluso con fines comerciales, y **no obliga a atribuir**. Se acredita igualmente: no cuesta nada y es lo justo con quien hizo la foto.

| Alojamiento | Autor | Foto |
|---|---|---|
| Nº 10001 · Casa Rural El Pinar | Abby Rurenko | [uOYak90r4L0](https://unsplash.com/photos/uOYak90r4L0) |
| Nº 10002 · Apartamento Playa Centro | Roberto Nickson | [tleCJiDOri0](https://unsplash.com/photos/tleCJiDOri0) |
| Nº 10003 · Villa Mediterráneo | Wes Fischer | [g39p1kDjvSY](https://unsplash.com/photos/g39p1kDjvSY) |
| Nº 10004 · Cabaña del Bosque | Li Yan | [cZOouJsXs8k](https://unsplash.com/photos/cZOouJsXs8k) |
| Nº 10005 · Loft Barrio Gótico | Andrea Davis | [nbI8gqbBaHo](https://unsplash.com/photos/nbI8gqbBaHo) |
| Nº 10006 · Casa Adosada Las Palmas | Bernard Hermant | [nM5-mS5eA8I](https://unsplash.com/photos/nM5-mS5eA8I) |
| Nº 10007 · Ático Malasaña | Alberto Castillo | [q-mx4mSkK9zeo](https://unsplash.com/photos/q-mx4mSkK9zeo) |
| Nº 10008 · Villa Costa del Sol | Webaliser | [_TPTXZd9mOo](https://unsplash.com/photos/_TPTXZd9mOo) |
| Nº 10009 · Casa Patio Andaluz | Maria Orlova | [b37mDyPzdJM](https://unsplash.com/photos/b37mDyPzdJM) |
| Nº 10010 · Refugio de Nieve | Baptx | [BTQWx51keUY](https://unsplash.com/photos/BTQWx51keUY) |

Las cuatro últimas (Fase 7.10) ya estaban descargadas en `assets/` desde el principio del proyecto, junto con el resto de las que no se usan — no hizo falta salir a buscar nada nuevo, solo revisar lo que ya había.

Los originales están en `assets/References/Alojamientos de ejemplo/` junto con el resto de las descargadas, que no se usan. Las que sí usa la aplicación viven reducidas en `src/main/resources/images/housings/`, con el nombre del código del alojamiento, y las genera [`GenerarAssets`](src/main/java/fp/project/actihome/ui/dev/GenerarAssets.java).

## Marca e ilustraciones

Olaz, el logotipo de CocoBrain, el icono de la aplicación y el fondo decorativo son **originales del proyecto** y no proceden de ninguna biblioteca externa.

## Tipografías

| Familia | Diseño | Licencia |
|---|---|---|
| **Fraunces** | Undercase Type (Phaedra Charles, Flavia Zimbardi) | SIL Open Font License 1.1 |
| **Archivo** | Omnibus-Type | SIL Open Font License 1.1 |

La OFL permite empaquetarlas dentro de la aplicación, que es lo que se hace en `src/main/resources/fonts/`.

> Esta tabla decía **Spectral y Manrope** hasta el 07-08-2026, aunque el cambio de familias fue en la Fase 8.2. Es la trampa de siempre: **un documento que describe un estado caduca en silencio**, porque nadie lo compila.

## Servicios externos

Todo lo que la aplicación consulta por internet, y bajo qué condiciones. **Ninguno pide clave de API**, que es exactamente el motivo por el que se eligieron: un secreto dentro del ejecutable se extrae descompilándolo.

| Servicio | Para qué | Condiciones |
|---|---|---|
| **OpenStreetMap** | Teselas del mapa de ubicación (F19) | Datos © colaboradores de OpenStreetMap, bajo [ODbL](https://www.openstreetmap.org/copyright). **La atribución es obligatoria** y se muestra bajo el mapa. Se respeta su [política de teselas](https://operations.osmfoundation.org/policies/tiles/): identificación por `User-Agent`, sin descarga por lotes y con caché local |
| **Open-Meteo** | Previsión meteorológica de la ficha (F18) | Uso libre no comercial, sin registro |
| **Open-Meteo Geocoding** | Convertir el nombre de un sitio en coordenadas (F17) | Igual que el anterior |
| **MyMemory** | Traducción de descripciones y reseñas (Fases 8.5 y 8.7) | Punto de acceso anónimo, 5.000 palabras al día |
| **Brevo** | Envío del correo de recuperación de contraseña (Fase 8.6) | **Opcional.** Solo se usa si hay credenciales en variables de entorno; sin ellas la aplicación va por el código de administrador |

> **Si esto llegara a tener uso real, lo primero a revisar es OpenStreetMap.** Sus servidores van con donaciones, y crecer sobre infraestructura donada no está bien: a partir de cierto volumen lo correcto es autoalojar las teselas o pagar a un proveedor.
