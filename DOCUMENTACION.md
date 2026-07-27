# DOCUMENTACIÓN COMPLETA — ActiHome

> Esta guía explica el proyecto desde cero, como si nunca hubieras visto código Java ni arquitecturas de software. Cada concepto se explica antes de entrar en detalle técnico.

---

## 1. ¿Qué es ActiHome?

**ActiHome** es una aplicación de escritorio (se abre como un programa en tu ordenador, no en el navegador) que sirve para **gestionar y reservar alojamientos turísticos** — piensa en ella como una versión simplificada de Booking.com o Airbnb, pero que se instala y se ejecuta localmente.

### ¿Qué puede hacer?

- Registrarse e iniciar sesión como usuario
- Ver todos los alojamientos disponibles
- Reservar un alojamiento para unas fechas concretas
- Hacer el check-in cuando llegas al alojamiento
- Publicar reseñas y valorar alojamientos
- (Si eres administrador) Publicar y gestionar alojamientos

---

## 2. Stack Tecnológico

> El "stack tecnológico" es el conjunto de herramientas, lenguajes y frameworks usados para construir la aplicación.

| Tecnología | ¿Para qué sirve? |
|---|---|
| **Java 11** | Lenguaje de programación principal. Todo el código está escrito en Java. |
| **Spring Boot 2.2.2** | Framework que simplifica enormemente crear aplicaciones Java. Gestiona la configuración, la inyección de dependencias, las transacciones de base de datos, etc. |
| **Spring Data JPA / Hibernate** | Permite trabajar con la base de datos usando objetos Java en lugar de SQL puro. |
| **Swing (Java)** | La librería de Java para crear interfaces gráficas de escritorio (ventanas, botones, tablas...). |
| **MySQL** | Base de datos relacional donde se guardan todos los datos: usuarios, alojamientos, reservas, reseñas. |
| **Maven** | Herramienta de construcción del proyecto. Gestiona las dependencias (librerías externas) y compila el código. |
| **BCrypt** | Algoritmo de encriptación para guardar contraseñas de forma segura. |

---

## 3. Arquitectura del Proyecto

> La "arquitectura" es cómo está organizado el código. ActiHome usa una arquitectura en **capas**, donde cada capa tiene una responsabilidad concreta y solo habla con la capa de abajo.

```
┌─────────────────────────────────────────────────────────┐
│                     CAPA UI (Interfaz)                   │
│  LoginFrame, ShowHousingsFrame, HousingDetailsFrame...   │
│  Aquí está todo lo visual: ventanas, botones, tablas     │
└──────────────────────────┬──────────────────────────────┘
                           │ llama a
┌──────────────────────────▼──────────────────────────────┐
│               CAPA DE SERVICIO (Lógica)                  │
│  UserService, HousingService, ReservationService...      │
│  Aquí viven las reglas de negocio: validaciones,         │
│  cálculos de precio, comprobación de permisos...         │
└──────────────────────────┬──────────────────────────────┘
                           │ llama a
┌──────────────────────────▼──────────────────────────────┐
│               CAPA DAO (Acceso a datos)                  │
│  UserDao, HousingDao, ReservationDao, ReviewDao          │
│  Aquí se realizan las consultas a la base de datos.      │
│  Spring genera el SQL automáticamente.                   │
└──────────────────────────┬──────────────────────────────┘
                           │ consulta
┌──────────────────────────▼──────────────────────────────┐
│                  BASE DE DATOS MySQL                     │
│  Tablas: USERS, HOUSINGS, RESERVATIONS, REVIEWS          │
│  Los datos persisten aquí aunque se cierre la app.       │
└─────────────────────────────────────────────────────────┘
```

### ¿Por qué capas?

- **Separación de responsabilidades**: cada parte del código hace solo una cosa.
- **Mantenibilidad**: si cambias la base de datos, solo tocas los DAOs. Si cambias la UI, solo tocas los Frames.
- **Testabilidad**: puedes probar la lógica (servicios) sin necesidad de la interfaz gráfica.

