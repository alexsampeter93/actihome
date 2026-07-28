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
	-- La unicidad del nombre de usuario se comprobaba solo en Java. Eso no es una
	-- garantía, es una comprobación optimista: con dos peticiones simultáneas las
	-- dos pueden pasar el "¿existe ya?" antes de que ninguna haya insertado. La
	-- base de datos es el último guardián de la integridad.
	CONSTRAINT UniqueUsername UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS HOUSINGS (
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	housingCode BIGINT NOT NULL,
	type VARCHAR(30) NOT NULL,
	numberOfRooms INTEGER NOT NULL,
	pricePerNight DECIMAL(10, 2) NOT NULL,
	description VARCHAR(500) NOT NULL,
	breakfast BOOLEAN NOT NULL,
	lunch BOOLEAN NOT NULL,
	dinner BOOLEAN NOT NULL,
	score DOUBLE,
	available BOOLEAN NOT NULL,
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
	customerId BIGINT NOT NULL,
	housingId BIGINT NOT NULL,
	CONSTRAINT CustomerIdFK FOREIGN KEY(customerId) REFERENCES USERS(id),
	CONSTRAINT HousingIdFK2 FOREIGN KEY(housingId) REFERENCES HOUSINGS(id)
);
