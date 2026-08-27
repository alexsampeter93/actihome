-- =============================================================================
-- R — Datos de ejemplo y reparaciones. Migración REPETIBLE.
--
-- POR QUÉ REPETIBLE Y NO VERSIONADA
--
-- Las migraciones versionadas (`V1`, `V2`...) se ejecutan una vez y no vuelven a
-- mirarse. Este archivo no describe una estructura sino un CONTENIDO —los diez
-- alojamientos de ejemplo, los usuarios de prueba, la caché de traducciones— y
-- ese contenido crece con el proyecto. Flyway ejecuta las repetibles **después**
-- de todas las versionadas y **cada vez que cambia su contenido**, que es
-- exactamente el comportamiento que hace falta: añadir un alojamiento de ejemplo
-- mañana lo siembra también en las instalaciones que ya existen, sin inventarse
-- una versión nueva para algo que no es un cambio de esquema.
--
-- POR QUÉ SIGUE SIENDO IDEMPOTENTE
--
-- Porque se va a ejecutar más de una vez. Cada inserción va protegida por un
-- `WHERE NOT EXISTS` y cada reparación por una condición que deja de cumplirse
-- en cuanto se ha aplicado (`WHERE image IS NULL`, `WHERE password = '1234'`).
-- Es la regla dura del proyecto: **corregir el proceso que genera un dato no
-- corrige el dato ya generado**, así que cada arreglo de datos sembrados necesita
-- dos partes, y la segunda vive aquí.
-- =============================================================================

-- Datos de ejemplo de ActiHome.
--
-- Este script se ejecuta en cada arranque, así que cada inserción va protegida
-- por un WHERE NOT EXISTS: si la fila ya está, no se duplica. A eso se le llama
-- idempotente — ejecutarlo una vez o cien veces deja el mismo resultado.
--
-- Se usa FROM DUAL porque MySQL exige un FROM cuando hay WHERE; H2 también lo
-- acepta, así que el script vale para las dos bases.
--
-- Las comillas son simples y no dobles: en SQL estándar las dobles delimitan
-- nombres de columna, no texto. MySQL era permisivo con eso; H2 no.
--
-- Las referencias entre tablas se resuelven con subconsultas por el nombre de
-- usuario o por el código de alojamiento, nunca con el número de fila: un id
-- fijo deja de ser correcto en cuanto la base no está vacía.
--
-- NOTA sobre la contraseña: es el hash BCrypt de "1234", precalculado (no se
-- puede escribir un hash BCrypt a mano: cada vez que se genera sale distinto,
-- porque la sal es aleatoria, así que se calcula una vez con el mismo
-- BCryptPasswordEncoder que usa la aplicación y se pega el resultado aquí).
-- Antes de la Fase 3d la contraseña estaba en texto plano y estos usuarios no
-- podían iniciar sesión contra el login() real, que compara con BCrypt: cero
-- de los usuarios de ejemplo eran utilizables sin registrarse primero, y
-- registrarse no daba acceso a los alojamientos ya sembrados (pertenecen a
-- Lucia/Marcos/Elena). Con el hash real, "Admin" / "1234" entra directamente,
-- y "Lucia" / "1234" entra como propietaria de sus alojamientos.
--
-- CAMBIO DE LA FASE 3A: los cinco alojamientos de relleno anteriores —los cinco
-- llamados "Casa en la playa", en Andalucía, con descripción "Descripcion"— se
-- sustituyen por los seis del diseño, con nombre, tipo, comodidades y reseñas
-- reales. El catálogo es la pantalla que enseña el producto: con datos de
-- relleno idénticos entre sí no se puede ni juzgar el diseño ni probar los
-- filtros, porque todos los alojamientos caen siempre en el mismo grupo.

-- ---------------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------------

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Admin', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Alejandro', 'Sampedro', 'Coruña', 666777892, 'alejsamcalo@gmail.com', '1993-01-03', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Admin');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Customer', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Jorge', 'Sampedro', 'Coruña', 664567076, 'jorgesamcalo@gmail.com', '1999-12-09', 'CUSTOMER'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Customer');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Customer10', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Jorge', 'Sampedro', 'Coruña', 664567076, 'jorgesamcalo@gmail.com', '1999-12-09', 'CUSTOMER'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Customer10');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Customer16', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Jorge', 'Sampedro', 'Coruña', 664567076, 'jorgesamcalo@gmail.com', '1999-12-09', 'CUSTOMER'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Customer16');

-- Propietarios de los alojamientos de ejemplo. Son ADMIN porque en ActiHome
-- solo ese rol puede publicar. Tener varios permite ver en el catálogo la línea
-- de "propietario" con valores distintos, y probar que editar e intercambiar
-- respetan la titularidad.

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Lucia', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Lucía', 'Ferreiro', 'Granada', 655101202, 'lucia@actihome.example', '1986-04-17', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Lucia');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Marcos', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Marcos', 'Iglesias', 'Málaga', 655101203, 'marcos@actihome.example', '1981-09-02', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Marcos');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Elena', '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m', 'Elena', 'Prado', 'Oviedo', 655101204, 'elena@actihome.example', '1990-11-25', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Elena');

