package fp.project.actihome.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * Comprueba que {@link Foco#activable} le da a un control pintado a mano lo que
 * un botón de Swing trae de serie: ser alcanzable con el tabulador y activarse
 * con Espacio o Intro.
 *
 * <p>
 * <b>Por qué merece un test.</b> Cuatro controles del proyecto —los enlaces de
 * la cabecera, las pestañas de estación, el orden del catálogo y el conmutador
 * de vista— dibujan su propio contenido sobre un {@code JPanel} y solo escuchan
 * al ratón. Sin esta pieza, con solo teclado <b>no había forma de cambiar de
 * estación ni de navegar por la cabecera</b>, que es la función más repetida de
 * toda la aplicación. Es un fallo que no da error, no se ve en una captura y no
 * lo detecta ninguna de las herramientas de medida: solo aparece si alguien
 * suelta el ratón e intenta usarla.
 */
class FocoTest {

	@Test
	void elControlPasaAAlcanzableConElTabulador() {

		JComponent control = new JPanel();
		control.setFocusable(false);

		Foco.activable(control, () -> {
			// La accion no importa para este caso.
		});

		assertTrue(control.isFocusable());
	}

	@Test
	void espacioEIntroDisparanLaMismaAccionQueElClic() {

		JComponent control = new JPanel();
		int[] veces = { 0 };

		Foco.activable(control, () -> veces[0]++);

		Action accion = control.getActionMap().get("activar");
		assertNotNull(accion, "no hay accion registrada bajo activar");

		assertEquals("activar", control.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("SPACE")));
		assertEquals("activar", control.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER")));

		accion.actionPerformed(new ActionEvent(control, ActionEvent.ACTION_PERFORMED, "activar"));
		assertEquals(1, veces[0]);
	}
}