---

## 4. Estructura de Carpetas

```
actihome/
├── pom.xml                              ← Configuración Maven (dependencias)
├── src/
│   ├── main/
│   │   ├── java/fp/project/actihome/
│   │   │   ├── ActihomeApplication.java ← PUNTO DE ENTRADA de la app
│   │   │   ├── model/
│   │   │   │   ├── entities/            ← Clases que representan las tablas BD
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Housing.java
│   │   │   │   │   ├── Reservation.java
│   │   │   │   │   └── Review.java
│   │   │   │   ├── services/            ← Lógica de negocio
│   │   │   │   │   ├── UserService.java (interfaz)
│   │   │   │   │   ├── UserServiceImpl.java (implementación)
│   │   │   │   │   ├── HousingService.java
│   │   │   │   │   ├── HousingServiceImpl.java
│   │   │   │   │   ├── ReservationService.java
│   │   │   │   │   ├── ReservationServiceImpl.java
│   │   │   │   │   ├── ReviewService.java
│   │   │   │   │   ├── ReviewServiceImpl.java
│   │   │   │   │   └── PermissionChecker.java
│   │   │   │   └── exceptions/          ← Errores personalizados
│   │   │   └── ui/                      ← Ventanas Swing
│   │   │       ├── SessionManager.java
│   │   │       ├── HeaderPanel.java
│   │   │       ├── LoginFrame.java
│   │   │       ├── ShowHousingsFrame.java
│   │   │       └── ... (14 frames más)
│   │   └── resources/
│   │       ├── application.yaml         ← Configuración BD y Spring
│   │       └── schema.sql               ← Script de creación de tablas
│   └── test/                            ← Tests automatizados
```

---

## 5. Base de Datos

> La base de datos es donde se guardan todos los datos de forma permanente. MySQL organiza los datos en "tablas", que son como hojas de cálculo.

### Tabla `USERS` — Usuarios del sistema

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador único, se genera automáticamente |
| `username` | VARCHAR(50) | Nombre de usuario único |
| `password` | VARCHAR(200) | Contraseña encriptada con BCrypt |
| `name` | VARCHAR(20) | Nombre real |
| `surname` | VARCHAR(20) | Apellido |
| `locality` | VARCHAR(20) | Ciudad/localidad |
| `phoneNumber` | INT | Teléfono de contacto |
| `email` | VARCHAR(500) | Correo electrónico |
| `birthDate` | DATETIME | Fecha de nacimiento |
| `role` | TINYINT | 0 = ADMIN, 1 = CUSTOMER |

### Tabla `HOUSINGS` — Alojamientos

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador único |
| `housingCode` | BIGINT (UNIQUE) | Código único del alojamiento |
| `type` | VARCHAR(30) | Tipo: Casa, Apartamento, Villa... |
| `numberOfRooms` | INTEGER | Número de habitaciones (mínimo 1) |
| `pricePerNight` | DECIMAL(10,2) | Precio por noche en euros |
| `description` | VARCHAR(500) | Descripción del alojamiento |
| `breakfast` | BOOLEAN | ¿Incluye desayuno? |
| `lunch` | BOOLEAN | ¿Incluye almuerzo? |
| `dinner` | BOOLEAN | ¿Incluye cena? |
| `score` | DOUBLE | Puntuación media de las reseñas |
| `available` | BOOLEAN | ¿Está disponible para reservar? |
| `location` | VARCHAR(40) | Ubicación |
| `ownerId` | BIGINT (FK) | ID del usuario propietario |

### Tabla `RESERVATIONS` — Reservas

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador único |
| `reservationCode` | BIGINT | Código secreto para el check-in |
| `checkIn` | DATETIME | Fecha de entrada |
| `checkOut` | DATETIME | Fecha de salida |
| `paymentMethod` | VARCHAR(50) | Número de tarjeta de crédito |
| `reservationDate` | DATETIME | Cuándo se hizo la reserva |
| `totalPrice` | DECIMAL | Precio total (precio/noche × noches) |
| `checkedIn` | BOOLEAN | ¿Ya se hizo el check-in? |
| `customerId` | BIGINT (FK) | ID del cliente |
| `housingId` | BIGINT (FK) | ID del alojamiento |

