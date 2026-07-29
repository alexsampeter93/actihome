package fp.project.actihome.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de la reparación de acentos.
 *
 * <p>
 * No levantan Spring: lo único que hay que verificar es la regla que decide si
 * un texto está corrompido, y esa es una función pura. Lo importante aquí no es
 * que arregle lo roto —eso es fácil— sino que <b>no toque lo que está bien</b>,
 * porque un falso positivo corrompería datos correctos del usuario.
 */
class ReparacionDeAcentosTests {

	@Test
	void reparaLosTextosSembradosQueSeCorrompieron() {

		assertEquals("Málaga", ReparacionDeAcentos.reparar("MÃ¡laga"));
		assertEquals("Cabaña del Bosque", ReparacionDeAcentos.reparar("CabaÃ±a del Bosque"));
		assertEquals("Loft Barrio Gótico", ReparacionDeAcentos.reparar("Loft Barrio GÃ³tico"));
		assertEquals("Villa Mediterráneo", ReparacionDeAcentos.reparar("Villa MediterrÃ¡neo"));
		assertEquals("Coruña", ReparacionDeAcentos.reparar("CoruÃ±a"));
		assertEquals("Lucía", ReparacionDeAcentos.reparar("LucÃ­a"));
	}

	@Test
	void noTocaUnTextoConAcentosCorrectos() {

		// Es el caso que de verdad importa: son los datos que el usuario ha escrito
		// desde la aplicación, que siempre se guardaron bien.
		assertNull(ReparacionDeAcentos.reparar("A Coruña"));
		assertNull(ReparacionDeAcentos.reparar("Lucía"));
		assertNull(ReparacionDeAcentos.reparar("Málaga"));
	}

	@Test
	void noTocaUnTextoSinAcentos() {

		assertNull(ReparacionDeAcentos.reparar("Casa Rural El Pinar"));
		assertNull(ReparacionDeAcentos.reparar("Tarjeta de credito"));
		assertNull(ReparacionDeAcentos.reparar(""));
	}

	@Test
	void esIdempotente() {

		// Ejecutarla dos veces no puede volver a cambiar nada: si lo hiciera, cada
		// arranque degradaría un poco más los datos.
		String reparado = ReparacionDeAcentos.reparar("MÃ¡laga");

		assertEquals("Málaga", reparado);
		assertNull(ReparacionDeAcentos.reparar(reparado));
	}

	@Test
	void noSeInventaNadaConSecuenciasQueNoSonUtf8() {

		// "Ã" suelta no es el principio de ninguna secuencia UTF-8 válida de dos
		// bytes seguida de esa letra, así que el candidato tendría carácter de
		// sustitución y hay que descartarlo.
		assertNull(ReparacionDeAcentos.reparar("Ã"));
		assertNull(ReparacionDeAcentos.reparar("100 % algodón".replace('ó', 'o')));
	}
}
