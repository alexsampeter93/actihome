# Auditoría técnica de ActiHome

**Fecha:** 05-08-2026 · **Alcance:** 145 ficheros de código, 25.401 líneas, 10 clases de test, 120 tests.

> **Los números de arriba son los del 05-08-2026 y hoy se han quedado cortos** (223 ficheros, 41.862 líneas, 21 clases de test, 180 tests). No se actualizan a propósito: esto es una foto fechada, y una foto que se retoca deja de ser una foto. Para el estado de hoy, [README.md](README.md).

Este documento es una foto del estado técnico, no una lista de tareas. Lo que se corrigió durante la propia auditoría va marcado ✅; lo que queda abierto lleva una valoración honesta de si merece la pena tocarlo.

---

## 1. Resumen

**El proyecto está sano.** La arquitectura de tres capas se respeta sin excepciones, los tests pasan, no hay hilos mal usados, no hay `printStackTrace` ni `catch` que se traguen errores en silencio (quedaban dos, corregidos aquí), y el sistema de diseño está aplicado de forma consistente en las 23 pantallas.

Los hallazgos serios son **tres**, y ninguno es un fallo de programación:

| # | Hallazgo | Gravedad | Estado |
|---|---|---|---|
| A1 | **Todo corre en el hilo de la interfaz**, incluidas las consultas a la base de datos | Media hoy, **alta a partir de la Fase 8.5** | Abierto — decisión pendiente |
| A2 | Dependencias de diciembre de 2019, con versiones que los escáneres marcan como vulnerables | Media | Abierto — plan propuesto |
| A3 | Infraestructura muerta: dos beans, una dependencia y una librería de servlets | Baja | ✅ Corregido |

---

## 2. A1 — Todo corre en el hilo de la interfaz

### Qué se encontró

En 145 ficheros **no hay un solo `SwingWorker`, `Thread`, `ExecutorService` ni `CompletableFuture`**. Todos los `invokeLater` que existen son para lo contrario de lo habitual: aplazar algo *dentro* del hilo de Swing (poner el foco, reiniciar un scroll después de que el layout haya medido). Los cinco temporizadores son `javax.swing.Timer`, que también dispara en el hilo de la interfaz.

Eso significa que **cada consulta a la base de datos ocurre mientras la ventana está bloqueada**. Concretamente:

- El catálogo carga los alojamientos y luego, uno por uno, cuenta las reseñas de cada uno (N+1).
- `BackupService` ejecuta el `BACKUP TO` de H2 sobre el fichero entero.
- `TrayReminders` consulta las reservas cada cinco minutos.

### Por qué hoy no se nota

H2 es una base local, en el mismo proceso, con diez alojamientos. Una consulta tarda microsegundos. La decisión de no meter concurrencia está además **razonada por escrito** en `SettingsFrame` y es defendible: introducir hilos donde no hacen falta añade una clase entera de fallos (condiciones de carrera, componentes tocados fuera del EDT) a cambio de nada.

### Por qué cambia con la Fase 8.5

**Una llamada HTTP no es una consulta a H2.** MyMemory está en internet, y una petición de red puede tardar segundos o no contestar nunca. Hecha en el hilo de la interfaz, **la aplicación entera se congela**: no repinta, no responde al ratón, y Windows la marca como «no responde». No es un riesgo teórico, es el comportamiento garantizado en cuanto la red vaya lenta.

**Recomendación:** la Fase 8.5 debe introducir concurrencia, pero **solo para la llamada de red**, no como refactor general. Un `SwingWorker` en el punto exacto donde se traduce, con `doInBackground` haciendo la petición y `done` volcando el resultado en la pantalla ya de vuelta en el EDT. El resto del proyecto se queda como está, y la decisión de no usar hilos para la base de datos sigue siendo correcta.

---

## 3. A2 — Dependencias

### Versiones resueltas

| Librería | Versión | Publicada | Nota |
|---|---|---|---|
| Spring Boot | 2.2.2.RELEASE | dic. 2019 | Fin de soporte hace años |
| Spring Framework | 5.2.2.RELEASE | dic. 2019 | |
| Hibernate | 5.4.9.Final | 2019 | |
| **H2** | **1.4.200** | 2019 | **CVE-2021-42392 y CVE-2022-23221** |
| snakeyaml | 1.25 | 2019 | CVE-2022-1471 |
| mysql-connector-java | 8.0.18 | 2019 | |
| FlatLaf / MigLayout | 3.7.2 / 11.4.2 | actuales | Añadidas en el rediseño |

### Cuánto de esto es un riesgo real, y cuánto es ruido de escáner

Hay que separar las dos cosas, porque tratar todo como crítico es tan poco útil como ignorarlo.