-- ---------------------------------------------------------------------------
-- Migración de contraseñas para quien ya tenía la base creada (Fase 3d)
--
-- Los INSERT de arriba van protegidos por "WHERE NOT EXISTS": si el usuario ya
-- existe, no se reinserta, así que quien ya tuviera ~/.actihome con estos
-- usuarios se habría quedado con la contraseña vieja en texto plano para
-- siempre. Este UPDATE los pone al día una sola vez.
--
-- La condición "password = '1234'" es la que lo hace seguro de repetir: un
-- hash BCrypt real empieza siempre por "$2a$" y mide 60 caracteres, así que
-- nunca puede valer literalmente "1234". Una vez aplicado el cambio, esta
-- condición deja de cumplirse y el UPDATE no vuelve a tocar la fila.
-- ---------------------------------------------------------------------------

UPDATE USERS
SET password = '$2a$10$3vQGMyd/3SLOjyMvMuw.deJsgMlyLYXY1dZd8NvmXF7yzawxc310m'
WHERE username IN ('Admin', 'Customer', 'Customer10', 'Customer16', 'Lucia', 'Marcos', 'Elena')
	AND password = '1234';

-- ---------------------------------------------------------------------------
-- Retirada de los alojamientos de relleno anteriores
--
-- Solo se borran si nadie los ha usado. Un DELETE a secas fallaría contra las
-- claves ajenas en cuanto alguien hubiera reservado o reseñado uno de ellos —y
-- fallar al arrancar es peor que dejar cinco filas viejas en el catálogo.
-- ---------------------------------------------------------------------------

DELETE FROM HOUSINGS
WHERE housingCode IN (204183, 204184, 204163, 204132, 2042183)
	AND id NOT IN (SELECT housingId FROM RESERVATIONS)
	AND id NOT IN (SELECT housingId FROM REVIEWS);

-- ---------------------------------------------------------------------------
-- Alojamientos
--
-- Son los seis del handoff de diseño, con sus tipos, precios y comodidades. El
-- campo "image" queda a NULL: mientras no haya fotos reales, la ficha pinta el
-- marcador tintado con el color de la estación activa, que es exactamente lo
-- que el diseño especifica para ese caso.
--
-- La puntuación (score) NO se escribe aquí: se calcula más abajo a partir de las
-- reseñas, igual que hace el servicio al publicar una. Escribirla a mano habría
-- dejado alojamientos con un 4,8 y cero reseñas, que es un dato que se
-- contradice a sí mismo.
-- ---------------------------------------------------------------------------

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10001, 'Casa Rural El Pinar', 'Casa', 3, 6, 75.00,
	'Casa de piedra restaurada a media ladera, con chimenea, huerto y vistas abiertas a la sierra. El pueblo queda a diez minutos a pie.',
	NULL, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, NULL, 'Sierra Nevada, Granada',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10001);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10002, 'Apartamento Playa Centro', 'Apartamento', 2, 4, 95.00,
	'Segunda línea de playa, con terraza orientada al sur y ascensor. La zona de bares y el paseo marítimo quedan al doblar la esquina.',
	NULL, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, 'Málaga',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10002);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10003, 'Villa Mediterráneo', 'Villa', 5, 10, 320.00,
	'Villa encalada con piscina privada, porche de sombra y acceso directo a una cala pequeña. Pensión completa incluida.',
	NULL, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, 'Ibiza',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10003);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10004, 'Cabaña del Bosque', 'Cabaña', 1, 2, 48.00,
	'Cabaña de madera para dos, con estufa de leña y ventanal al hayedo. Sin cobertura y sin vecinos: ese es el plan.',
	NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, NULL, 'Picos de Europa, Asturias',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10004);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10005, 'Loft Barrio Gótico', 'Apartamento', 2, 4, 130.00,
	'Loft diáfano en un edificio del XIX, con vigas vistas y techos de cuatro metros. En pleno casco antiguo, a paso de todo.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, 'Barcelona',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10005);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10006, 'Casa Adosada Las Palmas', 'Casa', 4, 8, 110.00,
	'Adosado con patio, barbacoa y piscina comunitaria, en una urbanización tranquila a quince minutos de la playa de Las Canteras.',
	NULL, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, 'Gran Canaria',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10006);

-- Cuatro alojamientos más (Fase 7.10), para que los filtros nuevos de precio y
-- ciudad tengan algo de verdad que filtrar: sin variedad de precio ni de
-- ciudad, esos dos filtros no se pueden ni probar.
INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10007, 'Ático Malasaña', 'Apartamento', 2, 4, 88.00,
	'Ático con terraza recién reformado, cocina abierta y aire acondicionado en las dos habitaciones. El barrio se recorre entero a pie: bares, teatros y el Retiro a quince minutos.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, 'Madrid',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10007);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10008, 'Villa Costa del Sol', 'Villa', 5, 10, 275.00,
	'Villa de líneas contemporáneas con piscina infinita, terraza panorámica y vistas al mar desde las dos plantas. Pensión completa incluida.',
	NULL, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, 'Marbella, Málaga',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10008);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10009, 'Casa Patio Andaluz', 'Casa', 3, 6, 58.00,
	'Casa con patio interior en el corazón del casco histórico, paredes encaladas y suelo de barro cocido. El Tajo queda a cinco minutos a pie desde la puerta.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, NULL, 'Ronda, Málaga',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10009);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, capacity, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10010, 'Refugio de Nieve', 'Cabaña', 2, 4, 68.00,
	'Refugio de madera con estufa de leña y vistas a las pistas desde el porche. Aparcamiento propio junto a la puerta, imprescindible cuando nieva.',
	NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, NULL, 'Valle de Tena, Huesca',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10010);