### Tabla `REVIEWS` — Reseñas

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT (PK) | Identificador único |
| `title` | VARCHAR(50) | Título de la reseña |
| `body` | VARCHAR(500) | Texto de la reseña |
| `locationScore` | DOUBLE | Puntuación ubicación (0-5) |
| `serviceScore` | DOUBLE | Puntuación servicio (0-5) |
| `wifiScore` | DOUBLE | Puntuación wifi (0-5) |
| `foodScore` | DOUBLE | Puntuación comida (0-5) |
| `cleaningScore` | DOUBLE | Puntuación limpieza (0-5) |
| `totalScore` | DOUBLE | Media de las 5 puntuaciones |
| `publicationDate` | DATETIME | Fecha de publicación |
| `authorId` | BIGINT (FK) | ID del autor |
| `housingId` | BIGINT (FK) | ID del alojamiento reseñado |

---

## 6. Entidades (Modelo de Datos)

> Una "entidad" es una clase Java anotada con `@Entity` que representa directamente una tabla de la base de datos. Cada objeto Java = una fila en la tabla.

### `User.java`

```java
@Entity
@Table(name = "USERS")
public class User {
    @Id @GeneratedValue
    private Long id;
    private String username;
    private String password;   // siempre encriptada
    private String name;
    private String surname;
    private String locality;
    private int phoneNumber;
    private String email;
    private LocalDateTime birthDate;
    @Enumerated(EnumType.ORDINAL)
    private RoleType role;     // ADMIN o CUSTOMER
}
```

El enum `RoleType` tiene dos valores:
- `ADMIN` — gestiona alojamientos, puede intercambiar propiedades
- `CUSTOMER` — puede reservar y escribir reseñas

### `Housing.java`

```java
@Entity
@Table(name = "HOUSINGS")
public class Housing {
    @Id @GeneratedValue
    private Long id;
    private Long housingCode;
    private String type;
    private int numberOfRooms;
    private BigDecimal pricePerNight;
    private String description;
    private boolean breakfast, lunch, dinner;
    private double score;
    private boolean available;
    private String location;
    @ManyToOne(fetch = EAGER)
    private User owner;        // relación: un usuario puede tener muchos alojamientos
}
```

### `Reservation.java`

```java
@Entity
@Table(name = "RESERVATIONS")
public class Reservation {
    @Id @GeneratedValue
    private Long id;
    private Long reservationCode;  // código generado aleatoriamente
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String paymentMethod;  // número de tarjeta
    private LocalDateTime reservationDate;
    private BigDecimal totalPrice;
    private boolean checkedIn;
    @ManyToOne(fetch = EAGER)
    private User customer;
    @ManyToOne(fetch = EAGER)
    private Housing housing;
}
```

### `Review.java`

```java
@Entity
@Table(name = "REVIEWS")
public class Review {
    @Id @GeneratedValue
    private Long id;
    private String title;
    private String body;
    private double locationScore;   // 0.0 - 5.0
    private double serviceScore;
    private double wifiScore;
    private double foodScore;
    private double cleaningScore;
    private double totalScore;      // media calculada automáticamente
    private LocalDateTime publicationDate;
    @ManyToOne(fetch = EAGER)
    private User author;
    @ManyToOne(fetch = EAGER)
    private Housing housing;
}
```

---

## 7. DAOs (Acceso a Datos)

> Un DAO (**D**ata **A**ccess **O**bject) es una interfaz que define cómo consultar la base de datos. Spring genera el SQL automáticamente a partir del nombre del método — no hay que escribir SQL manualmente.

