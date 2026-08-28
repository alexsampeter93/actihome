package fp.project.actihome.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.FontMetrics;

import org.junit.jupiter.api.Test;


/**
 * Comprueba lo único que de verdad hay que garantizar de {@link WrappingText}:
 * que su mínimo <b>no se mueve bajo los pies del gestor de layout</b>.
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
 * <b>Esta clase afirmaba antes algo más fuerte —«el mínimo no cambia nunca»— y
 * era demasiado fuerte.</b> Un párrafo que ya sabe que ocupa dos líneas no puede
 * seguir diciendo que le basta con una: {@link Rescate} aprieta la pantalla
 * antes de sacar la barra y se lo cobra dibujando la segunda línea cortada por
 * la mitad. Así que el alto mínimo sí sube en cuanto hay un ancho asignado, y lo
 * que hay que proteger es más fino y más exacto:
 *
 * <ol>
 * <li>el mínimo de <b>ancho</b> no cambia jamás — es la palabra más larga, y de
 * ahí no baja nada;</li>
 * <li>el mínimo de <b>alto</b> es una línea mientras nadie haya dado un ancho, y
 * a partir de ahí <b>es estable para un ancho dado</b>. Eso es lo que impedía
 * que el CI fuera una moneda: MigLayout pregunta muchas veces sin cambiar los
 * límites, y todas las respuestas tienen que coincidir.</li>
 * </ol>
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
	void elMinimoDeAnchoNoCambiaAunqueElComponenteCambieDeTamano() {

		WrappingText parrafo = new WrappingText(PARRAFO);

		int reciennacido = parrafo.getMinimumSize().width;

		parrafo.setSize(60, 400);
		int estrecho = parrafo.getMinimumSize().width;

		parrafo.setSize(2000, 20);
		int ancho = parrafo.getMinimumSize().width;

		assertEquals(reciennacido, estrecho, "el minimo de ancho cambio al estrechar el componente");
		assertEquals(reciennacido, ancho, "el minimo de ancho cambio al ensanchar el componente");
	}

	@Test
	void elAltoMinimoEsUnaSolaLineaMientrasNadieLeHayaDadoAncho() {

		WrappingText parrafo = new WrappingText(PARRAFO);
		FontMetrics metrica = parrafo.getFontMetrics(parrafo.getFont());

		assertEquals(metrica.getHeight(), parrafo.getMinimumSize().height);
	}

	@Test
	void elAltoMinimoNoSeMuevePreguntandoVariasVecesConElMismoAncho() {

		WrappingText parrafo = new WrappingText(PARRAFO);
		parrafo.setSize(240, 500);

		int primera = parrafo.getMinimumSize().height;

		// Diez preguntas seguidas, que es lo que hace un gestor de layout mientras
		// tantea. Si alguna contesta distinta sin que nadie haya cambiado los limites,
		// el detector de recortes del CI vuelve a fallar la mitad de las veces.
		for (int i = 0; i < 10; i++) {
			assertEquals(primera, parrafo.getMinimumSize().height, "el alto minimo cambio en la pregunta " + i);
		}
	}

	@Test
	void elAltoMinimoCreceCuandoElParrafoYaSabeQueOcupaVariasLineas() {

		WrappingText parrafo = new WrappingText(PARRAFO);
		FontMetrics metrica = parrafo.getFontMetrics(parrafo.getFont());

		// Un ancho estrecho de verdad: este parrafo no cabe ahi en una sola linea.
		parrafo.setSize(200, 500);

		// La regla que costo un texto cortado por la mitad en el detalle de una resena:
		// un parrafo puede ceder ancho, porque refluye, y no puede ceder alto.
		assertTrue(parrafo.getMinimumSize().height > metrica.getHeight(),
				"un parrafo que ocupa varias lineas no puede declarar que le basta con una");
	}
}