-- ---------------------------------------------------------------------------
-- Reseñas
--
-- Dos por alojamiento. No son las 14, 31 u 8 reseñas que enseña la maqueta: allí
-- el número es una etiqueta pintada, aquí cada reseña es una fila de verdad que
-- se puede abrir, y ciento veinte párrafos inventados no aportan nada que no
-- aporten doce.
--
-- La fecha se calcula con TIMESTAMPADD sobre la fecha actual, no se escribe a
-- mano. Una fecha fija envejece: los datos de 2026 que había antes hacían que
-- las reservas de ejemplo apareciesen en estados imposibles, y los tests con
-- fechas fijas fueron justo lo que rompió la suite (bug B14). TIMESTAMPADD lo
-- entienden H2 y MySQL con la misma sintaxis.
--
-- totalScore es la media de las cinco sub-notas, que es como lo calcula
-- ReviewServiceImpl. Los datos sembrados deben cumplir las mismas reglas que
-- aplicaría la aplicación; si no, son datos que la aplicación nunca habría
-- podido crear.
-- ---------------------------------------------------------------------------

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Silencio y buen desayuno', 'La casa es tal cual las fotos y el desayuno con pan de la panadería del pueblo merece madrugar. La subida en coche es estrecha pero se hace en cinco minutos.',
	4.5, 4.5, 4.0, 4.5, 4.5, 4.4, TIMESTAMPADD(DAY, -34, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10001)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Silencio y buen desayuno');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Perfecta para desconectar', 'Fuimos con el perro y no hubo ningún problema. El wifi va justo, pero tampoco íbamos buscando eso. Volveríamos en otoño.',
	4.0, 4.0, 3.5, 4.5, 4.0, 4.0, TIMESTAMPADD(DAY, -12, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10001)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Perfecta para desconectar');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'No se puede pedir más', 'Ubicación inmejorable, limpieza impecable y la terraza da el sol toda la tarde. La anfitriona respondió en minutos a todo.',
	5.0, 5.0, 5.0, 5.0, 5.0, 5.0, TIMESTAMPADD(DAY, -47, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer16'), (SELECT id FROM HOUSINGS WHERE housingCode = 10002)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'No se puede pedir más');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Muy bien situado', 'A dos calles del paseo y con aire acondicionado que se agradece en agosto. El desayuno es sencillo pero cumple.',
	4.5, 5.0, 4.5, 4.5, 4.5, 4.6, TIMESTAMPADD(DAY, -9, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10002)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Muy bien situado');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'La cala privada lo vale', 'Somos ocho y sobraba sitio. La piscina se limpia a diario y la cena del segundo día fue de lo mejor del viaje.',
	5.0, 4.5, 4.5, 4.5, 4.5, 4.6, TIMESTAMPADD(DAY, -61, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10003)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'La cala privada lo vale');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Caro pero se disfruta', 'El precio por noche asusta hasta que lo divides entre los que vamos. Pensión completa y no pisar el coche en una semana.',
	4.5, 4.5, 4.0, 4.5, 4.5, 4.4, TIMESTAMPADD(DAY, -21, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer16'), (SELECT id FROM HOUSINGS WHERE housingCode = 10003)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Caro pero se disfruta');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Rústica de verdad', 'Es una cabaña, no un hotel: hay que encender la estufa y no hay comidas. Si vas sabiendo eso, es un acierto.',
	4.0, 3.5, 3.0, 4.0, 3.5, 3.6, TIMESTAMPADD(DAY, -55, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10004)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Rústica de verdad');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Nos faltó calefacción', 'La ubicación en el hayedo es preciosa y aparcas en la puerta. En noviembre pasamos frío por la noche y no había forma de avisar.',
	3.5, 3.0, 2.5, 3.5, 3.5, 3.2, TIMESTAMPADD(DAY, -16, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10004)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Nos faltó calefacción');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'El mejor sitio del barrio', 'Techos altísimos, silencio pese a estar en el centro y una cama enorme. Repetiremos seguro.',
	5.0, 5.0, 5.0, 5.0, 5.0, 5.0, TIMESTAMPADD(DAY, -28, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer16'), (SELECT id FROM HOUSINGS WHERE housingCode = 10005)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'El mejor sitio del barrio');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Encanto de edificio antiguo', 'La escalera es de época y no hay ascensor, con maletas se nota. Por lo demás, impecable y muy bien comunicado.',
	5.0, 5.0, 4.5, 4.5, 5.0, 4.8, TIMESTAMPADD(DAY, -5, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10005)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Encanto de edificio antiguo');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Buen plan en familia', 'Cuatro habitaciones, patio para los niños y la piscina comunitaria casi vacía en mayo. La media pensión ahorra mucho lío.',
	4.5, 4.0, 4.0, 4.5, 4.0, 4.2, TIMESTAMPADD(DAY, -40, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10006)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Buen plan en familia');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Correcto de principio a fin', 'Sin sorpresas ni buenas ni malas: todo funcionaba, todo estaba limpio y el coche cabía en la plaza. Se agradece.',
	4.0, 4.0, 4.0, 4.0, 4.0, 4.0, TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer16'), (SELECT id FROM HOUSINGS WHERE housingCode = 10006)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Correcto de principio a fin');