### `UserDao`
```java
public interface UserDao extends PagingAndSortingRepository<User, Long> {
    boolean existsByUsername(String username);   // ¿Existe ya este username?
    Optional<User> findByUsername(String username); // Busca usuario por username
}
```

### `HousingDao`
```java
public interface HousingDao extends PagingAndSortingRepository<Housing, Long> {
    Optional<Housing> findById(Long id);
    Optional<Housing> findByHousingCode(Long code);
    boolean existsByHousingCode(Long code);
    Iterable<Housing> findAllBy();
    Iterable<Housing> findHousingsByType(String type);           // filtro por tipo
    Iterable<Housing> findHousingsByNumberOfRooms(int minimum);  // filtro por habitaciones
}
```

### `ReservationDao`
```java
public interface ReservationDao extends PagingAndSortingRepository<Reservation, Long> {
    List<Reservation> findByCustomerIdOrderByReservationDateDesc(Long customerId);
    Optional<Reservation> findById(Long id);
}
```

### `ReviewDao`
```java
public interface ReviewDao extends PagingAndSortingRepository<Review, Long> {
    boolean existsByAuthorIdAndHousingId(Long authorId, Long housingId);
    Optional<Review> findById(Long id);
    List<Review> findByHousingIdOrderByPublicationDateDesc(Long housingId);
}
```

---

## 8. Servicios (Lógica de Negocio)

> Los servicios contienen las **reglas de negocio**: validaciones, cálculos, comprobaciones de permisos. Cada servicio tiene una interfaz (qué métodos hay) y una implementación (cómo funcionan).

### `UserService` — Gestión de usuarios

| Método | Qué hace |
|---|---|
| `signUp(User user)` | Registra un nuevo usuario. Encripta la contraseña antes de guardarla. Lanza error si el username ya existe. |
| `login(username, password)` | Valida credenciales. Compara la contraseña introducida con el hash BCrypt almacenado. |
| `loginFromId(Long id)` | Recupera un usuario por su ID (útil al reabrir ventanas). |
| `updateProfile(...)` | Actualiza datos personales. Verifica que el nuevo username no esté ya en uso. |
| `changePassword(...)` | Cambia la contraseña. Exige introducir la contraseña actual para confirmar. |

### `HousingService` — Gestión de alojamientos

| Método | Qué hace |
|---|---|
| `uploadHousing(...)` | Crea un nuevo alojamiento. Solo puede hacerlo un ADMIN. Valida: código único, mínimo 1 habitación, precio >= 0. |
| `findHousing(Long id)` | Devuelve un alojamiento por ID. |
| `showHousings()` | Devuelve todos los alojamientos del sistema. |
| `updateHousing(...)` | Actualiza datos de un alojamiento. Solo el propietario (que además es ADMIN) puede hacerlo. |
| `filterHousingsByType(String type)` | Filtra alojamientos por tipo (Casa, Apartamento...). |
| `filterHousingsByMinimumRooms(int min)` | Filtra alojamientos con al menos N habitaciones. |
| `tradeHousings(...)` | Intercambia la propiedad de dos alojamientos entre dos ADMINs. Falla si alguno está reservado. |

### `ReservationService` — Gestión de reservas

| Método | Qué hace |
|---|---|
| `reserveHousing(...)` | Crea una reserva. Validaciones: el usuario debe ser CUSTOMER, tarjeta de 16 dígitos, check-in >= hoy, check-out >= check-in + 1 día. Calcula precio total y marca el alojamiento como no disponible. |
| `showMyReservations(Long customerId)` | Devuelve todas las reservas de un usuario, ordenadas por fecha desc. |
| `doCheckIn(...)` | Realiza el check-in. Exige: fecha actual >= fecha check-in, el código de reserva coincide, no se haya hecho check-in antes. |

### `ReviewService` — Gestión de reseñas

