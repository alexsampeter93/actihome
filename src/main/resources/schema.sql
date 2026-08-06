-- Esquema de ActiHome.
--
-- Dos cambios importantes respecto a la versión anterior:
--
-- 1. Ya NO empieza con DROP TABLE. Antes el esquema se destruía y se recreaba en
--    cada arranque, así que todo lo que hicieras en la aplicación desaparecía al
--    cerrarla. Ahora se crea solo si no existe y los datos persisten.
--
-- 2. Es portable entre H2 y MySQL. Se evitan los tipos con precisión que solo
--    admite MySQL (DOUBLE(10,2) pasa a DOUBLE) y se usa AUTO_INCREMENT, que las
--    dos entienden.
--
-- Si alguna vez hace falta empezar de cero, basta con borrar el fichero
-- ~/.actihome/actihome.mv.db

CREATE TABLE IF NOT EXISTS USERS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	username VARCHAR(50) NOT NULL,
	password VARCHAR(200) NOT NULL,
	name VARCHAR(20) NOT NULL,
	surname VARCHAR(20) NOT NULL,
	locality VARCHAR(20) NOT NULL,
	phoneNumber INT NOT NULL,
	email VARCHAR(500) NOT NULL,
	birthDate DATETIME NOT NULL,
	-- El rol se guarda como texto ("ADMIN" / "CUSTOMER") y no como número de
	-- orden del enum. Guardarlo por posición significa que reordenar o insertar
	-- un valor en el enum convierte silenciosamente a todos los administradores
	-- en clientes: un fallo invisible hasta que es grave.
	role VARCHAR(20) NOT NULL,
	-- Ajustes (Fase 7.6). Las tres son preferencias personales, no datos de
	-- negocio. defaultSeason es nula a propósito: nulo significa "sin
	-- preferencia guardada, usa la estación real de hoy", que es el
	-- comportamiento de siempre antes de que existiera esta pantalla.
	defaultSeason VARCHAR(20),
	particlesEnabled BOOLEAN DEFAULT TRUE NOT NULL,
	language VARCHAR(5) DEFAULT 'ES' NOT NULL,
	-- Bienvenida (Fase 7.8): FALSE hasta que se cierra la pantalla de
	-- onboarding, una sola vez por cuenta.
	onboardingSeen BOOLEAN DEFAULT FALSE NOT NULL,
	-- Vista de catálogo por defecto (Fase 7.11): FALSE lista, TRUE cuadrícula.
	defaultGridView BOOLEAN DEFAULT FALSE NOT NULL,
	-- La unicidad del nombre de usuario se comprobaba solo en Java. Eso no es una
	-- garantía, es una comprobación optimista: con dos peticiones simultáneas las
	-- dos pueden pasar el "¿existe ya?" antes de que ninguna haya insertado. La
	-- base de datos es el último guardián de la integridad.
	CONSTRAINT UniqueUsername UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS HOUSINGS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	housingCode BIGINT NOT NULL,
	-- Nombre comercial del alojamiento ("Casa Rural El Pinar"). Es distinto del
	-- tipo: el nombre titula la ficha, el tipo es la categoría por la que se
	-- filtra en el catálogo. Añadido en la Fase 3a.
	name VARCHAR(80) NOT NULL,
	type VARCHAR(30) NOT NULL,
	numberOfRooms INTEGER NOT NULL,
	pricePerNight DECIMAL(10, 2) NOT NULL,
	description VARCHAR(500) NOT NULL,
	-- Nombre del archivo de foto dentro de /images/housings/. Nulo mientras no
	-- haya foto: la ficha pinta entonces el marcador tintado por estación.
	image VARCHAR(120),
	breakfast BOOLEAN NOT NULL,
	lunch BOOLEAN NOT NULL,
	dinner BOOLEAN NOT NULL,
	-- Comodidades del catálogo (Fase 3a, ADR-003). "Desayuno" no está aquí: es el
	-- campo breakfast de arriba, que ya existía y se reutiliza.
	pool BOOLEAN DEFAULT FALSE NOT NULL,
	wifi BOOLEAN DEFAULT FALSE NOT NULL,
	tv BOOLEAN DEFAULT FALSE NOT NULL,
	parking BOOLEAN DEFAULT FALSE NOT NULL,
	airConditioning BOOLEAN DEFAULT FALSE NOT NULL,
	pets BOOLEAN DEFAULT FALSE NOT NULL,
	-- En qué estación luce más este alojamiento (Fase 8.4). Se guarda como texto,
	-- no por ordinal, igual que el resto de enumerados del proyecto: reordenar el
	-- enum no debe convertir en silencio una casa de playa en una de montaña.
	-- Admite nulo a propósito: un alojamiento publicado desde la aplicación no
	-- tiene por qué declarar estación, y entonces sencillamente no lleva
	-- distintivo. Obligar a elegir una convertiría un matiz editorial en un
	-- trámite.
	idealSeason VARCHAR(20),
	-- Intercambio abierto (Fase 8.4): el propietario declara que acepta permutar
	-- este alojamiento, y opcionalmente qué busca a cambio. Es una OFERTA publica,
	-- no una propuesta entre dos personas: por eso vive en el alojamiento y no en
	-- una tabla de propuestas con remitente y destinatario.
	openToExchange BOOLEAN DEFAULT FALSE NOT NULL,
	exchangeWanted VARCHAR(120),
	score DOUBLE,
	-- La disponibilidad ya no es una columna: se calcula a partir de las reservas
	-- activas de cada alojamiento (Fase 7.5, ver migracion-h2.sql). Un booleano
	-- guardado no distinguía "reservado hoy" de "reservado en marzo de 2027", así
	-- que una vez reservado un alojamiento quedaba bloqueado para siempre (B5).
	location VARCHAR(40) NOT NULL,
	-- Coordenadas en grados decimales (F17). Admiten nulo A PROPÓSITO: "location"
	-- es texto para leer y no se le puede preguntar a un servicio meteorológico
	-- ni a un mapa por la cadena "Sierra Nevada, Granada". Los dos campos no
	-- compiten: el texto es lo que se enseña, las coordenadas son lo que se
	-- consulta. Un alojamiento sin localizar es un caso normal —solo las tiene
	-- quien pulsó "Localizar" al publicarlo— y lo que dependa de ellas debe
	-- desaparecer sin ruido cuando falten.
	latitude DOUBLE,
	longitude DOUBLE,
	ownerId BIGINT NOT NULL,
	CONSTRAINT UniqueHousingCode UNIQUE (housingCode),
	CONSTRAINT OwnerIdFK FOREIGN KEY(ownerId) REFERENCES USERS(id)
);