-- Reseñas de los cuatro alojamientos de la Fase 7.10.

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Ubicación insuperable', 'Se sale a la calle y ya estás en Malasaña. Ruido normal de barrio por la noche, así que si buscas silencio no es tu sitio, pero para lo que es, perfecto.',
	5.0, 4.5, 4.5, 4.0, 4.5, 4.5, TIMESTAMPADD(DAY, -18, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10007)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Ubicación insuperable');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Pequeño pero muy bien pensado', 'La terraza aprovecha cada rincón y el aire acondicionado se agradeció en pleno agosto. Repetiríamos.',
	4.5, 4.5, 4.5, 4.0, 4.5, 4.4, TIMESTAMPADD(DAY, -6, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10007)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Pequeño pero muy bien pensado');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Vale cada euro', 'La piscina infinita con vistas al mar es tal cual las fotos, y la pensión completa quitó cualquier preocupación durante la semana.',
	5.0, 5.0, 4.5, 5.0, 5.0, 4.9, TIMESTAMPADD(DAY, -25, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer16'), (SELECT id FROM HOUSINGS WHERE housingCode = 10008)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Vale cada euro');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Lujo sin postureo', 'Éramos diez y no faltó sitio para nadie. El servicio respondió rápido a todo lo que pedimos, sin agobiar.',
	5.0, 4.5, 4.5, 4.5, 4.5, 4.6, TIMESTAMPADD(DAY, -11, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10008)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Lujo sin postureo');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'El patio es otro mundo', 'Entras del bullicio del casco antiguo y el patio te calla de golpe. Sin aparcamiento propio, así que hay que dejar el coche fuera.',
	4.5, 4.0, 3.5, 4.0, 4.5, 4.1, TIMESTAMPADD(DAY, -14, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10009)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'El patio es otro mundo');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Auténtica de verdad', 'Nada de decoración de cartón piedra: es una casa real de pueblo, con sus paredes gruesas y su fresco en verano. Se admiten perros y el nuestro estuvo encantado.',
	4.5, 4.5, 3.5, 4.5, 4.5, 4.3, TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer16'), (SELECT id FROM HOUSINGS WHERE housingCode = 10009)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Auténtica de verdad');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Desconexión total', 'Cero cobertura y la estufa de leña como única calefacción: hay que ir sabiéndolo. Las vistas desde el porche compensan cualquier incomodidad.',
	4.5, 3.5, 2.0, 3.0, 4.0, 3.4, TIMESTAMPADD(DAY, -30, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer'), (SELECT id FROM HOUSINGS WHERE housingCode = 10010)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Desconexión total');

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
SELECT 'Ideal para esquiar', 'A cinco minutos en coche de las pistas y con sitio de sobra para dejar el equipo a secar. El aparcamiento propio es un lujo cuando nieva de verdad.',
	5.0, 4.0, 2.5, 3.5, 4.0, 3.8, TIMESTAMPADD(DAY, -8, CURRENT_TIMESTAMP),
	(SELECT id FROM USERS WHERE username = 'Customer10'), (SELECT id FROM HOUSINGS WHERE housingCode = 10010)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM REVIEWS WHERE title = 'Ideal para esquiar');

-- ---------------------------------------------------------------------------
-- Puntuación de cada alojamiento, derivada de sus reseñas
--
-- Es la misma regla que aplica ReviewServiceImpl al publicar o editar una
-- reseña: la nota del alojamiento es la media de las notas totales de sus
-- reseñas. Se recalcula en cada arranque a partir de lo que haya en la tabla, de
-- modo que si alguien publica una reseña desde la aplicación el valor sembrado
-- no la contradice.
-- ---------------------------------------------------------------------------

UPDATE HOUSINGS h
SET score = (SELECT AVG(r.totalScore) FROM REVIEWS r WHERE r.housingId = h.id)
WHERE EXISTS (SELECT 1 FROM REVIEWS r WHERE r.housingId = h.id);