| Método | Qué hace |
|---|---|
| `publishReview(...)` | Publica una reseña. Solo CUSTOMER. Valida puntuaciones entre 0 y 5. Un usuario solo puede reseñar un alojamiento una vez. Actualiza el score medio del alojamiento. |
| `findReview(Long id)` | Devuelve una reseña por ID. |
| `showHousingReviews(Long housingId)` | Devuelve todas las reseñas de un alojamiento ordenadas por fecha. |
| `updateReview(...)` | Edita una reseña. Solo el autor puede hacerlo. Recalcula el score medio del alojamiento. |

---

## 9. Roles y Permisos

> Hay dos tipos de usuario. El rol determina qué puede hacer cada uno en la aplicación.

### ADMIN

| Acción | ¿Puede? |
|---|---|
| Publicar alojamientos | ✅ |
| Actualizar sus alojamientos | ✅ |
| Intercambiar alojamientos con otro ADMIN | ✅ |
| Ver todos los alojamientos | ✅ |
| Reservar alojamientos | ❌ |
| Escribir reseñas | ❌ |

### CUSTOMER

| Acción | ¿Puede? |
|---|---|
| Ver todos los alojamientos | ✅ |
| Reservar alojamientos | ✅ |
| Ver sus reservas | ✅ |
| Hacer check-in | ✅ |
| Escribir reseñas | ✅ |
| Publicar alojamientos | ❌ |

---

## 10. Excepciones Personalizadas

> En Java, cuando algo sale mal se lanza una "excepción". ActiHome tiene 20 excepciones propias para representar errores de negocio concretos.

| Excepción | Cuándo se lanza |
|---|---|
| `InstanceNotFoundException` | No se encuentra el usuario/alojamiento/reserva/reseña |
| `DuplicateInstanceException` | Ya existe ese username o código de alojamiento |
| `IncorrectLoginException` | Username o contraseña incorrectos al hacer login |
| `WrongPasswordException` | Contraseña actual incorrecta al intentar cambiarla |
| `NotAuthorizedUserException` | El rol del usuario no le permite hacer esa acción |
| `NotTheOwnerException` | Intenta modificar un alojamiento que no es suyo |
| `NotTheAuthorException` | Intenta editar una reseña que no es suya |
| `NotMyReservationException` | Intenta hacer check-in en una reserva ajena |
| `LessThanOneRoomException` | Se intenta crear un alojamiento con 0 habitaciones |
| `NegativePrizeException` | Se intenta poner un precio negativo |
| `AlreadyReservedException` | Se intenta reservar un alojamiento ya reservado |
| `AlreadyPublishedException` | Se intenta reseñar un alojamiento que ya reseñaste |
| `AlreadyCheckedInException` | Se intenta hacer check-in cuando ya se hizo |
| `CannotCheckInException` | La fecha de check-in aún no ha llegado |
| `CheckOutMustBeOneDayAfterException` | El check-out es antes del check-in + 1 día |
| `MustBeTodayOrAfterException` | La fecha de check-in está en el pasado |
| `CodeDoesNotMatchException` | El código de reserva introducido es incorrecto |
| `WrongCreditCardNumberException` | La tarjeta no tiene exactamente 16 dígitos |
| `ScoreOutOfBoundsException` | La puntuación está fuera del rango 0-5 |

---

## 11. Interfaz Gráfica — Los 17 Frames

> Un "Frame" es una ventana de la aplicación. Están implementados con **Java Swing**, la librería estándar de Java para aplicaciones de escritorio. Cada frame es una clase Java que extiende `JFrame`.

### `ActihomeApplication.java` — Punto de entrada

```
Al arrancar la app:
  1. Desactiva el modo headless de AWT (necesario para mostrar ventanas)
  2. Configura Spring Boot como aplicación de escritorio (tipo NONE)
  3. Lanza LoginFrame en el hilo de eventos de Swing (EDT)
```

### `SessionManager.java` — Gestión de sesión

Singleton que guarda el usuario que tiene la sesión iniciada. Métodos:
- `login(User)` — guarda el usuario en sesión
- `logout()` — elimina la sesión
- `getLoggedInUser()` — devuelve el usuario actual
- `isUserLoggedIn()` — comprueba si hay sesión activa

