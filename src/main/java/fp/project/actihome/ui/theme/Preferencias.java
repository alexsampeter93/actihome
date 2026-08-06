package fp.project.actihome.ui.theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Las preferencias visuales de la última sesión, guardadas en el disco.
 *
 * <p>
 * <b>El problema que resuelve.</b> Estación, idioma y partículas se guardan por
 * cuenta en la base de datos (Fase 7.6) y se aplican en
 * {@code LoginFrame.aplicarPreferencias}, es decir, <em>después</em> de saber
 * quién eres. Pero la pantalla de inicio de sesión se ve <em>antes</em> de eso,
 * cuando todavía no hay ningún usuario del que leer nada, así que se pintaba
 * siempre con {@link Season#actual()} — la estación del calendario. En agosto,
 * verano; y quien tuviera invierno guardado en Ajustes veía verano en el login
 * cada vez que abría la aplicación, sin manera de entender por qué.
 *
 * <p>
 * Lo mismo le pasaba al idioma y a las partículas, aunque nadie lo hubiera
 * reportado: la aplicación arrancaba siempre en español con las partículas
 * encendidas, dijera lo que dijera la cuenta.
 *
 * <p>
 * <b>Por qué un fichero local y no la base de datos.</b> No es que la base de
 * datos no sirva: es que la pregunta llega demasiado pronto. Antes del login no
 * hay usuario, y no se puede elegir la preferencia de nadie sin saber de quién.
 * Este fichero no compite con la base de datos ni la duplica como fuente de
 * verdad — es un <b>espejo de lo último que se aplicó</b>, para que la primera
 * pantalla no tenga que pintar a ciegas. En cuanto alguien inicia sesión, la
 * base de datos vuelve a mandar y sobrescribe lo que hubiera aquí.
 *
 * <p>
 * Vive junto a la base de datos, en {@code ~/.actihome/}, por coherencia con lo
 * que el proyecto ya establece: borrar esa carpeta deja la aplicación como
 * recién instalada, y eso debe seguir siendo cierto también para esto.
 *
 * <p>
 * <b>Nunca lanza.</b> Un fallo leyendo o escribiendo estas tres líneas no puede
 * impedir que la aplicación arranque: si el fichero no existe, no se puede leer
 * o está corrupto, se usan los valores de siempre y la aplicación se comporta
 * exactamente como antes de que esta clase existiera.
 */
public final class Preferencias {

	private static final Logger log = LoggerFactory.getLogger(Preferencias.class);

	private static final Path CARPETA = Paths.get(System.getProperty("user.home"), ".actihome");
	private static final Path FICHERO = CARPETA.resolve("preferencias.properties");

	private static final String CLAVE_ESTACION = "estacion";
	private static final String CLAVE_IDIOMA = "idioma";
	private static final String CLAVE_PARTICULAS = "particulas";

	private Preferencias() {
	}

	/**
	 * Aplica al arranque lo que quedó guardado la última vez.
	 *
	 * <p>
	 * Se llama desde {@link ActiHomeTheme#install()}, antes de que exista ninguna
	 * ventana: la estación tiene que estar puesta cuando FlatLaf calcule su paleta,
	 * o el primer pintado saldría con los colores equivocados y habría que
	 * repintarlo entero un instante después.
	 *
	 * <p>
	 * Lo que no esté guardado no se toca. Una instalación nueva se comporta como
	 * siempre: estación del calendario, español y partículas encendidas.
	 */
	static void restaurar() {

		Properties guardadas = leer();

		String estacion = guardadas.getProperty(CLAVE_ESTACION);

		if (estacion != null) {
			try {
				Theme.cambiarA(Season.valueOf(estacion));
			} catch (IllegalArgumentException ex) {
				// Nombre de estación que ya no existe (fichero de una versión anterior):
				// se ignora y se queda la del calendario.
			}
		}

		String idioma = guardadas.getProperty(CLAVE_IDIOMA);

		if (idioma != null) {
			Textos.cambiarA("en".equals(idioma) ? Locale.ENGLISH : new Locale("es"));
		}

		String particulas = guardadas.getProperty(CLAVE_PARTICULAS);

		if (particulas != null) {
			Particulas.activar(Boolean.parseBoolean(particulas));
		}
	}

	/**
	 * Guarda las preferencias que se acaban de aplicar, para que la próxima
	 * pantalla de inicio de sesión las respete.
	 *
	 * <p>
	 * Se llama en los dos únicos sitios donde una <em>cuenta</em> impone su
	 * preferencia: al iniciar sesión y al guardar en Ajustes. <b>No</b> se llama al
	 * cambiar de estación desde las pestañas de la cabecera, y es deliberado: eso
	 * es curiosear, no configurar. Si probar las cuatro estaciones cambiara la
	 * preferencia guardada, el selector dejaría de poder tocarse sin consecuencias.
	 */
	public static void recordar() {

		Properties valores = new Properties();
		valores.setProperty(CLAVE_ESTACION, Theme.estacion().name());
		valores.setProperty(CLAVE_IDIOMA, Locale.ENGLISH.getLanguage().equals(Textos.idioma().getLanguage()) ? "en" : "es");
		valores.setProperty(CLAVE_PARTICULAS, Boolean.toString(Particulas.activas()));

		try {
			Files.createDirectories(CARPETA);

			try (OutputStream salida = Files.newOutputStream(FICHERO)) {
				valores.store(salida, "Preferencias visuales de la ultima sesion de ActiHome");
			}

		} catch (IOException ex) {
			// Disco lleno, carpeta sin permisos, perfil de red caído... Nada de esto
			// justifica interrumpir al usuario: lo único que se pierde es que la próxima
			// pantalla de login salga con la estación del calendario.
			log.warn("No se pudieron guardar las preferencias", ex);
		}
	}

	private static Properties leer() {

		Properties valores = new Properties();

		if (!Files.isReadable(FICHERO)) {
			return valores;
		}

		try (InputStream entrada = Files.newInputStream(FICHERO)) {
			valores.load(entrada);

		} catch (IOException ex) {
			log.warn("No se pudieron leer las preferencias", ex);
		}

		return valores;
	}
}
