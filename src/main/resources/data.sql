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
-- NOTA: la contraseña de estos usuarios está en texto plano, así que NO pueden
-- iniciar sesión (el login usa BCrypt). Son datos para poblar el catálogo; para
-- entrar hay que registrarse.
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

-- Propietarios de los alojamientos de ejemplo. Son ADMIN porque en ActiHome
-- solo ese rol puede publicar. Tener varios permite ver en el catálogo la línea
-- de "propietario" con valores distintos, y probar que editar e intercambiar
-- respetan la titularidad.

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Lucia', '1234', 'Lucía', 'Ferreiro', 'Granada', 655101202, 'lucia@actihome.example', '1986-04-17', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Lucia');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Marcos', '1234', 'Marcos', 'Iglesias', 'Málaga', 655101203, 'marcos@actihome.example', '1981-09-02', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Marcos');

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
SELECT 'Elena', '1234', 'Elena', 'Prado', 'Oviedo', 655101204, 'elena@actihome.example', '1990-11-25', 'ADMIN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE username = 'Elena');

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

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, available, location, ownerId)
SELECT 10001, 'Casa Rural El Pinar', 'Casa', 3, 75.00,
	'Casa de piedra restaurada a media ladera, con chimenea, huerto y vistas abiertas a la sierra. El pueblo queda a diez minutos a pie.',
	NULL, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, NULL, TRUE, 'Sierra Nevada, Granada',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10001);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, available, location, ownerId)
SELECT 10002, 'Apartamento Playa Centro', 'Apartamento', 2, 95.00,
	'Segunda línea de playa, con terraza orientada al sur y ascensor. La zona de bares y el paseo marítimo quedan al doblar la esquina.',
	NULL, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, TRUE, 'Málaga',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10002);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, available, location, ownerId)
SELECT 10003, 'Villa Mediterráneo', 'Villa', 5, 320.00,
	'Villa encalada con piscina privada, porche de sombra y acceso directo a una cala pequeña. Pensión completa incluida.',
	NULL, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, FALSE, 'Ibiza',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10003);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, available, location, ownerId)
SELECT 10004, 'Cabaña del Bosque', 'Cabaña', 1, 48.00,
	'Cabaña de madera para dos, con estufa de leña y ventanal al hayedo. Sin cobertura y sin vecinos: ese es el plan.',
	NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, NULL, TRUE, 'Picos de Europa, Asturias',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10004);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, available, location, ownerId)
SELECT 10005, 'Loft Barrio Gótico', 'Apartamento', 2, 130.00,
	'Loft diáfano en un edificio del XIX, con vigas vistas y techos de cuatro metros. En pleno casco antiguo, a paso de todo.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, TRUE, 'Barcelona',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10005);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, available, location, ownerId)
SELECT 10006, 'Casa Adosada Las Palmas', 'Casa', 4, 110.00,
	'Adosado con patio, barbacoa y piscina comunitaria, en una urbanización tranquila a quince minutos de la playa de Las Canteras.',
	NULL, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, TRUE, 'Gran Canaria',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10006);

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
