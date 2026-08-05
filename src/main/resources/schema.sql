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
	score DOUBLE,
	-- La disponibilidad ya no es una columna: se calcula a partir de las reservas
	-- activas de cada alojamiento (Fase 7.5, ver migracion-h2.sql). Un booleano
	-- guardado no distinguía "reservado hoy" de "reservado en marzo de 2027", así
	-- que una vez reservado un alojamiento quedaba bloqueado para siempre (B5).
	location VARCHAR(40) NOT NULL,
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
