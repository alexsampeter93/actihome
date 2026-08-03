package fp.project.actihome.ui.dev;

import java.awt.Color;

import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Theme;

/**
 * Comprueba el contraste de cada par texto/fondo del sistema de diseño, en las
 * cuatro estaciones, contra los umbrales de WCAG 2.1 AA.
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.MedirContraste"
 * </pre>
 *
 * <p>
 * <b>Por qué hace falta medirlo y no basta con mirar.</b> El acento partido de
 * verano (ADR-008) nació exactamente así: alguien midió que el acento único
 * daba 2,83:1 como texto, no lo vio a ojo. Un color puede parecer legible en un
 * monitor calibrado y fallar en otro, y las cuatro estaciones multiplican por
 * cuatro las combinaciones a comprobar cada vez que se toca un token — a mano
 * es fácil que se cuele una.
 *
 * <p>
 * <b>Los dos umbrales de WCAG 2.1 AA</b>: 4,5:1 para texto normal, 3:1 para
 * texto grande (≥24px, o ≥18,7px en negrita) y para componentes de interfaz que
 * no son texto (bordes de campo, iconos con significado). Aquí se comprueban
 * los dos umbrales para cada par y se deja anotado cuál aplica según dónde se
 * usa ese color en la aplicación real.
 */
public class MedirContraste {

	private static final double UMBRAL_NORMAL = 4.5;
	private static final double UMBRAL_GRANDE = 3.0;

	public static void main(String[] args) {

		int fallos = 0;

		for (Season estacion : Season.values()) {

			System.out.println("=== " + estacion.nombre().toUpperCase() + " ===");

			fallos += fila("txt sobre bg (cuerpo, 15px)", estacion.txt(), estacion.bg(), UMBRAL_NORMAL);
			fallos += fila("txt sobre SURFACE (cuerpo en fichas)", estacion.txt(), Theme.SURFACE, UMBRAL_NORMAL);
			fallos += fila("mut sobre bg (texto secundario, 14px)", estacion.mut(), estacion.bg(), UMBRAL_NORMAL);
			fallos += fila("mut sobre SURFACE (texto secundario en fichas)", estacion.mut(), Theme.SURFACE,
					UMBRAL_NORMAL);
			fallos += fila("accText sobre bg (versalita 11px, enlaces)", estacion.accText(), estacion.bg(),
					UMBRAL_NORMAL);
			fallos += fila("accText sobre bg, como precio (≥22px, 3:1 basta)", estacion.accText(), estacion.bg(),
					UMBRAL_GRANDE);
			fallos += fila("DANGER sobre bg (mensaje de error, 14px)", Theme.DANGER, estacion.bg(), UMBRAL_NORMAL);
			fallos += fila("DANGER sobre SURFACE (mensaje de error en formulario)", Theme.DANGER, Theme.SURFACE,
					UMBRAL_NORMAL);

			Color mutOscuro = new Color(estacion.bg().getRed(), estacion.bg().getGreen(), estacion.bg().getBlue(),
					155);
			fallos += filaConAlfa("mutSobreOscuro sobre hdr (nav inactiva, 12px)", mutOscuro, estacion.hdr(),
					UMBRAL_NORMAL);
			fallos += fila("bg (blanco de página) sobre hdr, como texto activo 12px", estacion.bg(), estacion.hdr(),
					UMBRAL_NORMAL);

			Color sobreAcento = estacion == Season.VERANO ? estacion.txt() : Color.WHITE;
			fallos += fila("onAccent() sobre acc (etiqueta de botón, 15px)", sobreAcento, estacion.acc(),
					UMBRAL_NORMAL);

			// El subrayado de pestaña/nav activa en la cabecera usa el mismo color que su
			// propio texto (bg en Destino, blanco en Pestana), no el acento — ver la nota
			// en HeaderPanel.Destino.actualizarColores() y SeasonSelector.Pestana. Los dos
			// ya están cubiertos por "bg sobre hdr" más arriba; aquí se deja constancia
			// explícita del segundo caso.
			fallos += fila("blanco como barra sobre hdr (subrayado de SeasonSelector activa)", Color.WHITE,
					estacion.hdr(), UMBRAL_GRANDE);
			// OptionLinks ("Ordenar") usa accText para el texto y el mismo accText para el
			// subrayado, no el acento — ver OptionLinks.Opcion.actualizarColores().
			fallos += fila("accText como barra sobre bg (subrayado de OptionLinks, 'Ordenar')", estacion.accText(),
					estacion.bg(), UMBRAL_GRANDE);
			fallos += fila("FIELD_BORDER sobre SURFACE, como componente UI", opacoSobre(Theme.FIELD_BORDER,
					Theme.SURFACE), Theme.SURFACE, UMBRAL_GRANDE);

			System.out.println();
		}

		System.out.println(fallos == 0 ? "SIN FALLOS DE CONTRASTE en ninguna estacion."
				: "TOTAL de combinaciones por debajo de su umbral: " + fallos);

		System.exit(fallos == 0 ? 0 : 1);
	}

	private static int fila(String etiqueta, Color texto, Color fondo, double umbral) {
		return filaConAlfa(etiqueta, texto, fondo, umbral);
	}

	/**
	 * Admite colores con canal alfa: la ratio se calcula sobre el color
	 * <b>resultante</b> de mezclarlo con el fondo, que es lo que el ojo ve de
	 * verdad, no sobre los componentes RGB del color semitransparente en crudo.
	 */
	private static int filaConAlfa(String etiqueta, Color texto, Color fondo, double umbral) {

		Color textoOpaco = opacoSobre(texto, fondo);
		double ratio = contraste(textoOpaco, fondo);
		boolean pasa = ratio >= umbral;

		System.out.printf("  %-58s %5.2f:1  (min %.1f:1)  %s%n", etiqueta, ratio, umbral, pasa ? "ok" : "FALLA");

		return pasa ? 0 : 1;
	}

	/** Aplana un color con transparencia sobre un fondo, componente a componente. */
	private static Color opacoSobre(Color conAlfa, Color fondo) {

		double a = conAlfa.getAlpha() / 255.0;

		int r = (int) Math.round(conAlfa.getRed() * a + fondo.getRed() * (1 - a));
		int g = (int) Math.round(conAlfa.getGreen() * a + fondo.getGreen() * (1 - a));
		int b = (int) Math.round(conAlfa.getBlue() * a + fondo.getBlue() * (1 - a));

		return new Color(r, g, b);
	}

	/** Ratio de contraste de WCAG 2.1: (L1 + 0.05) / (L2 + 0.05), con L1 el más claro. */
	private static double contraste(Color a, Color b) {

		double la = luminanciaRelativa(a);
		double lb = luminanciaRelativa(b);
		double claro = Math.max(la, lb);
		double oscuro = Math.min(la, lb);

		return (claro + 0.05) / (oscuro + 0.05);
	}

	private static double luminanciaRelativa(Color c) {

		double r = canal(c.getRed());
		double g = canal(c.getGreen());
		double b = canal(c.getBlue());

		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private static double canal(int valor255) {

		double v = valor255 / 255.0;
		return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
	}
}
