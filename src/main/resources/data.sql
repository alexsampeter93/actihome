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

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10001, 'Casa Rural El Pinar', 'Casa', 3, 75.00,
	'Casa de piedra restaurada a media ladera, con chimenea, huerto y vistas abiertas a la sierra. El pueblo queda a diez minutos a pie.',
	NULL, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, NULL, 'Sierra Nevada, Granada',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10001);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10002, 'Apartamento Playa Centro', 'Apartamento', 2, 95.00,
	'Segunda línea de playa, con terraza orientada al sur y ascensor. La zona de bares y el paseo marítimo quedan al doblar la esquina.',
	NULL, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, 'Málaga',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10002);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10003, 'Villa Mediterráneo', 'Villa', 5, 320.00,
	'Villa encalada con piscina privada, porche de sombra y acceso directo a una cala pequeña. Pensión completa incluida.',
	NULL, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, 'Ibiza',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10003);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10004, 'Cabaña del Bosque', 'Cabaña', 1, 48.00,
	'Cabaña de madera para dos, con estufa de leña y ventanal al hayedo. Sin cobertura y sin vecinos: ese es el plan.',
	NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, NULL, 'Picos de Europa, Asturias',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10004);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10005, 'Loft Barrio Gótico', 'Apartamento', 2, 130.00,
	'Loft diáfano en un edificio del XIX, con vigas vistas y techos de cuatro metros. En pleno casco antiguo, a paso de todo.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, 'Barcelona',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10005);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10006, 'Casa Adosada Las Palmas', 'Casa', 4, 110.00,
	'Adosado con patio, barbacoa y piscina comunitaria, en una urbanización tranquila a quince minutos de la playa de Las Canteras.',
	NULL, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, 'Gran Canaria',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10006);

-- Cuatro alojamientos más (Fase 7.10), para que los filtros nuevos de precio y
-- ciudad tengan algo de verdad que filtrar: sin variedad de precio ni de
-- ciudad, esos dos filtros no se pueden ni probar.
INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10007, 'Ático Malasaña', 'Apartamento', 2, 88.00,
	'Ático con terraza recién reformado, cocina abierta y aire acondicionado en las dos habitaciones. El barrio se recorre entero a pie: bares, teatros y el Retiro a quince minutos.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, NULL, 'Madrid',
	(SELECT id FROM USERS WHERE username = 'Marcos')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10007);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10008, 'Villa Costa del Sol', 'Villa', 5, 275.00,
	'Villa de líneas contemporáneas con piscina infinita, terraza panorámica y vistas al mar desde las dos plantas. Pensión completa incluida.',
	NULL, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, NULL, 'Marbella, Málaga',
	(SELECT id FROM USERS WHERE username = 'Lucia')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10008);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10009, 'Casa Patio Andaluz', 'Casa', 3, 58.00,
	'Casa con patio interior en el corazón del casco histórico, paredes encaladas y suelo de barro cocido. El Tajo queda a cinco minutos a pie desde la puerta.',
	NULL, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, NULL, 'Ronda, Málaga',
	(SELECT id FROM USERS WHERE username = 'Elena')
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM HOUSINGS WHERE housingCode = 10009);

INSERT INTO HOUSINGS(housingCode, name, type, numberOfRooms, pricePerNight, description, image,
	breakfast, lunch, dinner, pool, wifi, tv, parking, airConditioning, pets, score, location, ownerId)
SELECT 10010, 'Refugio de Nieve', 'Cabaña', 2, 68.00,
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