-- ---------------------------------------------------------------------------
-- Fotos de los alojamientos de ejemplo
--
-- El archivo se llama como el código del alojamiento, así que basta una regla en
-- lugar de seis asignaciones. Las fotos las prepara ui/dev/GenerarAssets a
-- partir de assets/References/Alojamientos de ejemplo; son de Unsplash y están
-- acreditadas en CREDITOS.md.
--
-- Va como UPDATE al final y no dentro de cada INSERT por el mismo motivo que la
-- migración de contraseñas de la Fase 3d: los INSERT van protegidos por
-- WHERE NOT EXISTS, así que en una base que ya existía no se ejecutan nunca y
-- los alojamientos se habrían quedado sin foto para siempre.
--
-- La condición "image IS NULL" lo hace idempotente y además respeta a quien haya
-- puesto una foto propia: solo rellena lo que está vacío.
-- ---------------------------------------------------------------------------

UPDATE HOUSINGS
SET image = CONCAT(housingCode, '.jpg')
WHERE image IS NULL AND housingCode BETWEEN 10001 AND 10010;

-- ---------------------------------------------------------------------------
-- Estación ideal de cada alojamiento de ejemplo (Fase 8.4)
--
-- Alimenta el distintivo "Ideal en {estación}" del catálogo, que solo aparece
-- cuando la estación del alojamiento coincide con la activa. Es lo que hace que
-- cambiar de estación cambie *lo que ves* y no solo los colores.
--
-- Va como UPDATE al final y no dentro de cada INSERT, por la misma razón que la
-- migración de fotos de aquí arriba y la de contraseñas de la Fase 3d: los
-- INSERT están protegidos por WHERE NOT EXISTS, así que en una base que ya
-- existía no se ejecutan nunca y los diez alojamientos se habrían quedado sin
-- estación para siempre. Un solo UPDATE cubre los dos casos —instalación nueva y
-- base ya creada—, que es la razón de no duplicarlo también en los INSERT.
--
-- La condición "idealSeason IS NULL" lo hace idempotente y respeta a quien haya
-- cambiado la estación de un alojamiento desde la aplicación: solo rellena lo
-- que está vacío.
--
-- El reparto está equilibrado a propósito (2 primavera, 4 verano, 2 otoño,
-- 2 invierno): con todas las estaciones representadas, el distintivo se ve sea
-- cual sea la activa. Si tres cuartas partes fueran de verano, el catálogo
-- parecería roto en invierno.
-- ---------------------------------------------------------------------------

UPDATE HOUSINGS SET idealSeason = 'INVIERNO' WHERE idealSeason IS NULL AND housingCode IN (10001, 10010);
UPDATE HOUSINGS SET idealSeason = 'VERANO'   WHERE idealSeason IS NULL AND housingCode IN (10002, 10003, 10006, 10008);
UPDATE HOUSINGS SET idealSeason = 'OTONO'    WHERE idealSeason IS NULL AND housingCode IN (10004, 10009);
UPDATE HOUSINGS SET idealSeason = 'PRIMAVERA' WHERE idealSeason IS NULL AND housingCode IN (10005, 10007);

-- ---------------------------------------------------------------------------
-- Coordenadas de los alojamientos de ejemplo (F17)
--
-- Alimentan la previsión meteorológica de la ficha, y mañana el mapa. Sin ellas
-- ese bloque sencillamente no aparece, que es el comportamiento correcto para un
-- alojamiento sin localizar pero deja los diez de ejemplo sin enseñar la
-- función.
--
-- POR QUÉ VAN COMO UPDATE Y NO DENTRO DE LOS INSERT DE ARRIBA, que es lo que
-- parecería natural. Los INSERT están protegidos por WHERE NOT EXISTS, así que
-- en una base que ya existe NO SE EJECUTAN NUNCA: añadir las columnas allí solo
-- serviría para instalaciones nuevas y dejaría sin coordenadas a quien ya tenga
-- la aplicación. Es la regla dura del proyecto —corregir el proceso que genera
-- un dato no corrige el dato ya generado— y ya se aprendió tres veces: con las
-- contraseñas en texto plano, con los acentos y con el campo image.
--
-- Un UPDATE guardado por "latitude IS NULL" cubre LOS DOS CASOS con un solo
-- mecanismo: en una base nueva las columnas acaban de nacer vacías, y en una
-- vieja también. Y respeta a quien haya movido el punto desde la aplicación,
-- porque entonces ya no es nulo y la condición no se cumple.
--
-- Los valores son el centro aproximado de cada sitio, con cuatro decimales:
-- ~11 metros, de sobra para una previsión meteorológica, cuya rejilla ronda el
-- kilómetro. Más decimales serían una precisión que el dato no tiene.
-- ---------------------------------------------------------------------------

UPDATE HOUSINGS SET latitude = 37.0955, longitude = -3.3987 WHERE latitude IS NULL AND housingCode = 10001;
UPDATE HOUSINGS SET latitude = 36.7213, longitude = -4.4214 WHERE latitude IS NULL AND housingCode = 10002;
UPDATE HOUSINGS SET latitude = 38.9067, longitude =  1.4206 WHERE latitude IS NULL AND housingCode = 10003;
UPDATE HOUSINGS SET latitude = 43.1975, longitude = -4.8517 WHERE latitude IS NULL AND housingCode = 10004;
UPDATE HOUSINGS SET latitude = 41.3874, longitude =  2.1686 WHERE latitude IS NULL AND housingCode = 10005;
UPDATE HOUSINGS SET latitude = 28.1235, longitude = -15.4363 WHERE latitude IS NULL AND housingCode = 10006;
UPDATE HOUSINGS SET latitude = 40.4168, longitude = -3.7038 WHERE latitude IS NULL AND housingCode = 10007;
UPDATE HOUSINGS SET latitude = 36.5101, longitude = -4.8825 WHERE latitude IS NULL AND housingCode = 10008;
UPDATE HOUSINGS SET latitude = 36.7423, longitude = -5.1673 WHERE latitude IS NULL AND housingCode = 10009;
UPDATE HOUSINGS SET latitude = 42.7797, longitude = -0.3211 WHERE latitude IS NULL AND housingCode = 10010;