### `HeaderPanel.java` — Cabecera de navegación

Panel reutilizado en todas las ventanas. Contiene:
- Botón "ActiHome" (izquierda) → vuelve a `ShowHousingsFrame`
- Botón con el nombre del usuario (derecha) → menú desplegable con:
  - Editar perfil
  - Cambiar contraseña
  - Cerrar sesión

---

### Ventanas de Autenticación

#### `LoginFrame` — Inicio de sesión
- **Tamaño**: 500×500 px
- **Campos**: username, password
- **Botón**: "Iniciar sesión" → valida credenciales y abre `ShowHousingsFrame`
- **Link**: "¿Es tu primera vez aquí? Regístrate" → abre `SignUpFrame`

#### `SignUpFrame` — Registro
- **Tamaño**: 500×500 px
- **Campos**: username, password, nombre, apellido, localidad, teléfono, email, fecha nacimiento, rol
- **Botón**: "Registrarse" → crea usuario y vuelve a `LoginFrame`

---

### Ventanas de Alojamientos

#### `ShowHousingsFrame` — Lista principal de alojamientos
- **Pantalla principal** tras el login
- Tabla con columnas: Código, Propietario, Tipo, Ubicación
- Doble clic en una fila → abre `HousingDetailsFrame`
- ADMIN: botón "Registrar alojamiento" → abre `UploadHousingFrame`
- CUSTOMER: botón "Ver mis reservas" → abre `ShowMyReservationsFrame`

#### `HousingDetailsFrame` — Detalle de un alojamiento
- Muestra: código, tipo, puntuación, habitaciones, precio/noche, descripción, comidas, disponibilidad, ubicación
- Botón "Ver reseñas" → abre `ShowReviewsFrame`
- (Si es el propietario ADMIN) Botón "Actualizar" → abre `UpdateHousingFrame`
- (Si es CUSTOMER) Botón "Reservar" → abre `ReserveHousingFrame`
- (Si es ADMIN propietario) Botón "Intercambiar" → abre `TradeHousingsFrame`

#### `UploadHousingFrame` — Crear alojamiento (solo ADMIN)
- Campos: código, tipo, habitaciones, precio, descripción, desayuno/almuerzo/cena, ubicación
- Solo accesible para usuarios ADMIN

#### `UpdateHousingFrame` — Editar alojamiento (solo propietario)
- Permite modificar: habitaciones, precio, descripción, comidas
- Solo accesible para el propietario del alojamiento

#### `TradeHousingsFrame` — Intercambiar alojamientos (solo ADMIN)
- Permite a un ADMIN intercambiar uno de sus alojamientos por el de otro ADMIN
- No funciona si alguno de los alojamientos tiene reserva activa

---

### Ventanas de Reservas

#### `ReserveHousingFrame` — Hacer una reserva
- Campos: fecha check-in, fecha check-out, número de tarjeta (16 dígitos)
- Calcula y muestra el precio total automáticamente
- Solo para CUSTOMER

#### `ShowMyReservationsFrame` — Mis reservas
- Tabla con todas las reservas del usuario
- Botón "Check-in" para reservas pendientes → abre `DoCheckInFrame`

#### `DoCheckInFrame` — Realizar check-in
- El usuario introduce el código de reserva que recibió al reservar
- El sistema verifica que el código es correcto y que la fecha ya ha llegado
- Marca la reserva como `checkedIn = true`

---

### Ventanas de Reseñas

#### `ShowReviewsFrame` — Lista de reseñas de un alojamiento
- Tabla con las reseñas ordenadas de más reciente a más antigua
- Doble clic → abre `ReviewDetailsFrame`
- Botón "Publicar reseña" (solo CUSTOMER) → abre `PublishReviewFrame`

#### `ReviewDetailsFrame` — Detalle de una reseña
- Muestra: título, texto, 5 puntuaciones individuales, puntuación total, fecha
- Botón "Editar" si eres el autor → abre `UpdateReviewFrame`