CREATE TABLE IF NOT EXISTS REVIEWS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	title VARCHAR(50) NOT NULL,
	body VARCHAR(500) NOT NULL,
	locationScore DOUBLE NOT NULL,
	serviceScore DOUBLE NOT NULL,
	wifiScore DOUBLE NOT NULL,
	foodScore DOUBLE NOT NULL,
	cleaningScore DOUBLE NOT NULL,
	totalScore DOUBLE NOT NULL,
	publicationDate DATETIME NOT NULL,
	authorId BIGINT NOT NULL,
	housingId BIGINT NOT NULL,
	-- Nombre del archivo de foto dentro de /images/reviews/. Nulo mientras la
	-- reseña no lleve foto adjunta (F15): no todas la llevan.
	image VARCHAR(120),
	-- Respuesta pública del propietario del alojamiento (F15). Las dos van juntas
	-- y las dos nulas hasta que el propietario responde.
	ownerResponse VARCHAR(500),
	ownerResponseDate DATETIME,
	CONSTRAINT AuthorIdFK FOREIGN KEY(authorId) REFERENCES USERS(id),
	CONSTRAINT HousingIdFK FOREIGN KEY(housingId) REFERENCES HOUSINGS(id)
);

CREATE TABLE IF NOT EXISTS RESERVATIONS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	reservationCode BIGINT NOT NULL,
	checkIn DATETIME NOT NULL,
	checkOut DATETIME NOT NULL,
	paymentMethod VARCHAR(50) NOT NULL,
	reservationDate DATETIME NOT NULL,
	totalPrice DECIMAL(10, 2) NOT NULL,
	checkedIn BOOLEAN NOT NULL,
	-- Fase 7.5.3: si el cliente la ha cancelado. Una reserva cancelada deja de
	-- contar para el solapamiento de fechas (ver ReservationDao), así que sus
	-- días vuelven a estar libres para cualquiera.
	cancelled BOOLEAN DEFAULT FALSE NOT NULL,
	customerId BIGINT NOT NULL,
	housingId BIGINT NOT NULL,
	CONSTRAINT CustomerIdFK FOREIGN KEY(customerId) REFERENCES USERS(id),
	CONSTRAINT HousingIdFK2 FOREIGN KEY(housingId) REFERENCES HOUSINGS(id)
);

