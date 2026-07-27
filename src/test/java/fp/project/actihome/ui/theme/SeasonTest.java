package fp.project.actihome.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Comprueba el cálculo de la estación a partir de la fecha.
 *
 * <p>
 * Es un test de JUnit puro: no lleva {@code @SpringBootTest}, así que no
 * levanta el contexto de Spring ni necesita base de datos. Se ejecuta en
 * milisegundos y en cualquier máquina.
 *
 * <p>
 * Se prueba {@code actualPara(fecha)} en vez de {@code actual()} precisamente
 * para poder fijar la fecha: un test que dependiera del reloj del sistema daría
 * resultados distintos según el día en que se ejecute, que es la definición de
 * test inútil.
 */
class SeasonTest {

	@Test
	void cadaEstacionEmpiezaYAcabaCuandoDebe() {

		assertEquals(Season.PRIMAVERA, Season.actualPara(LocalDate.of(2026, 3, 20)));
		assertEquals(Season.PRIMAVERA, Season.actualPara(LocalDate.of(2026, 6, 20)));

		assertEquals(Season.VERANO, Season.actualPara(LocalDate.of(2026, 6, 21)));
		assertEquals(Season.VERANO, Season.actualPara(LocalDate.of(2026, 9, 22)));

		assertEquals(Season.OTONO, Season.actualPara(LocalDate.of(2026, 9, 23)));
		assertEquals(Season.OTONO, Season.actualPara(LocalDate.of(2026, 12, 20)));

		assertEquals(Season.INVIERNO, Season.actualPara(LocalDate.of(2026, 12, 21)));
		assertEquals(Season.INVIERNO, Season.actualPara(LocalDate.of(2026, 1, 15)));
		assertEquals(Season.INVIERNO, Season.actualPara(LocalDate.of(2026, 3, 19)));
	}

	@Test
	void unDiaCualquieraDeJulioEsVerano() {

		assertEquals(Season.VERANO, Season.actualPara(LocalDate.of(2026, 7, 27)));
	}

	@Test
	void todasLasEstacionesTienenSusSieteTokensYSuEtiqueta() {

		for (Season estacion : Season.values()) {

			assertEquals(255, estacion.acc().getAlpha(), estacion + ": el acento debe ser opaco");
			assertEquals(255, estacion.bg().getAlpha(), estacion + ": el fondo debe ser opaco");
			assertEquals(255, estacion.hdr().getAlpha(), estacion + ": la cabecera debe ser opaca");
			assertEquals(255, estacion.txt().getAlpha(), estacion + ": el texto debe ser opaco");
			assertEquals(255, estacion.mut().getAlpha(), estacion + ": el secundario debe ser opaco");
			assertEquals(255, estacion.img().getAlpha(), estacion + ": el hueco de foto debe ser opaco");

			// El velo sí es translúcido: es su razón de ser.
			org.junit.jupiter.api.Assertions.assertTrue(estacion.imgTint().getAlpha() < 255,
					estacion + ": el velo de foto debe ser translúcido");

			org.junit.jupiter.api.Assertions.assertFalse(estacion.etiqueta().isEmpty());
		}
	}

	@Test
	void laFraseSeQuedaSoloConLaParteDespuesDelGuion() {

		assertEquals("Sol alto, luz dorada y sombra fresca", Season.VERANO.frase());
		assertEquals("Hojas, viñedos y tardes doradas", Season.OTONO.frase());

		// Todas deben tener parte poética: si alguna etiqueta se escribiera sin guion,
		// frase() devolvería la etiqueta entera y el panel del login se cortaría.
		for (Season estacion : Season.values()) {
			org.junit.jupiter.api.Assertions.assertNotEquals(estacion.etiqueta(), estacion.frase(),
					estacion + ": la etiqueta debe llevar guion largo separando la frase");
		}
	}

	@Test
	void otonoYVeranoSonColoresDistinguibles() {

		// Los valores del handoff hacían las dos estaciones casi idénticas (ADR-005).
		// Este test fija la corrección: si alguien vuelve a acercarlas, salta.
		java.awt.Color verano = Season.VERANO.acc();
		java.awt.Color otono = Season.OTONO.acc();

		int distancia = Math.abs(verano.getRed() - otono.getRed())
				+ Math.abs(verano.getGreen() - otono.getGreen())
				+ Math.abs(verano.getBlue() - otono.getBlue());

		org.junit.jupiter.api.Assertions.assertTrue(distancia > 90,
				"Verano y otoño deben distinguirse a simple vista; distancia actual: " + distancia);
	}

	@Test
	void elHexadecimalSeFormateaComoLoEsperaFlatLaf() {

		assertEquals("#4E7A3E", Season.hex(Season.PRIMAVERA.acc()));
		assertEquals("#FBF3E1", Season.hex(Season.VERANO.bg()));
	}
}