#### `PublishReviewFrame` — Publicar reseña
- Campos: título, texto, 5 spinners de puntuación (0.0 - 5.0 en pasos de 0.5)
- Solo CUSTOMER. Un usuario solo puede reseñar cada alojamiento una vez.

#### `UpdateReviewFrame` — Editar reseña
- Mismos campos que `PublishReviewFrame`
- Solo accesible para el autor de la reseña

---

### Ventanas de Perfil

#### `UpdateProfileFrame` — Editar perfil
- Permite actualizar: username, nombre, apellido, email, teléfono, localidad
- Verifica que el nuevo username no esté ya en uso

#### `ChangePasswordFrame` — Cambiar contraseña
- Campos: contraseña actual, nueva contraseña
- Exige introducir la contraseña actual para confirmar el cambio

---

## 12. Flujos Principales Paso a Paso

### Flujo 1: Registro e inicio de sesión

```
1. La app arranca → aparece LoginFrame
2. Usuario hace clic en "Regístrate" → abre SignUpFrame
3. Rellena el formulario y hace clic en "Registrarse"
4. UserService.signUp() valida el formulario y guarda el usuario con contraseña encriptada
5. Vuelve a LoginFrame
6. Introduce username + password y hace clic en "Iniciar sesión"
7. UserService.login() compara la contraseña con el hash BCrypt
8. Si coincide → SessionManager guarda al usuario → abre ShowHousingsFrame
```

### Flujo 2: Reservar un alojamiento (CUSTOMER)

```
1. En ShowHousingsFrame, el usuario hace doble clic en un alojamiento
2. Abre HousingDetailsFrame con todos los detalles
3. Si el alojamiento está disponible, aparece botón "Reservar"
4. Hace clic → abre ReserveHousingFrame
5. Rellena: fecha check-in, fecha check-out, número de tarjeta
6. ReservationService.reserveHousing() valida todo:
   - El usuario es CUSTOMER
   - La tarjeta tiene 16 dígitos
   - Check-in >= hoy
   - Check-out >= check-in + 1 día
   - El alojamiento sigue disponible
7. Se crea la Reservation con un código aleatorio y precio total calculado
8. El alojamiento se marca como available = false
9. El usuario puede ver la reserva en ShowMyReservationsFrame
```

### Flujo 3: Hacer check-in

```
1. En ShowMyReservationsFrame, el usuario localiza la reserva
2. Hace clic en "Check-in"
3. Abre DoCheckInFrame
4. Introduce el código de reserva que recibió al hacer la reserva
5. ReservationService.doCheckIn() verifica:
   - El código coincide con el de la reserva
   - La fecha actual >= fecha de check-in
   - No se ha hecho check-in antes
6. Si todo es correcto → checkedIn = true
```

### Flujo 4: Publicar una reseña (CUSTOMER)

```
1. En HousingDetailsFrame → "Ver reseñas" → ShowReviewsFrame
2. Hace clic en "Publicar reseña" → abre PublishReviewFrame
3. Rellena: título, texto, 5 puntuaciones (0-5)
4. ReviewService.publishReview() verifica:
   - El usuario es CUSTOMER
   - Las puntuaciones están entre 0 y 5
   - El usuario no ha reseñado antes este alojamiento
5. Se guarda la reseña con totalScore = media de las 5 puntuaciones
6. Se recalcula housing.score = media de TODAS las reseñas del alojamiento
```

---

## 13. Seguridad — Contraseñas con BCrypt

> BCrypt es un algoritmo especialmente diseñado para almacenar contraseñas. A diferencia de un hash simple (como MD5), BCrypt es **intencionalmente lento** y genera un "salt" aleatorio para que no se pueda usar una tabla de hashes precalculados.

### ¿Cómo funciona?

