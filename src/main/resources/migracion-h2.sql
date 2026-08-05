-- Migración del esquema para bases de datos H2 que ya existen.
--
-- POR QUÉ HACE FALTA ESTE ARCHIVO
--
-- schema.sql crea las tablas con "CREATE TABLE IF NOT EXISTS". Eso protege los
-- datos —no borra nada al arrancar— pero tiene una consecuencia que no es obvia:
-- si la tabla ya existe, el CREATE se salta ENTERO. Las columnas nuevas que se
-- añadan a ese CREATE solo aparecen en bases de datos creadas desde cero.
--
-- Para quien ya tenía la aplicación instalada, su ~/.actihome/actihome.mv.db se
-- quedaría con el esquema viejo, y como Hibernate arranca con "ddl-auto:
-- validate" la aplicación ni siquiera abriría: fallaría diciendo que la entidad
-- Housing tiene propiedades sin columna. Este archivo pone al día esas bases.
--
-- POR QUÉ ESTÁ SEPARADO Y SOLO SE EJECUTA EN H2
--
-- "ALTER TABLE ... ADD COLUMN IF NOT EXISTS" lo entienden H2 y MariaDB, pero
-- NO MySQL 8: allí es un error de sintaxis, y un error de sintaxis rompe el
-- arranque aunque la columna ya estuviera. Manteniéndolo fuera de schema.sql y
-- declarándolo solo en los perfiles que usan H2, el perfil "mysql" sigue
-- funcionando igual que antes. Quien tenga una base MySQL anterior a la Fase 3a
-- tiene que aplicar estos mismos ALTER a mano una vez, sin el IF NOT EXISTS.
--
-- Es idempotente: ejecutarlo en cada arranque no hace nada si ya está aplicado.
--
-- NOTA PARA MÁS ADELANTE: este archivo es un parche puntual, no un sistema de
-- migraciones. Cuando el esquema vuelva a cambiar tocará valorar Flyway o
-- Liquibase, que llevan la cuenta de qué migraciones se han aplicado en lugar de
-- depender de que cada sentencia sepa comprobarse a sí misma.

ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS name VARCHAR(80) DEFAULT '' NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS image VARCHAR(120);
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS pool BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS wifi BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS tv BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS parking BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS airConditioning BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS pets BOOLEAN DEFAULT FALSE NOT NULL;

-- Los alojamientos que ya existían se quedan con el nombre vacío, porque hasta
-- ahora no había dónde guardarlo. Se rellena con el tipo, que es lo más parecido
-- a un nombre que tenían ("Casa en la playa"), para que la ficha no salga sin
-- título. Es reversible: el propietario puede editarlo.
UPDATE HOUSINGS SET name = type WHERE name = '';

-- Fase 7.5: la disponibilidad deja de ser una columna guardada y pasa a
-- calcularse desde las reservas activas (ver CLAUDE.md, bug B5). En una base ya
-- creada, "available" seguía existiendo como BOOLEAN NOT NULL sin valor por
-- defecto: dejar de mapearla en la entidad no rompe el arranque (ddl-auto:
-- validate solo mira que lo que la entidad pide exista), pero sí el primer
-- INSERT que hiciera Hibernate al publicar un alojamiento nuevo, porque esa
-- columna huérfana seguiría exigiendo un valor. Quien tenga una base MySQL
-- anterior a esta fase necesita aplicar a mano:
--     ALTER TABLE HOUSINGS DROP COLUMN available;
ALTER TABLE HOUSINGS DROP COLUMN IF EXISTS available;

-- Fase 7.5.3: cancelar una reserva. Quien tenga una base MySQL anterior a
-- esta fase necesita aplicar a mano:
--     ALTER TABLE RESERVATIONS ADD COLUMN cancelled BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE RESERVATIONS ADD COLUMN IF NOT EXISTS cancelled BOOLEAN DEFAULT FALSE NOT NULL;

-- Fase 7.6: pantalla de Ajustes (estación por defecto, partículas, idioma).
-- Quien tenga una base MySQL anterior a esta fase necesita aplicar a mano:
--     ALTER TABLE USERS ADD COLUMN defaultSeason VARCHAR(20);
--     ALTER TABLE USERS ADD COLUMN particlesEnabled BOOLEAN DEFAULT TRUE NOT NULL;
--     ALTER TABLE USERS ADD COLUMN language VARCHAR(5) DEFAULT 'ES' NOT NULL;
ALTER TABLE USERS ADD COLUMN IF NOT EXISTS defaultSeason VARCHAR(20);
ALTER TABLE USERS ADD COLUMN IF NOT EXISTS particlesEnabled BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE USERS ADD COLUMN IF NOT EXISTS language VARCHAR(5) DEFAULT 'ES' NOT NULL;

-- Fase 7.8: pantalla de bienvenida. Quien tenga una base MySQL anterior a esta
-- fase necesita aplicar a mano:
--     ALTER TABLE USERS ADD COLUMN onboardingSeen BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE USERS ADD COLUMN IF NOT EXISTS onboardingSeen BOOLEAN DEFAULT FALSE NOT NULL;

-- Fase 7.11: vista de catálogo por defecto. Quien tenga una base MySQL
-- anterior a esta fase necesita aplicar a mano:
--     ALTER TABLE USERS ADD COLUMN defaultGridView BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE USERS ADD COLUMN IF NOT EXISTS defaultGridView BOOLEAN DEFAULT FALSE NOT NULL;

-- F15: reseñas enriquecidas (foto adjunta, respuesta pública del propietario).
-- Quien tenga una base MySQL anterior a esta fase necesita aplicar a mano:
--     ALTER TABLE REVIEWS ADD COLUMN image VARCHAR(120);
--     ALTER TABLE REVIEWS ADD COLUMN ownerResponse VARCHAR(500);
--     ALTER TABLE REVIEWS ADD COLUMN ownerResponseDate DATETIME;
ALTER TABLE REVIEWS ADD COLUMN IF NOT EXISTS image VARCHAR(120);
ALTER TABLE REVIEWS ADD COLUMN IF NOT EXISTS ownerResponse VARCHAR(500);
ALTER TABLE REVIEWS ADD COLUMN IF NOT EXISTS ownerResponseDate DATETIME;

-- Fase 8.4: estación ideal del alojamiento, para el distintivo "Ideal en {estación}"
-- del catálogo. Quien tenga una base MySQL anterior a esta fase necesita aplicar
-- a mano:
--     ALTER TABLE HOUSINGS ADD COLUMN idealSeason VARCHAR(20);
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS idealSeason VARCHAR(20);

-- Fase 8.4: intercambio abierto. Un propietario puede declarar que acepta
-- permutar un alojamiento y qué busca a cambio, y eso alimenta la lista de
-- "intercambios abiertos ahora mismo" de la pantalla de intercambio. Quien tenga
-- una base MySQL anterior a esta fase necesita aplicar a mano:
--     ALTER TABLE HOUSINGS ADD COLUMN openToExchange BOOLEAN DEFAULT FALSE NOT NULL;
--     ALTER TABLE HOUSINGS ADD COLUMN exchangeWanted VARCHAR(120);
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS openToExchange BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE HOUSINGS ADD COLUMN IF NOT EXISTS exchangeWanted VARCHAR(120);
