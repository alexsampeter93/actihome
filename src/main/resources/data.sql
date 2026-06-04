INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
VALUES("Admin", "1234", "Alejandro", "Sampedro", "Coruña", 666777892, "alejsamcalo@gmail.com", "1993-01-03", 0); 

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
VALUES("Customer", "1234", "Jorge", "Sampedro", "Coruña", 664567076, "jorgesamcalo@gmail.com", "1999-12-09", 1); 

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
VALUES("Customer10", "1234", "Jorge", "Sampedro", "Coruña", 664567076, "jorgesamcalo@gmail.com", "1999-12-09", 1); 

INSERT INTO USERS(username, password, name, surname, locality, phoneNumber, email, birthDate, role)
VALUES("Customer16", "1234", "Jorge", "Sampedro", "Coruña", 664567076, "jorgesamcalo@gmail.com", "1999-12-09", 1); 

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
VALUES(204183, "Casa en la playa", 6, 40.42, "Descripcion", TRUE, FALSE, TRUE, NULL, TRUE, "Andalucia", 1);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
VALUES(204184, "Casa en la playa", 6, 40.42, "Descripcion", TRUE, FALSE, TRUE, NULL, TRUE, "Andalucia", 1);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
VALUES(204163, "Casa en la playa", 6, 40.42, "Descripcion", TRUE, FALSE, TRUE, NULL, TRUE, "Andalucia", 1);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
VALUES(204132, "Casa en la playa", 6, 40.42, "Descripcion", TRUE, FALSE, TRUE, NULL, TRUE, "Andalucia", 1);

INSERT INTO HOUSINGS(housingCode, type, numberOfRooms, pricePerNight, description, breakfast, lunch, dinner, score, available, location, ownerId)
VALUES(2042183, "Casa en la playa", 6, 40.42, "Descripcion", TRUE, FALSE, TRUE, NULL, TRUE, "Andalucia", 1);

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
VALUES("Titulo1", "Cuerpo", 4, 4, 4, 4, 4, 4, "2026-02-04", 2, 1);

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
VALUES("Titulo1", "Cuerpo", 4, 4, 4, 4, 4, 4, "2026-02-04", 3, 1);

INSERT INTO REVIEWS(title, body, locationScore, serviceScore, wifiScore, foodScore, cleaningScore, totalScore, publicationDate, authorId, housingId)
VALUES("Titulo1", "Cuerpo", 4, 4, 4, 4, 4, 4, "2026-02-04", 4, 1);

INSERT INTO RESERVATIONS(reservationCode, checkIn, checkOut, paymentMethod, reservationDate, totalPrice, checkedIn, customerId, housingId)
VALUES("3910", "2026-02-13", "2026-02-18", "Tarjeta", "2026-02-05", 50.50, TRUE, 2, 1);

INSERT INTO RESERVATIONS(reservationCode, checkIn, checkOut, paymentMethod, reservationDate, totalPrice, checkedIn, customerId, housingId)
VALUES("3915", "2026-03-13", "2026-03-18", "Tarjeta", "2026-02-05", 50.50, TRUE, 2, 2);

INSERT INTO RESERVATIONS(reservationCode, checkIn, checkOut, paymentMethod, reservationDate, totalPrice, checkedIn, customerId, housingId)
VALUES("3920", "2026-05-05 03:00:00", "2026-05-18", "Tarjeta", "2026-01-05", 50.50, FALSE, 3, 3);