**Las CVE de H2 no son explotables en esta aplicación tal y como está.** Las dos permiten ejecutar código a través de la URL de conexión JDBC o de la consola web de H2. Aquí la consola web **no se activa**, y la URL de conexión no la controla el usuario: viene de `application.yaml`, dentro del jar. Quien pudiera modificarla ya tiene el jar en su ordenador, así que no gana nada que no tuviera. Lo mismo con snakeyaml: solo parsea el `application.yaml` del propio proyecto, que es un fichero de confianza.

**Lo que sí es un riesgo real es distinto y menos llamativo:** son versiones sin mantenimiento. Cualquier vulnerabilidad descubierta de aquí en adelante **no se va a parchear**, y cualquier auditoría automática (un `dependabot`, un escáner de un empleador, un cliente que pida un informe) marcará el proyecto en rojo sin entrar en matices. Para un proyecto de portfolio eso importa: es lo primero que mira quien lo revise.

### Recomendación

Subir a **Spring Boot 3.x** en una fase propia, no de paso. No es un cambio de número: Spring Boot 3 exige **Java 17** (el proyecto está en 11) y mueve todo `javax.*` a `jakarta.*`, lo que toca cada entidad JPA del proyecto. A cambio arrastra Hibernate 6, H2 2.x y el resto de la cadena a versiones vivas de golpe.

**El proyecto está en buena posición para hacerlo**: 120 tests en verde son exactamente la red de seguridad que ese salto necesita, y `PLAN.md` ya lo tenía anotado como horizonte desde la Fase 1.0.

**Aviso concreto sobre H2 1.4.200 → 2.x**: el formato del fichero de base de datos cambió y **no es compatible hacia atrás**. Un `~/.actihome/actihome.mv.db` creado con la versión actual no lo abre H2 2.x. Como en este proyecto todo el esquema es idempotente y `data.sql` siembra los datos de ejemplo, la migración se resuelve borrando el fichero — pero eso hay que decidirlo y documentarlo, no descubrirlo al arrancar.

---

## 4. A3 — Infraestructura muerta ✅ corregido

Tres beans y una dependencia conectados entre sí y **a nada más**:

- `ActihomeApplication.messageSource()` apuntaba a `classpath:messages`, un fichero que **no existe** ni ha existido nunca en el proyecto.
- Su único consumidor era `validator()`, que montaba un `LocalValidatorFactoryBean` para validación por anotaciones.
- Y la aplicación no tiene **ni una sola** anotación de Bean Validation: ni un `@NotNull`, ni un `@Size`, ni un `@Valid` en 145 ficheros. Toda la validación es código explícito en servicios y pantallas.
- Con ellos venía `spring-boot-starter-validation`, y con esa dependencia **`tomcat-embed-el`**: una implementación de Expression Language para contenedores de servlets, dentro de una aplicación de escritorio que no levanta ningún servidor web.

Merece anotarse **por qué duró tanto**: cada pieza justificaba a la siguiente —el validador necesita un origen de mensajes; el origen de mensajes tiene un consumidor— y leídas por separado las dos parecían tener sentido. Lo que no lo tenía era el conjunto. Es la forma más difícil de detectar del código muerto: no un método suelto sin usar, sino **un pequeño sistema coherente conectado a nada**.

Efecto secundario positivo: las subclases de `InstanceException` llevan claves i18n (`"project.entities.user"`) que nunca se resolvían justamente porque el `messageSource` no tenía fichero detrás. Ahora al menos ya no hay una infraestructura que aparente resolverlas.

---

## 5. Hallazgos menores

### ✅ Corregidos en esta auditoría

| Qué | Detalle |
|---|---|
| **Dos `catch (Exception)` en producción** | `CLAUDE.md` daba B11 por cerrado («ya no queda ningún `catch (Exception)` en la capa de UI») y quedaban dos, los dos contando reseñas. El método que capturaban solo declara `InstanceNotFoundException`, así que el genérico se tragaba además cualquier fallo de programación — y se manifestaba como una ficha diciendo «0 reseñas» teniendo varias, sin ninguna traza. Estrechados. |
| **Cuatro archivos de fuente empaquetados y nunca dibujados** | `Typography.sansBold()`, `sansExtraBold()` y `serifSemiBold()` no se llamaban desde ningún sitio, pero cargaban sus `.ttf` al arrancar. 590 KB, un tercio del peso tipográfico del jar. Retirados con sus métodos: de 1,8 MB a 1,2 MB. |
| **Mezcla de idioma publicada** | La frase estacional de invierno en español decía «Nieve, chimenea y luz de **dusk**». |

### Abiertos, con recomendación