-- ---------------------------------------------------------------------------
-- Intercambios abiertos de ejemplo (Fase 8.4)
--
-- Alimentan la lista de "intercambios abiertos ahora mismo" de la pantalla de
-- intercambio. Sin ellos, esa pantalla sigue estando vacía para quien todavía no
-- tiene alojamientos, que es justo a quien hay que convencer de publicar uno.
--
-- Van como UPDATE al final por lo mismo que las fotos y la estación ideal: los
-- INSERT están protegidos por WHERE NOT EXISTS y en una base ya creada no se
-- ejecutan nunca. La condición "openToExchange = FALSE" lo hace idempotente y
-- respeta a quien haya retirado su oferta desde la aplicación.
--
-- Son de tres propietarios distintos (Marcos, Lucia, Elena) a propósito: la
-- lista tiene que enseñar variedad de gente, no el tablón de una sola persona.
-- ---------------------------------------------------------------------------

UPDATE HOUSINGS SET openToExchange = TRUE, exchangeWanted = 'una casa rural para agosto'
WHERE openToExchange = FALSE AND housingCode = 10005;

UPDATE HOUSINGS SET openToExchange = TRUE, exchangeWanted = 'un apartamento urbano con buena conexión'
WHERE openToExchange = FALSE AND housingCode = 10008;

UPDATE HOUSINGS SET openToExchange = TRUE, exchangeWanted = 'una cabaña de montaña en invierno'
WHERE openToExchange = FALSE AND housingCode = 10009;

-- ---------------------------------------------------------------------------
-- Fotos de galería de los alojamientos de ejemplo (Fase 8.4)
--
-- Las prepara ui/dev/GenerarAssets desde assets/References/Alojamientos de
-- ejemplo; son de Unsplash y están acreditadas en CREDITOS.md.
--
-- Aquí sí hacen falta INSERT y no un UPDATE, porque son filas nuevas de una
-- tabla nueva: no hay ninguna fila previa que poner al día. El WHERE NOT EXISTS
-- de siempre los hace idempotentes, y comprueba el par (alojamiento, archivo)
-- en lugar de solo el archivo: el nombre lleva el código del alojamiento, así
-- que no puede repetirse entre alojamientos, pero comprobar el par deja la
-- condición correcta aunque esa convención cambie.
-- ---------------------------------------------------------------------------

INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10001-2.jpg', 1 FROM HOUSINGS h WHERE h.housingCode = 10001
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10001-2.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10001-3.jpg', 2 FROM HOUSINGS h WHERE h.housingCode = 10001
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10001-3.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10001-4.jpg', 3 FROM HOUSINGS h WHERE h.housingCode = 10001
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10001-4.jpg');

INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10002-2.jpg', 1 FROM HOUSINGS h WHERE h.housingCode = 10002
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10002-2.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10002-3.jpg', 2 FROM HOUSINGS h WHERE h.housingCode = 10002
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10002-3.jpg');

INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10003-2.jpg', 1 FROM HOUSINGS h WHERE h.housingCode = 10003
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10003-2.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10003-3.jpg', 2 FROM HOUSINGS h WHERE h.housingCode = 10003
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10003-3.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10003-4.jpg', 3 FROM HOUSINGS h WHERE h.housingCode = 10003
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10003-4.jpg');

INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10004-2.jpg', 1 FROM HOUSINGS h WHERE h.housingCode = 10004
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10004-2.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10004-3.jpg', 2 FROM HOUSINGS h WHERE h.housingCode = 10004
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10004-3.jpg');

INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10005-2.jpg', 1 FROM HOUSINGS h WHERE h.housingCode = 10005
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10005-2.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10005-3.jpg', 2 FROM HOUSINGS h WHERE h.housingCode = 10005
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10005-3.jpg');

INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10008-2.jpg', 1 FROM HOUSINGS h WHERE h.housingCode = 10008
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10008-2.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10008-3.jpg', 2 FROM HOUSINGS h WHERE h.housingCode = 10008
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10008-3.jpg');
INSERT INTO HOUSING_PHOTOS(housingId, image, position)
SELECT h.id, '10008-4.jpg', 3 FROM HOUSINGS h WHERE h.housingCode = 10008
	AND NOT EXISTS (SELECT 1 FROM HOUSING_PHOTOS p WHERE p.housingId = h.id AND p.image = '10008-4.jpg');
