package fp.project.actihome.ui.dev;

import java.awt.Font;

import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Comprueba qué símbolos saben dibujar las fuentes empaquetadas.
 *
 * <p>
 * <b>Por qué existe esta herramienta.</b> Cuando a Swing le falta un glifo no
 * avisa: dibuja un rectángulo vacío que, junto a un texto correcto, se lee como
 * un icono raro y no como un fallo. Al proyecto ya le pasó dos veces con
 * Spectral y Manrope —el {@code ⇄} de "Intercambiar" y el {@code ✓} de
 * "Check-in realizado", publicados rotos en la Fase 4 y corregidos en la 6—, y
 * la regla que salió de ahí es que <b>antes de meter un símbolo en una etiqueta
 * se comprueba</b>.
 *
 * <p>
 * Cambiar de familia tipográfica (Fase 8.2) invalida esa comprobación entera:
 * el repertorio de Fraunces y Archivo no tiene por qué parecerse al de Spectral
 * y Manrope. Esto es lo que la rehace, en vez de dar por bueno lo que se
 * verificó para otras fuentes.
 */
public final class MedirGlifos {

	/** Los símbolos que la aplicación usa o ha usado en algún momento. */
	private static final String[] SIMBOLOS = { "→", "←", "·", "›", "—", "–", "€", "↔", "⇄", "⇆", "★", "✓", "⟷", "×",
			"✳", "•", "●", "±", "«", "»", "…", "¿", "¡", "ñ", "á", "ü" };

	private MedirGlifos() {
	}

	public static void main(String[] args) {

		ActiHomeTheme.install();

		Font sans = Typography.sans(15f);
		Font serifTexto = Typography.serif(15f);
		Font serifDisplay = Typography.serif(46f);

		System.out.println();
		System.out.printf("%-8s %-14s %-14s %-14s%n", "simbolo", "Archivo", "Fraunces 9pt", "Fraunces 144pt");
		System.out.println("--------------------------------------------------------");

		boolean hayFaltas = false;

		for (String simbolo : SIMBOLOS) {

			int punto = simbolo.codePointAt(0);

			boolean enSans = sans.canDisplay(punto);
			boolean enSerifTexto = serifTexto.canDisplay(punto);
			boolean enSerifDisplay = serifDisplay.canDisplay(punto);

			System.out.printf("%-8s %-14s %-14s %-14s%n", simbolo, marca(enSans), marca(enSerifTexto),
					marca(enSerifDisplay));

			hayFaltas = hayFaltas || !enSans || !enSerifTexto || !enSerifDisplay;
		}

		System.out.println();
		System.out.println(hayFaltas
				? "Hay huecos: cualquier simbolo con 'NO' debe dibujarse a mano, no escribirse en una etiqueta."
				: "Las tres fuentes cubren todos los simbolos comprobados.");

		System.exit(0);
	}

	private static String marca(boolean puede) {
		return puede ? "si" : "NO";
	}
}
