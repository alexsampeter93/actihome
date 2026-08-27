package fp.project.actihome.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.FontMetrics;

import org.junit.jupiter.api.Test;

import fp.project.actihome.ui.theme.Typography;

/**
 * Comprueba que {@link Contador} reserva sitio para el número más largo que
 * puede llegar a mostrar.
 *
 * <p>
 * <b>El fallo que evita se vio en una captura, no en una herramienta.</b> La
 * celda del número era un cuadrado del lado del control —un tamaño escrito sin
 * mirar el contenido— y con el filtro de precio del catálogo, que llega a 2000,
 * el número salía cortado dentro de su propia celda. Ninguna de las cuatro
 * herramientas de medida lo detecta: {@code MedirResponsive} busca componentes
 * que caen <em>fuera del área visible</em>, y un texto rebanado dentro de sus
 * propios límites no cae fuera de nada.
 *
 * <p>
 * Es además la regla que más veces se ha tenido que reaprender en este
 * proyecto: <b>ningún tamaño que dependa de un texto puede ser una constante</b>.
 */
class ContadorTest {


	/** La celda del medio: menos, VALOR, mas. */
	private static Component celdaDelNumero(Contador contador) {
		return contador.getComponent(1);
	}

	@Test
	void laCeldaDelNumeroCabeElValorMaximo() {

		Contador precio = new Contador(0, 0, 2000, 25, null);

		Component celda = celdaDelNumero(precio);
		FontMetrics metrica = celda.getFontMetrics(Typography.sansSemiBold(Typography.BODY_SM));

		assertTrue(celda.getPreferredSize().width >= metrica.stringWidth("2000"),
				"el numero mas largo no cabe en su celda: la celda mide " + celda.getPreferredSize().width
						+ " y 2000 ocupa " + metrica.stringWidth("2000"));
	}

	@Test
	void elAnchoNoCambiaAlCambiarElValor() {

		Contador precio = new Contador(0, 0, 2000, 25, null);

		int alEmpezar = celdaDelNumero(precio).getPreferredSize().width;

		precio.setValor(2000);
		int alMaximo = celdaDelNumero(precio).getPreferredSize().width;

		precio.setValor(75);
		int enMedio = celdaDelNumero(precio).getPreferredSize().width;

		// Una fila que se ensancha al pulsar "+" es lo que hace que un formulario
		// "baile". Por eso se mide la cifra mas ancha repetida y no el valor actual.
		assertEquals(alEmpezar, alMaximo);
		assertEquals(alEmpezar, enMedio);
	}

	@Test
	void noBajaDelMinimoNiSubeDelMaximo() {

		Contador huespedes = new Contador(1, 1, 4, 1, null);

		huespedes.setValor(99);
		assertEquals(4, huespedes.getValor());

		huespedes.setValor(-5);
		assertEquals(1, huespedes.getValor());
	}
}
