package fp.project.actihome.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.FontMetrics;

import org.junit.jupiter.api.Test;


/**
 * Comprueba lo único que de verdad hay que garantizar de {@link WrappingText}:
 * que su mínimo es <b>una función pura del componente</b>.
 *
 * <p>
 * <b>Estos tests son un fallo real, no un ejercicio.</b> Durante las fases 8.11
 * y 8.12 la comprobación de recortes del CI fallaba tres de cada seis
 * ejecuciones sobre la misma pantalla y sin tocar una línea de código. La causa
 * era que {@code getMinimumSize()} devolvía {@code getPreferredSize().height}, y
 * el alto preferido de un {@code JTextArea} con ajuste de línea <b>depende de la
 * última anchura que recibió su vista interna</b>, que va cambiando mientras el
 * gestor de layout tantea tamaños: el mismo componente contestaba 18 o 36 según
 * el instante en que se le preguntara.
 *
 * <p>
 * De ahí la forma del segundo test, que es la parte que importa: preguntar el
 * mínimo, cambiarle el tamaño al componente y volver a preguntar. Si la
 * respuesta cambia, no es un mínimo — es una medición, y el CI vuelve a ser una
 * moneda.
 */
class WrappingTextTest {


	private static final String PARRAFO = "Una casa tranquila con vistas al valle, perfecta para "
			+ "desconectar durante unos dias sin renunciar a nada.";

	@Test
	void elMinimoDeAnchoEsLaPalabraMasLargaYNoElParrafoEntero() {

		WrappingText parrafo = new WrappingText(PARRAFO);
		FontMetrics metrica = parrafo.getFontMetrics(parrafo.getFont());

		int palabraMasLarga = 0;

		for (String palabra : PARRAFO.split(" ")) {
			palabraMasLarga = Math.max(palabraMasLarga, metrica.stringWidth(palabra));
		}

		int minimo = parrafo.getMinimumSize().width;

		// Por debajo del elemento indivisible mas ancho no hay reflujo posible; por
		// encima siempre se puede repartir. Es el mismo razonamiento que FilaFluida.
		assertEquals(palabraMasLarga, minimo);

		// Y sobre todo: NO es el parrafo entero en una linea, que es lo que contesta
		// un JTextArea al que nadie le ha dado ancho todavia.
		assertTrue(minimo < metrica.stringWidth(PARRAFO) / 2,
				"el minimo no puede acercarse al ancho del parrafo entero: era " + minimo);
	}

	@Test
	void elMinimoNoCambiaAunqueElComponenteCambieDeTamano() {

		WrappingText parrafo = new WrappingText(PARRAFO);

		Dimension reciennacido = parrafo.getMinimumSize();

		parrafo.setSize(60, 400);
		Dimension estrecho = parrafo.getMinimumSize();

		parrafo.setSize(2000, 20);
		Dimension ancho = parrafo.getMinimumSize();

		assertEquals(reciennacido, estrecho, "el minimo cambio al estrechar el componente");
		assertEquals(reciennacido, ancho, "el minimo cambio al ensanchar el componente");
	}

	@Test
	void elAltoMinimoEsUnaSolaLinea() {

		WrappingText parrafo = new WrappingText(PARRAFO);
		FontMetrics metrica = parrafo.getFontMetrics(parrafo.getFont());

		assertEquals(metrica.getHeight(), parrafo.getMinimumSize().height);
	}
}
