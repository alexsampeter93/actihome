package fp.project.actihome.ui.brand;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;

/**
 * El diálogo "Acerca de": el cuarto y último sitio donde firma CocoBrain.
 *
 * <p>
 * Los otros tres son el icono de la aplicación, el splash de arranque y la
 * versalita "por CocoBrain" del colofón del catálogo. Con este se cierra la
 * presencia de la marca prevista en el sistema de diseño: cuatro apariciones
 * discretas, ninguna quitándole sitio a ActiHome.
 *
 * <p>
 * <b>Es un diálogo modal y no una pantalla más</b>, y la diferencia importa: no
 * pasa por el {@link fp.project.actihome.ui.nav.Navigator} porque no sustituye a
 * la ventana actual, se pone encima y se quita. Meterlo en el flujo de
 * navegación habría obligado a recordar de dónde venías para poder volver.
 */
public final class AboutDialog {

	private static final String VERSION = "0.1 · rediseño de interfaz";

	private AboutDialog() {
	}

	/** Abre el diálogo centrado sobre la ventana que lo pide. */
	public static void mostrar(Window padre) {

		JDialog dialogo = new JDialog(padre, "Acerca de ActiHome", Dialog.ModalityType.APPLICATION_MODAL);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(Space.XXL, Space.XXL, Space.XL, Space.XXL),
				"[grow,fill]", "[]" + Space.LG + "[]" + Space.MD + "[]" + Space.LG + "[]" + Space.LG + "[]push[]"));

		raiz.add(cabecera(), "growx");
		raiz.add(Hairline.horizontal(), "growx, h 1!");
		raiz.add(Labels.body("Aplicación de escritorio para gestionar y reservar alojamientos turísticos."),
				"growx");
		raiz.add(datos(), "growx");
		raiz.add(Labels.muted("Olaz, la mascota, cambia con la estación que elijas. Ahora mismo es "
				+ Theme.estacion().nombre().toLowerCase() + "."), "growx");
		raiz.add(Buttons.primary("Cerrar", e -> dialogo.dispose()), "height 40!, alignx left");

		dialogo.setContentPane(raiz);
		dialogo.setSize(560, 430);
		dialogo.setMinimumSize(new Dimension(480, 400));
		dialogo.setResizable(false);
		dialogo.setLocationRelativeTo(padre);
		dialogo.setVisible(true);
	}

	private static JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		titulos.add(Labels.capsAccent("por CocoBrain"));
		titulos.add(Labels.title("ActiHome"), "gaptop " + Space.XXS);
		titulos.add(Labels.muted(VERSION), "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 64!, h 64!");

		return panel;
	}

	/** Ficha técnica breve: lo que alguien preguntaría al ver la aplicación. */
	private static JPanel datos() {

		JPanel panel = new JPanel(new MigLayout("wrap 2, " + Space.insets(0),
				"[140!]" + Space.MD + "[grow,fill]", ""));
		panel.setOpaque(false);

		fila(panel, "Interfaz", "Java Swing con FlatLaf y MigLayout");
		fila(panel, "Datos", "Spring Data JPA sobre H2 embebida");
		fila(panel, "Tipografía", "Spectral y Manrope, empaquetadas");
		fila(panel, "Estaciones", cuantasEstaciones());

		return panel;
	}

	private static void fila(JPanel panel, String etiqueta, String valor) {

		panel.add(Labels.caps(etiqueta), "aligny top");
		panel.add(Labels.body(valor));
	}

	private static String cuantasEstaciones() {
		return Season.values().length + " paletas completas, con su propia ilustración de Olaz";
	}
}