```
Al registrarse:
   Contraseña "miPassword123" → BCrypt → "$2a$10$qNkTlT8v..." (hash de 60 chars)
   Solo el HASH se guarda en la base de datos. La contraseña real NUNCA se guarda.

Al hacer login:
   Contraseña introducida → BCrypt.matches("miPassword123", "$2a$10$qNkTlT8v...") → true/false
   BCrypt hashea la contraseña introducida y la compara con el hash almacenado.
```

### En el código

```java
// En UserServiceImpl.java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

// Al registrar:
user.setPassword(encoder.encode(user.getPassword()));

// Al hacer login:
if (!encoder.matches(passwordIntroducida, user.getPassword())) {
    throw new IncorrectLoginException();
}
```

---

## 14. Transacciones (@Transactional)

> Una "transacción" es un conjunto de operaciones que deben ejecutarse todas o ninguna. Si falla a mitad, se deshacen todos los cambios (rollback). Esto evita datos inconsistentes en la BD.

Todos los métodos de los servicios están anotados con `@Transactional`. Los métodos que solo leen datos usan `@Transactional(readOnly = true)` para mayor rendimiento.

---

## 15. Configuración del Proyecto

### `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/actihome
    username: alex
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none          # Las tablas se crean manualmente con schema.sql
    show-sql: true             # Muestra el SQL generado en consola (útil para debug)
    properties:
      hibernate:
        dialect: MySQL8Dialect
  main:
    headless: false            # Necesario para que Swing pueda mostrar ventanas
```

### `pom.xml` — Dependencias principales

```xml
<dependencies>
  <dependency>spring-boot-starter-data-jpa</dependency>    <!-- ORM + JPA -->
  <dependency>spring-security-crypto</dependency>           <!-- BCrypt -->
  <dependency>spring-boot-starter-validation</dependency>  <!-- Validaciones -->
  <dependency>mysql-connector-java</dependency>             <!-- Driver MySQL -->
  <dependency>spring-boot-devtools</dependency>             <!-- Hot reload -->
  <dependency>spring-boot-starter-test</dependency>         <!-- JUnit + Mockito -->
</dependencies>
```

---

## 16. Cómo Ejecutar el Proyecto

### Requisitos previos

1. **Java 11** instalado (`java -version` en terminal)
2. **MySQL 8** instalado y corriendo
3. **Maven** instalado (`mvn -version` en terminal)
4. **IDE** recomendado: IntelliJ IDEA o Eclipse

### Pasos

1. **Crear la base de datos** en MySQL:
   ```sql
   CREATE DATABASE actihome;
   CREATE USER 'alex'@'localhost' IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON actihome.* TO 'alex'@'localhost';
   ```

2. **Ejecutar el schema** para crear las tablas:
   ```bash
   mysql -u alex -p actihome < src/main/resources/schema.sql
   ```

3. **Configurar la conexión** en `src/main/resources/application.yaml` si tus credenciales son distintas.

4. **Compilar y ejecutar**:
   ```bash
   mvn spring-boot:run
   ```
   O desde el IDE: ejecutar la clase `ActihomeApplication.java`.

5. Aparecerá la ventana de **Login**. Crea un usuario con rol ADMIN para empezar a publicar alojamientos.

---

## 17. Tests Automatizados

Los tests se encuentran en `src/test/java/fp/project/actihome/model/services/` y cubren los 4 servicios:

- `UserServiceTests` — prueba registro, login, actualización de perfil, cambio de contraseña
- `HousingServiceTests` — prueba creación, filtros, actualización, intercambio
- `ReservationServiceTests` — prueba reserva, check-in, validaciones de fechas
- `ReviewServiceTests` — prueba publicación, actualización, validaciones de puntuaciones

Para ejecutar los tests:
```bash
mvn test
```

Los tests usan el perfil `test` (`@ActiveProfiles("test")`) que debería tener su propia configuración de base de datos (H2 en memoria o MySQL de test) para no afectar a los datos reales.

---

*Documentación generada automáticamente a partir del análisis del código fuente.*