-- GENERADO por ui/dev/SembrarTraducciones. No editar a mano.
--
-- Precarga la cache de traducciones con las versiones inglesas escritas a
-- mano del contenido de ejemplo. La clave es el SHA-256 del texto original,
-- que es la razon de que este fichero se genere en lugar de teclearse.
--
-- Idempotente por el WHERE NOT EXISTS de siempre: ejecutarlo en cada arranque
-- no duplica nada, y respeta cualquier traduccion ya guardada.

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '4b284e59d135cdc4dda14ae731f7aa8818db522536d8e7b61a228a9b8c5d32f1', 'en', 'A restored stone house halfway up the hillside, with a fireplace, a vegetable garden and open views of the mountains. The village is a ten-minute walk away.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '4b284e59d135cdc4dda14ae731f7aa8818db522536d8e7b61a228a9b8c5d32f1' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'ecf2ff8772b7d99a105957dbd43acfec5f01e8d558e68673327e4e87d1b7e0be', 'en', 'Second line from the beach, with a south-facing terrace and a lift. The bars and the seafront promenade are just around the corner.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'ecf2ff8772b7d99a105957dbd43acfec5f01e8d558e68673327e4e87d1b7e0be' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '3f0a2556600c1a56524429ffe2a2b80ba425225209526894902fb38fb5391ae6', 'en', 'A whitewashed villa with a private pool, a shaded porch and direct access to a small cove. Full board included.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '3f0a2556600c1a56524429ffe2a2b80ba425225209526894902fb38fb5391ae6' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'c0fe686e807d7efb6baceaf705912066aa790959ee7cacb4e19cf949f6ab962f', 'en', 'A wooden cabin for two, with a wood-burning stove and a picture window onto the beech forest. No phone signal and no neighbours: that is the whole point.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'c0fe686e807d7efb6baceaf705912066aa790959ee7cacb4e19cf949f6ab962f' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'ae374a5e6a4aa5ca9532722b97e1e2a80427ae438ad28ae335fcc8fdbf19c8f7', 'en', 'An open-plan loft in a 19th-century building, with exposed beams and four-metre ceilings. Right in the old town, walking distance from everything.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'ae374a5e6a4aa5ca9532722b97e1e2a80427ae438ad28ae335fcc8fdbf19c8f7' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'f1b7b9df15350d72b4d89df04be41baaa07d70d7c097131889ffd4b4473a227a', 'en', 'A townhouse with a patio, a barbecue and a shared pool, on a quiet development fifteen minutes from Las Canteras beach.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'f1b7b9df15350d72b4d89df04be41baaa07d70d7c097131889ffd4b4473a227a' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '630e872de77167331a99c88c4f9ef86e110bfebe42bf67412722c61e33b0a60c', 'en', 'A newly refurbished top-floor flat with a terrace, an open kitchen and air conditioning in both bedrooms. The whole neighbourhood is walkable: bars, theatres, and the Retiro fifteen minutes away.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '630e872de77167331a99c88c4f9ef86e110bfebe42bf67412722c61e33b0a60c' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '51d1dc603d134cd14deeee3385ca479b2d057b2eb601db4435a6000732fd44ce', 'en', 'A contemporary villa with an infinity pool, a panoramic terrace and sea views from both floors. Full board included.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '51d1dc603d134cd14deeee3385ca479b2d057b2eb601db4435a6000732fd44ce' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '6eabc8160883f017ec1b79fea1ba8968664658d1056bcd3e4e5cc0addab76573', 'en', 'A house with an inner courtyard in the heart of the old town, whitewashed walls and terracotta floors. The Tagus is a five-minute walk from the door.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '6eabc8160883f017ec1b79fea1ba8968664658d1056bcd3e4e5cc0addab76573' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '152dfacece8b9ac1cfb9dc16240d4813e498222530cf9dd894e783b27e383028', 'en', 'A wooden lodge with a wood-burning stove and views of the slopes from the porch. Private parking right by the door, which matters when it snows.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '152dfacece8b9ac1cfb9dc16240d4813e498222530cf9dd894e783b27e383028' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'd4ac03a708bcb1f93f6ad463d9ebaf8e26e85e62c5703f263444ec0e7d0380a8', 'en', 'Quiet, and a good breakfast'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'd4ac03a708bcb1f93f6ad463d9ebaf8e26e85e62c5703f263444ec0e7d0380a8' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'c992cb3ebd0015b416016ac8be1d1a8ea3837c8638a490747d0ee713a4710760', 'en', 'Perfect for switching off'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'c992cb3ebd0015b416016ac8be1d1a8ea3837c8638a490747d0ee713a4710760' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'f67a50677f98590587d8d2e6fae6a5d6a8ed88abca2bb6f6161a3ce3799422f2', 'en', 'You could not ask for more'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'f67a50677f98590587d8d2e6fae6a5d6a8ed88abca2bb6f6161a3ce3799422f2' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '55878a70d65fbff6357b58be05f95d422e882c1ef6fc27c14f163d4b7579256b', 'en', 'Very well located'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '55878a70d65fbff6357b58be05f95d422e882c1ef6fc27c14f163d4b7579256b' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'e9634286d765839035e74d6576b3183f75cf7f016c72bf21313c2609dda7faa3', 'en', 'The private cove is worth it'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'e9634286d765839035e74d6576b3183f75cf7f016c72bf21313c2609dda7faa3' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '495a72594ac651a11ad6340e219707302e94d3ca1b60de714601b25410b087c0', 'en', 'Pricey, but you enjoy every minute'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '495a72594ac651a11ad6340e219707302e94d3ca1b60de714601b25410b087c0' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'bc25023a4be473917ec34e0f52c1656702930686eb2cbf9fb6932dd41562e234', 'en', 'Genuinely rustic'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'bc25023a4be473917ec34e0f52c1656702930686eb2cbf9fb6932dd41562e234' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '9a0b9adf5ea42435b9bf03139f80f411dcea9a68718d39e2372e3763978fce7f', 'en', 'We could have used more heating'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '9a0b9adf5ea42435b9bf03139f80f411dcea9a68718d39e2372e3763978fce7f' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '527e33098b9c2c74e9e5c14813650afc8ad619f629845658c4b38beb34b153cf', 'en', 'The best spot in the neighbourhood'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '527e33098b9c2c74e9e5c14813650afc8ad619f629845658c4b38beb34b153cf' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '2a7be53f12c2ca69caefb0e55bdc28f79274f7622dc13d01087982cdf7a098bb', 'en', 'All the charm of an old building'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '2a7be53f12c2ca69caefb0e55bdc28f79274f7622dc13d01087982cdf7a098bb' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '76897d8d1573f3cb7d95e4a3afced3ffa3b768b5aca9ac3ca3c514a21f49c423', 'en', 'A good family trip'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '76897d8d1573f3cb7d95e4a3afced3ffa3b768b5aca9ac3ca3c514a21f49c423' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '650f1ef328dce6354019bf016f4e675cefebabb0cd24441c29766b011371e1f8', 'en', 'Solid from start to finish'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '650f1ef328dce6354019bf016f4e675cefebabb0cd24441c29766b011371e1f8' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'b4f16d50b2c54631dffdd861f3b8c3a630b88f9a0fafdf7f8ca41cb459c0d066', 'en', 'Unbeatable location'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'b4f16d50b2c54631dffdd861f3b8c3a630b88f9a0fafdf7f8ca41cb459c0d066' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '0416309176ffde98caaeb795c253be436b82dcae2ba52099fdefe125dbee04bf', 'en', 'Small, but very well thought out'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '0416309176ffde98caaeb795c253be436b82dcae2ba52099fdefe125dbee04bf' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'afd944648df3bbf97ef90322514aa56c05a02e8e4313a570a9106c1930f19a82', 'en', 'Worth every euro'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'afd944648df3bbf97ef90322514aa56c05a02e8e4313a570a9106c1930f19a82' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '9d22a2dc9e63774481e56f2a4f21db7e070cee4660d6392f274a698c87b11d40', 'en', 'Luxury without the showing off'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '9d22a2dc9e63774481e56f2a4f21db7e070cee4660d6392f274a698c87b11d40' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '26aaffc227693beef1e106c742c81d88d948ddeb3a9879b9e181531e51b4b841', 'en', 'The courtyard is another world'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '26aaffc227693beef1e106c742c81d88d948ddeb3a9879b9e181531e51b4b841' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '90a06faad51b9475f1523caed69c27bd0c8df37ccd469872ebc35960e58f525b', 'en', 'The real thing'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '90a06faad51b9475f1523caed69c27bd0c8df37ccd469872ebc35960e58f525b' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT '7bb48fbdc1b143cdc643cdfa41d89ed7ae915a6ebbc4f1ff29b3dd81a4b1edb9', 'en', 'Complete disconnection'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = '7bb48fbdc1b143cdc643cdfa41d89ed7ae915a6ebbc4f1ff29b3dd81a4b1edb9' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'abe4d9e634d5bc51c8aa8643e1ad081e18dd3e9bdeab213940164de84e92f917', 'en', 'Ideal for a ski trip'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'abe4d9e634d5bc51c8aa8643e1ad081e18dd3e9bdeab213940164de84e92f917' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'fd67f143c04b01e374cfd898e2ae7b569a0c8c4e0f6ae27421a2eb91f561e4dd', 'en', 'a country house for August'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'fd67f143c04b01e374cfd898e2ae7b569a0c8c4e0f6ae27421a2eb91f561e4dd' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'fdd44215fd49478afe7abb2e826fd8485faeb7fdcacb8ebb896eeb6d5e05b645', 'en', 'a city flat with good transport links'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'fdd44215fd49478afe7abb2e826fd8485faeb7fdcacb8ebb896eeb6d5e05b645' AND targetLanguage = 'en');

INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)
SELECT 'fbf9dfb88e48bafd1d2a23512981bf49b2303bee62fc65071e33d75510424871', 'en', 'a mountain cabin in winter'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS WHERE sourceHash = 'fbf9dfb88e48bafd1d2a23512981bf49b2303bee62fc65071e33d75510424871' AND targetLanguage = 'en');