-- Mensajería interna huésped <-> propietario (F10), siempre sobre un
-- alojamiento concreto. No hay tabla de "conversaciones": una conversación es
-- el conjunto de mensajes que comparten alojamiento y los dos mismos
-- participantes, y eso se agrupa en memoria (MessageServiceImpl), no en SQL.
CREATE TABLE IF NOT EXISTS MESSAGES (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	senderId BIGINT NOT NULL,
	recipientId BIGINT NOT NULL,
	housingId BIGINT NOT NULL,
	body VARCHAR(1000) NOT NULL,
	sentDate DATETIME NOT NULL,
	-- Nulo mientras el destinatario no lo haya abierto. No se llama "read": es
	-- palabra reservada en varios dialectos SQL, y además un LocalDateTime dice
	-- más que un booleano (cuándo, no solo si).
	readDate DATETIME,
	CONSTRAINT MessageSenderIdFK FOREIGN KEY(senderId) REFERENCES USERS(id),
	CONSTRAINT MessageRecipientIdFK FOREIGN KEY(recipientId) REFERENCES USERS(id),
	CONSTRAINT MessageHousingIdFK FOREIGN KEY(housingId) REFERENCES HOUSINGS(id)
);

-- Fotos adicionales de un alojamiento, para la galería del detalle (Fase 8.4).
-- La foto PRINCIPAL no está aquí: sigue en HOUSINGS.image, porque la usan el
-- catálogo, la comparación y el panel de propietario, y ninguno de los tres
-- quiere una galería. Pedirles una consulta más para obtener lo que ya tenían
-- sería pagar en todas las pantallas el precio de una.
CREATE TABLE IF NOT EXISTS HOUSING_PHOTOS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	housingId BIGINT NOT NULL,
	-- Nombre del archivo dentro de /images/housings/.
	image VARCHAR(120) NOT NULL,
	-- Orden dentro de la galería, empezando en 1. Se guarda en lugar de deducirse
	-- del nombre del archivo: un nombre no se puede reordenar sin renombrar
	-- ficheros.
	position INTEGER NOT NULL,
	CONSTRAINT PhotoHousingIdFK FOREIGN KEY(housingId) REFERENCES HOUSINGS(id)
);

-- Códigos de recuperación de contraseña (Fase 8.6).
--
-- El código se guarda HASHEADO, no en claro, y por el mismo motivo que las
-- contraseñas: quien pueda leer esta tabla —una copia de seguridad, el fichero
-- de la base— no debe poder usar lo que lee para entrar en ninguna cuenta.
--
-- "attempts" existe porque un código corto sin límite de intentos no protege de
-- nada: se prueban todos en segundos. Con cinco intentos, adivinarlo deja de ser
-- viable sin necesidad de alargar el código hasta hacerlo incómodo de teclear.
CREATE TABLE IF NOT EXISTS PASSWORD_RESET_CODES (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	userId BIGINT NOT NULL,
	codeHash VARCHAR(200) NOT NULL,
	expiresAt DATETIME NOT NULL,
	attempts INTEGER DEFAULT 0 NOT NULL,
	-- Nulo mientras no se haya usado. Un código es de un solo uso: una vez
	-- cambiada la contraseña deja de valer, aunque no haya caducado todavía.
	usedAt DATETIME,
	CONSTRAINT ResetCodeUserIdFK FOREIGN KEY(userId) REFERENCES USERS(id)
);

-- Caché de traducciones de contenido (Fase 8.7).
--
-- Es lo que hace viable traducir el contenido y no solo la interfaz. Sin caché,
-- entrar en el catálogo en inglés serían treinta llamadas al traductor cada vez;
-- con 5.000 palabras al día de cuota, eso se agota en dos sesiones. Con caché se
-- traduce una vez por texto y para siempre.
--
-- La clave es un HASH del texto original, no el texto: un índice único sobre una
-- columna de 500 caracteres es frágil entre motores (MySQL tiene límite de bytes
-- por índice) y además obliga a comparar cadenas largas en cada búsqueda. El
-- hash es de longitud fija y compara rápido.
--
-- No hay clave ajena a ninguna tabla a propósito: esto traduce TEXTOS, no
-- campos de entidades. La misma descripción escrita en dos alojamientos se
-- traduce una sola vez, y borrar un alojamiento no invalida nada.
CREATE TABLE IF NOT EXISTS TRANSLATIONS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	sourceHash VARCHAR(64) NOT NULL,
	targetLanguage VARCHAR(5) NOT NULL,
	translatedText VARCHAR(1000) NOT NULL,
	CONSTRAINT UniqueTranslation UNIQUE (sourceHash, targetLanguage)
);
