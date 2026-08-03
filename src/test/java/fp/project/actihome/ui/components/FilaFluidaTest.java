package fp.project.actihome.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/**
 * Comprueba que {@link FilaFluida} reparte en líneas y, sobre todo, que
 * <b>declara bien lo que necesita</b>.
 *
 * <p>
 * Es un test de JUnit puro: sin Spring, sin base de datos y sin abrir ninguna
 * ventana. Se apoya en componentes de tamaño fijado a mano, así que tampoco
 * depende de qué fuentes haya instaladas ni del escalado del sistema — que es
 * justo lo que hacía falta poder probar sin depender de la máquina.
 *
 * <p>
 * Los dos casos que se prueban no son teóricos: son los dos errores que costó
 * escribir esta clase.
 */
class FilaFluidaTest {

	/** Un componente cuyo tamaño no depende de la fuente ni del sistema. */
	private static JComponent pieza(int ancho, int alto) {

		JPanel pieza = new JPanel();
		pieza.setPreferredSize(new Dimension(ancho, alto));

		return pieza;
	}

	private static FilaFluida conCincoPiezas() {

		FilaFluida fila = new FilaFluida(10, 10);

		for (int i = 0; i < 5; i++) {
			fila.add(pieza(100, 30));
		}

		return fila;
	}

	/**
	 * El ancho mínimo es el elemento más ancho, no la suma de todos.
	 *
	 * <p>
	 * <b>Este es el arreglo de fondo del comportamiento adaptable.</b> Una fila
	 * normal exige la suma —aquí 5×100 más los huecos, 540— y esa exigencia sube
	 * hasta la ventana entera: en un portátil con el escalado de Windows al 150 %,
	 * donde la aplicación dispone de 1280 puntos lógicos y no de 1920, los mínimos
	 * sumados de la cabecera y los filtros no cabían y el contenido se dibujaba
	 * fuera de la ventana.
	 */
	@Test
	void elMinimoEsElElementoMasAnchoYNoLaSuma() {

		FilaFluida fila = conCincoPiezas();

		assertEquals(100, fila.getMinimumSize().width,
				"el minimo debe ser el elemento mas ancho, no la suma de los cinco");
	}

	/**
	 * Con sitio de sobra, una sola línea; sin él, las que hagan falta.
	 *
	 * <p>
	 * Se mide a través del alto porque es lo que el contenedor de arriba va a
	 * reservar, que es lo que de verdad importa.
	 */
	@Test
	void seDoblaEnVariasLineasCuandoNoCabe() {

		FilaFluida fila = conCincoPiezas();

		fila.setSize(540, 30);
		fila.doLayout();
		assertEquals(30, fila.getPreferredSize().height, "con 540 de ancho las cinco piezas caben en una linea");

		fila.setSize(250, 30);
		fila.doLayout();
		assertTrue(fila.getPreferredSize().height > 30, "con 250 de ancho tiene que doblar en mas de una linea");
	}

	/**
	 * El alto declarado corresponde al ancho real, no al ancho mínimo.
	 *
	 * <p>
	 * <b>Fue el primer fallo de esta clase y se vio a la primera captura.</b> El
	 * mínimo devolvía el alto de estar plegada a una columna —cinco piezas, cinco
	 * líneas—, y MigLayout reserva el alto mínimo de la fila <em>siempre</em>,
	 * también cuando hay sitio de sobra. La banda de filtros del catálogo pasó de
	 * 114 a 280 puntos y el rótulo "Tipo" quedó flotando ochenta puntos por debajo
	 * de sus propios chips.
	 */
	@Test
	void elAltoMinimoNoSuponeElPeorCasoDeAncho() {

		FilaFluida fila = conCincoPiezas();

		fila.setSize(540, 30);
		fila.doLayout();

		assertEquals(30, fila.getMinimumSize().height,
				"con las cinco piezas en una linea, el alto minimo tambien es de una linea");
	}
}
