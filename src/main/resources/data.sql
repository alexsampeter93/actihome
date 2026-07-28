-- Datos de ejemplo de ActiHome.
--
-- Este script se ejecuta en cada arranque, así que cada inserción va protegida
-- por un WHERE NOT EXISTS: si la fila ya está, no se duplica. A eso se le llama
-- idempotente — ejecutarlo una vez o cien veces deja el mismo resultado.
--
-- Antes no hacía falta porque el esquema se borraba entero en cada arranque.
-- Ahora los datos persisten, así que el script tiene que poder convivir con lo
-- que ya haya.
--
-- Se usa FROM DUAL porque MySQL exige un FROM cuando hay WHERE; H2 también lo
-- acepta, así que el script vale para las dos bases.
--
-- Las comillas son simples y no dobles: en SQL estándar las dobles delimitan
-- nombres de columna, no texto. MySQL era permisivo con eso; H2 no.
--
-- NOTA: la contraseña de estos usuarios está en texto plano, así que NO pueden
-- iniciar sesión (el login usa BCrypt). Son datos para poblar el catálogo;
-- para entrar hay que registrarse.
--
-- Se han retirado las reseñas y reservas de ejemplo que había antes: apuntaban a
-- identificadores fijos (2, 3, 4) que solo eran correctos si la base estaba
-- recién creada, y sus fechas de 2026 ya son pasado, así que las reservas
-- aparecían en estados imposibles. Los datos de demostración del catálogo se
-- rehacen en la Fase 3, cuando la pantalla que los muestra esté diseñada.
--
-- Las referencias entre tablas se resuelven con subconsultas por el nombre de
-- usuario y no con el número de fila: un id fijo deja de ser correcto en cuanto
-- la base no está vacía.

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Admin', '1234', 'Alejandro', 'Sampedro', 'Coruña', 666777892, 'alejsamcalo@gmail.com', '1993-01-03', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Admin');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Customer', '1234', 'Jorge', 'Sampedro', 'Coruña', 664567076, 'jorgesamcalo@gmail.com', '1999-12-09', 'CUSTOMER'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Customer');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Customer10', '1234', 'Jorge', 'Sampedro', 'Coruña', 664567076, 'jorgesamcalo@gmail.com', '1999-12-09', 'CUSTOMER'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Customer10');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Customer16', '1234', 'Jorge', 'Sampedro', 'Coruña', 664567076, 'jorgesamcalo@gmail.com', '1999-12-09', 'CUSTOMER'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Customer16');

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
SELECT 204183, 'Casa en la playa', 6, 40.42, 'Descripcion', TRUE, FALSE, TRUE, NULL, TRUE, 'Andalucia',
	(SELECT id FROM USERS WHERE username = 'Admin')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 204183);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
SELECT 204184, 'Casa en la playa', 6, 40.42, 'Descripcion', TRUE, FALSE, TRUE, NULL, TRUE, 'Andalucia',
	(SELECT id FROM USERS WHERE username = 'Admin')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 204184);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
SELECT 204163, 'Casa en la playa', 6, 40.42, 'Descripcion', TRUE, FALSE, TRUE, NULL, TRUE, 'Andalucia',
	(SELECT id FROM USERS WHERE username = 'Admin')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 204163);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
SELECT 204132, 'Casa en la playa', 6, 40.42, 'Descripcion', TRUE, FALSE, TRUE, NULL, TRUE, 'Andalucia',
	(SELECT id FROM USERS WHERE username = 'Admin')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 204132);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
SELECT 2042183, 'Casa en la playa', 6, 40.42, 'Descripcion', TRUE, FALSE, TRUE, NULL, TRUE, 'Andalucia',
	(SELECT id FROM USERS WHERE username = 'Admin')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 2042183);
