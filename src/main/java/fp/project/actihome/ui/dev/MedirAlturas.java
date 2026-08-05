package fp.project.actihome.ui.dev;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JLabel;

import fp.project.actihome.ui.theme.ActiHomeTheme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Comprueba los altos de control derivados de la fuente y, sobre todo,
 * <b>cuánto cambia de anchura el texto al cambiar de tipografía</b> (Fase 8.2).
 *
 * <p>
 * Lo segundo es lo que {@code MedirResponsive} no puede ver. Aquella herramienta
 * busca componentes que caen fuera del área visible; un {@code JLabel} cuyo
 * texto no cabe <em>dentro de sus propios límites</em> no cae fuera de nada, así
 * que da "ok" mientras el usuario lee "Hueco de fotc". La primera captura con
 * Archivo lo enseñó a simple vista en la guía de estilo, y la pregunta que hay
 * que contestar con un número, no a ojo, es cuánto más ancha es.
 */
public final class MedirAlturas {

	private static final String[] FRASES = { "Hueco de foto", "Imagen y mascota", "NOMBRE DE USUARIO",
			"Un caserón de piedra a media hora de la sierra, con chimenea y vistas al valle.", "Reservar",
			"3 HABITACIONES", "Cambiar contraseña" };

	private MedirAlturas() {
	}

	public static void main(String[] args) {

		ActiHomeTheme.install();

		System.out.println();
		System.out.println("=== Altos de control ===");
		System.out.println("linea BODY (15)          = " + Typography.altoDeLinea(Typography.BODY));
		System.out.println("linea BODY_SM (14)       = " + Typography.altoDeLinea(Typography.BODY_SM));
		System.out.println("altoDeControl()          = " + Typography.altoDeControl() + "   (antes 38)");
		System.out.println("altoDeBoton()            = " + Typography.altoDeBoton() + "   (antes 44)");
		System.out.println("altoDeControlCompacto()  = " + Typography.altoDeControlCompacto() + "   (antes 32)");

		Font manrope = cargar("Manrope-Regular.ttf");

		if (manrope == null) {
			System.out.println();
			System.out.println("Manrope ya no esta empaquetada: la comparacion de anchuras no se puede hacer.");
			System.exit(0);
		}

		manrope = manrope.deriveFont(Typography.BODY);

		Font archivo = Typography.sans(Typography.BODY);
		JLabel regla = new JLabel();

		System.out.println();
		System.out.println("=== Anchura del texto: Manrope -> Archivo, al mismo cuerpo (15) ===");
		System.out.printf("%-8s %-8s %-8s  %s%n", "Manrope", "Archivo", "cambio", "frase");
		System.out.println("---------------------------------------------------------------");

		double peorCaso = 0;

		for (String frase : FRASES) {

			int antes = regla.getFontMetrics(manrope).stringWidth(frase);
			int ahora = regla.getFontMetrics(archivo).stringWidth(frase);
			double cambio = (ahora - antes) * 100.0 / antes;

			peorCaso = Math.max(peorCaso, cambio);

			System.out.printf("%-8d %-8d %+7.1f%%  %s%n", antes, ahora, cambio, frase);
		}

		System.out.println();
		System.out.printf("Peor caso: %+.1f %%%n", peorCaso);

		System.exit(0);
	}

	private static Font cargar(String fichero) {

		try (InputStream in = MedirAlturas.class.getResourceAsStream("/fonts/" + fichero)) {

			return in == null ? null : Font.createFont(Font.TRUETYPE_FONT, in);

		} catch (FontFormatException | IOException e) {
			return null;
		}
	}
}
