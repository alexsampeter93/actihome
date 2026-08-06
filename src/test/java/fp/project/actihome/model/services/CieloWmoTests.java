package fp.project.actihome.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * La traducción de códigos WMO a categorías que se pueden enseñar (F18).
 *
 * <p>
 * <b>Sin Spring y sin base de datos</b>: es una función pura, así que
 * levantar un contexto entero para probarla solo la haría más lenta. Es el mismo
 * criterio de {@code SeasonTest} y {@code ReminderEvaluatorTests}.
 */
public class CieloWmoTests {

	@Test
	public void testLosCodigosConocidos() {

		assertEquals(CieloWmo.DESPEJADO, CieloWmo.de(0));
		assertEquals(CieloWmo.NUBLADO, CieloWmo.de(3));
		assertEquals(CieloWmo.NIEBLA, CieloWmo.de(45));
		assertEquals(CieloWmo.LLUVIA, CieloWmo.de(61));
		assertEquals(CieloWmo.NIEVE, CieloWmo.de(75));
		assertEquals(CieloWmo.TORMENTA, CieloWmo.de(95));
	}

	/**
	 * Llovizna, lluvia y chubascos caen en la misma categoría.
	 *
	 * <p>
	 * Es una decisión de producto y conviene que quede fijada: para elegir un
	 * alojamiento, lo que importa es que cae agua, no cuánta. La tabla WMO
	 * distingue veintiocho situaciones y enseñarlas todas obligaría a veintiocho
	 * textos traducidos y veintiocho iconos para que el usuario leyera igualmente
	 * "va a llover".
	 */
	@Test
	public void testTodoLoQueMojaEsLluvia() {

		for (int codigo : new int[] { 51, 55, 61, 65, 80, 82 }) {
			assertEquals(CieloWmo.LLUVIA, CieloWmo.de(codigo), "código " + codigo);
		}
	}

	/**
	 * Un código que la tabla no define no se convierte en "despejado".
	 *
	 * <p>
	 * <b>Este es el test que de verdad importa.</b> La tabla WMO tiene huecos —el
	 * 45 y el 48 son niebla, el 46 y el 47 no existen— y el proveedor puede
	 * empezar a usar un código nuevo cualquier día sin avisar. Un
	 * {@code if (codigo < 50)} habría dado por bueno cualquier valor intermedio y
	 * la ficha diría que hace sol sin tener ni idea. Inventarse el tiempo es peor
	 * que no decirlo: {@code DESCONOCIDO} hace que la ficha no pinte nada.
	 */
	@Test
	public void testLoDesconocidoNoSeInventa() {

		for (int codigo : new int[] { -1, 4, 20, 46, 70, 90, 100 }) {
			assertEquals(CieloWmo.DESCONOCIDO, CieloWmo.de(codigo), "código " + codigo);
		}
	}
}
