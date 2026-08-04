package fp.project.actihome.ui.theme;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * El idioma activo de la interfaz (Fase 7.6).
 *
 * <p>
 * Hermana de {@link Theme}: estado estático porque hay una sola aplicación,
 * una sola ventana visible a la vez y un solo idioma activo — el mismo
 * argumento que ya justifica que {@code Theme} no sea un bean de Spring.
 *
 * <p>
 * <b>A diferencia de {@code Theme}, no hace falta un patrón observador.</b>
 * {@code Theme.acc()} se llama en cada pintado —25 veces por segundo, por las
 * partículas de fondo—, así que un cambio de estación tiene que avisar a quien
 * esté escuchando o la pantalla se quedaría a medio repintar. El texto de un
 * {@code JLabel} no funciona así: se fija una vez con {@code setText(...)} y
 * no se vuelve a mirar hasta que algo lo pida expresamente. El proyecto ya
 * tiene el gancho que hace falta para refrescarlo sin inventar nada nuevo:
 * todo frame recarga su contenido en {@code setVisible(true)} antes de
 * mostrarse (regla ya establecida, ver CLAUDE.md §4), y las filas de una lista
 * ({@code HousingCard}, {@code ReviewRow}...) se reconstruyen enteras en cada
 * refresco. Un observador aquí no protegería nada que ese ciclo no proteja ya,
 * y sí sería arriesgado: esas filas no son singleton, así que un oyente que no
 * se diera nunca de baja iría dejando una fuga en cada recarga del catálogo.
 */
public final class Textos {

	private static Locale idioma = new Locale("es");
	private static ResourceBundle bundle = ResourceBundle.getBundle("i18n.textos", idioma);

	private Textos() {
	}

	/** El idioma activo ahora mismo. */
	public static Locale idioma() {
		return idioma;
	}

	/** Cambia el idioma activo. No avisa a nadie: ver la nota de clase. */
	public static void cambiarA(Locale nuevo) {

		if (nuevo == null || nuevo.equals(idioma)) {
			return;
		}

		idioma = nuevo;
		bundle = ResourceBundle.getBundle("i18n.textos", idioma);
	}

	/** El texto asociado a una clave, en el idioma activo. */
	public static String t(String clave) {
		return bundle.getString(clave);
	}

	/** Igual que {@link #t(String)}, con sustitución de parámetros al estilo {@link MessageFormat}. */
	public static String t(String clave, Object... args) {
		return MessageFormat.format(bundle.getString(clave), args);
	}
}
