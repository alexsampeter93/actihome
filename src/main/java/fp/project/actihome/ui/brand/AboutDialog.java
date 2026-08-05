package fp.project.actihome.ui.brand;

import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * El diálogo "Acerca de": el cuarto y último sitio donde firma CocoBrain.
 *
 * <p>
 * Los otros tres son el icono de la aplicación, el splash de arranque y la
 * versalita "By CocoBrain" bajo el wordmark de la cabecera. Con este se cierra
 * la presencia de la marca prevista en el sistema de diseño: cuatro
 * apariciones discretas, ninguna quitándole sitio a ActiHome.
 *
 * <p>
 * <b>Es un diálogo modal y no una pantalla más</b>, y la diferencia importa: no
 * pasa por el {@link fp.project.actihome.ui.nav.Navigator} porque no sustituye a
 * la ventana actual, se pone encima y se quita. Meterlo en el flujo de
 * navegación habría obligado a recordar de dónde venías para poder volver.
 */
public final class AboutDialog {

	private static final String VERSION = "0.1 · rediseño de interfaz";

	/**
	 * Ancho de la columna de texto.
	 *
	 * <p>
	 * Fijo a propósito, y es lo que evita el otro fallo que tenía este diálogo: sin
	 * un ancho declarado, {@code pack()} le da a cada línea el que necesite para no
	 * partirse, y la frase más larga acababa decidiendo el ancho del diálogo
	 * entero. Con la columna fijada, el texto se ajusta dentro y es el alto el que
	 * se adapta.
	 */
	private static final int ANCHO_TEXTO = 460;

	private AboutDialog() {
	}

	/** Abre el diálogo centrado sobre la ventana que lo pide. */
	public static void mostrar(Window padre) {

		JDialog dialogo = new JDialog(padre, "Acerca de ActiHome", Dialog.ModalityType.APPLICATION_MODAL);

		JPanel raiz = new Page(new MigLayout("wrap 1, " + Space.insets(Space.XXL, Space.XXL, Space.XL, Space.XXL),
				"[" + ANCHO_TEXTO + "!]", "[]" + Space.LG + "[]" + Space.MD + "[]" + Space.LG + "[]" + Space.LG + "[]"));

		raiz.add(cabecera(), "growx");
		raiz.add(Hairline.horizontal(), "growx, h 1!");
		raiz.add(new WrappingText("Aplicación de escritorio para gestionar y reservar alojamientos turísticos."),
				"growx");
		raiz.add(datos(), "growx");
		raiz.add(new WrappingText("Olaz, la mascota, cambia con la estación que elijas. Ahora mismo es "
				+ Theme.estacion().nombre().toLowerCase() + "."), "growx");
		raiz.add(Buttons.primary("Cerrar", e -> dialogo.dispose()), "height " + Typography.altoDeBoton() + "!, alignx left");

		dialogo.setContentPane(raiz);

		// pack() en vez de un setSize con números fijos. El tamaño que ocupa este
		// contenido depende de cuánto miden las fuentes, y eso cambia con el escalado
		// del sistema: con números fijos, en un Windows al 125 % el botón "Cerrar"
		// quedaba fuera del diálogo. pack() se lo pregunta al contenido ya construido,
		// que es el único que lo sabe de verdad.
		dialogo.pack();

		dialogo.setResizable(false);
		dialogo.setLocationRelativeTo(padre);
		dialogo.setVisible(true);
	}

	private static JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);
		titulos.add(Labels.capsAccent("By CocoBrain"));
		titulos.add(Labels.title("ActiHome"), "gaptop " + Space.XXS);
		titulos.add(Labels.muted(VERSION), "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	/** Ficha técnica breve: lo que alguien preguntaría al ver la aplicación. */
	private static JPanel datos() {

		JPanel panel = new JPanel(new MigLayout("wrap 2, " + Space.insets(0),
				"[140!]" + Space.MD + "[grow,fill]", ""));
		panel.setOpaque(false);

		fila(panel, "Interfaz", "Java Swing con FlatLaf y MigLayout");
		fila(panel, "Datos", "Spring Data JPA sobre H2 embebida");
		fila(panel, "Tipografía", "Fraunces y Archivo, empaquetadas");
		fila(panel, "Estaciones", cuantasEstaciones());

		return panel;
	}

	/**
	 * El valor va en {@link WrappingText} y no en una etiqueta normal: un
	 * {@code JLabel} no parte el texto, así que la fila más larga se salía por la
	 * derecha del diálogo en lugar de bajar a una segunda línea.
	 */
	private static void fila(JPanel panel, String etiqueta, String valor) {

		panel.add(Labels.caps(etiqueta), "aligny top, gaptop 3");
		panel.add(new WrappingText(valor), "growx");
	}

	private static String cuantasEstaciones() {
		return Season.values().length + " paletas completas, con su propia ilustración de Olaz";
	}
}
