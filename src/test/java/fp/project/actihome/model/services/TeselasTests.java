package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * La aritmética del mapa (F19).
 *
 * <p>
 * <b>Sin Spring, sin red y sin ventana</b>, porque {@link Teselas} es una
 * función pura. Que se pueda probar así es la razón de que esa cuenta viva en la
 * capa de servicios y no dentro del componente que pinta: metida en el
 * {@code paintComponent} habría hecho falta una ventana de verdad para
 * comprobar una multiplicación.
 */
public class TeselasTests {

	/** Con un margen de un píxel de mapa a zoom 13, que es del orden de 5 metros. */
	private static final double MARGEN = 1.0 / 256;

	@Test
	public void testElMundoCabeEnUnaTeselaEnElZoomCero() {
		assertEquals(1, Teselas.porLado(0));
		assertEquals(4, Teselas.porLado(2));
		assertEquals(8192, Teselas.porLado(13));
	}

	/**
	 * El meridiano de Greenwich y el ecuador caen en el centro exacto del mapa.
	 *
	 * <p>
	 * Es el caso que verifica que las dos fórmulas no están desplazadas: a zoom 1
	 * el mundo son 2×2 teselas, así que el punto (0,0) tiene que caer justo en la
	 * esquina donde se juntan las cuatro.
	 */
	@Test
	public void testElOrigenCaeEnElCentroDelMapa() {

		assertEquals(1.0, Teselas.columna(0, 1), MARGEN);
		assertEquals(1.0, Teselas.fila(0, 1), MARGEN);
	}

	@Test
	public void testLosExtremosDeLongitud() {

		assertEquals(0.0, Teselas.columna(-180, 5), MARGEN);
		assertEquals(Teselas.porLado(5), Teselas.columna(180, 5), MARGEN);
	}

	/**
	 * Un punto conocido: Sierra Nevada, el alojamiento 10001 del catálogo.
	 *
	 * <p>
	 * (37,0955 N; 3,3987 O) cae en la tesela <b>4018, 3185</b> a zoom 13. Así que
	 * este test comprueba de paso que las coordenadas sembradas en
	 * {@code data.sql} caen donde deben.
	 *
	 * <p>
	 * <b>Estos dos números se calcularon, no se recordaron</b>, y merece decirlo
	 * porque la primera versión de este test llevaba otros dos que sonaban
	 * igual de creíbles y estaban inventados — el test falló y <b>tenía razón el
	 * código</b>. Es el mismo error que ya está anotado en el Anexo C con el
	 * parámetro de base de datos que nunca existió: <b>un detalle plausible es más
	 * difícil de detectar que uno absurdo</b>.
	 *
	 * <p>
	 * La comprobación se hizo con la fórmula estándar escrita aparte, no llamando a
	 * la clase que se está probando —eso sería circular—. Y se puede verificar a
	 * ojo: 4018 de 8192 columnas es el 49,05 % del mundo a lo ancho, que sobre un
	 * rango de -180 a 180 da -3,4° de longitud. Correcto.
	 */
	@Test
	public void testUnPuntoConocido() {

		assertEquals(4018, (int) Teselas.columna(-3.3987, 13));
		assertEquals(3185, (int) Teselas.fila(37.0955, 13));
	}

	/**
	 * El mundo da la vuelta a lo ancho, pero no a lo alto.
	 *
	 * <p>
	 * <b>Es la asimetría que más fácil sería programar mal</b>, porque la intuición
	 * dice que los dos ejes se tratan igual. No: después del meridiano 180 viene el
	 * -180, así que a lo ancho se envuelve; por encima del polo no hay nada, así
	 * que a lo alto se recorta. Si se envolviera también en vertical, el mapa
	 * enseñaría la Antártida encima de Groenlandia.
	 */
	@Test
	public void testLaColumnaSeEnvuelveYLaFilaNo() {

		int total = Teselas.porLado(3);

		assertEquals(0, Teselas.envolverColumna(total, 3));
		assertEquals(total - 1, Teselas.envolverColumna(-1, 3));

		assertEquals(0, Teselas.acotarFila(-1, 3));
		assertEquals(total - 1, Teselas.acotarFila(total, 3));
	}

	@Test
	public void testSaberQueFilasExisten() {

		assertTrue(Teselas.filaValida(0, 3));
		assertTrue(Teselas.filaValida(7, 3));
		assertFalse(Teselas.filaValida(-1, 3));
		assertFalse(Teselas.filaValida(8, 3));
	}

	/**
	 * Cerca del polo la fórmula no revienta.
	 *
	 * <p>
	 * <b>Este es el test que de verdad importa</b>, porque el fallo que evita es
	 * silencioso. A 90° la tangente se va a infinito y {@code Math.log} devuelve
	 * {@code Infinity}; a partir de ahí todo lo que se calcule con ese número es
	 * basura que no lanza ninguna excepción — el mapa saldría en blanco y nadie
	 * sabría por qué. Por eso la latitud se acota antes de entrar en la fórmula.
	 */
	@Test
	public void testLosPolosNoDanInfinito() {

		for (double latitud : new double[] { 90, -90, 89.9, -89.9, 85.0511 }) {

			double fila = Teselas.fila(latitud, 10);

			assertTrue(Double.isFinite(fila), "latitud " + latitud + " dio " + fila);
			assertTrue(fila >= 0 && fila <= Teselas.porLado(10), "latitud " + latitud + " se salio del mapa");
		}
	}
}