| # | Qué | Recomendación |
|---|---|---|
| ~~B4~~ | ~~`ReviewServiceImpl` compara entidades con `!=` en vez de `equals`.~~ ✅ **Corregido aquí.** Comparar con `!=` pregunta si son *el mismo objeto en memoria*, no si son la misma fila. Ahora compara por `getId().equals(...)`. **Sin test de regresión nuevo, y conviene decir por qué**: dentro de una transacción JPA garantiza una única instancia por fila, así que un test *no puede* distinguir las dos versiones — reproducir el fallo exigiría montar dos contextos de persistencia, que es más andamiaje del que justifica una línea. Los 24 tests existentes cubren que el camino feliz y el rechazo siguen funcionando. | — |
| ~~B13~~ | ~~`CLAUDE.md` lo listaba como abierto («ningún frame llama a `setDefaultCloseOperation`»).~~ ✅ **No era cierto**: `Navigator` pone `EXIT_ON_CLOSE` en cada ventana que abre, y desde la Fase 6 pasan por él las 17 pantallas. El código estaba bien; la ficha se quedó vieja. Cerrado en la documentación. | — |
| — | `Layout.Regimen`, `esCompacto()` y `esGrande()`: un sistema de tres regímenes de anchura **diseñado, documentado y que ninguna pantalla llegó a adoptar**. La adaptabilidad se resolvió por otra vía (rangos `min:pref:max`, `FilaFluida`, `Rescate`). | **No borrar.** Es distinto del código muerto de arriba: no es un descuido, es una intención sin aplicar. Y la Fase 8.4 la necesita de verdad — «amenidades reducidas a 2-3» y el hero contraíble son exactamente decisiones de régimen. |
| — | `Space.all()` y `Space.symmetric()` sin usar. | **Dejar.** Seis líneas, coste cero, API coherente con el resto de `Space`. |
| — | `ShowHousingsFrame` tiene **1.136 líneas**, más del doble que el siguiente fichero. | **Vigilar, no partir ahora.** Es la pantalla más compleja (hero, filtros, dos vistas, comparación, estados vacíos) y partirla por partirla movería el problema. Si la Fase 8.4 le añade más, entonces sí. |
| — | N+1 al contar reseñas, tanto en el catálogo como en los dos paneles de agregación. | **Aceptado y documentado.** Con diez alojamientos es irrelevante. La solución correcta cuando crezca es un método de servicio que devuelva los recuentos de una vez, **no** que la UI hable con los DAOs. |

---

## 6. Lo que está bien y conviene no romper

No todo informe tiene que ser una lista de defectos. Estas cuatro cosas están mejor de lo habitual y son las que sostienen el resto:

1. **La regla de dependencia se cumple sin excepciones.** Ningún servicio importa nada de `ui`. Es lo que hizo posible rediseñar las 23 pantallas enteras sin tocar la lógica de negocio, y lo que abarataría el día de mañana partir la aplicación en cliente y servidor.
2. **Las comprobaciones automáticas son de verdad automáticas.** `MedirResponsive`, `MedirContraste` y ahora `MedirGlifos` devuelven código de salida distinto de cero. Y `MedirResponsive` trae **autocontrol**: si a un tamaño imposible dijera «todo bien», sabríamos que el detector no detecta. Muy poca gente escribe eso.
3. **Cobertura razonable donde importa.** 120 tests, todos sobre la capa de servicio, que es donde vive el negocio. La ratio es buena: 25 tests para 5 métodos en `ReservationService`, 24 para 7 en `ReviewService`.
4. **Las decisiones están escritas.** `LEARNING_LOG.md` con 58 entradas y los ADR explican *por qué*, no solo *qué*. Media hora leyéndolo ahorra días de arqueología — y esta auditoría se apoyó en él constantemente.

---

## 7. Qué haría, por orden

1. ~~**B4**~~ ✅ hecho durante la auditoría.
2. ~~**Cerrar B13 en `CLAUDE.md`**~~ ✅ hecho durante la auditoría.
3. **Concurrencia solo para la llamada de red de la Fase 8.5** — sin esto, la traducción congela la aplicación. Es lo siguiente.
4. **Spring Boot 3 + Java 17**, en una fase propia y con los 120 tests como red. Es lo único de esta lista que cuesta días en vez de minutos, y también lo único que cambia cómo se ve el proyecto desde fuera.

---

## 8. Nota sobre el método

Tres de los hallazgos de este informe —los dos `catch (Exception)`, B13 y la infraestructura muerta— tienen la misma forma: **la documentación afirmaba algo que el código ya no cumplía, en las dos direcciones**. `CLAUDE.md` daba B11 por cerrado y quedaban dos supervivientes; daba B13 por abierto y estaba resuelto hacía fases.

No es descuido de quien lo escribió: es lo que le pasa a cualquier documento que se actualiza al terminar una tarea y no se vuelve a leer entero. La conclusión práctica es que **un documento de estado hay que verificarlo contra el código de vez en cuando, no solo mantenerlo**, y que la forma barata de hacerlo es exactamente esta: un `grep` por cada afirmación comprobable que contenga.
