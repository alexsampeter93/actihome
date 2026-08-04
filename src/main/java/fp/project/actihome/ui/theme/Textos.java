package fp.project.actihome.ui.theme;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import fp.project.actihome.model.entities.Amenity;

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

	/**
	 * Traduce un tipo de alojamiento <b>para mostrarlo</b>, sin tocar el valor
	 * guardado.
	 *
	 * <p>
	 * "Casa", "Villa"... no son solo texto de pantalla: son el valor real de
	 * {@code Housing.type}, guardado en la base de datos y comparado tal cual al
	 * filtrar (`CatalogFilters.aplicar`). Traducir el chip sin más desincronizaría
	 * lo que se ve de lo que se guarda y de lo que se compara. Esta traducción es
	 * solo de <b>presentación</b>: quien la llama sigue guardando y comparando el
	 * valor original en español; solo cambia lo que el usuario lee.
	 *
	 * <p>
	 * Es un {@code switch} explícito y no una clave derivada del texto
	 * ({@code "tipo." + valor.toLowerCase()}) a propósito: es una lista cerrada de
	 * cuatro valores (ver {@code HousingForm.TIPOS}), y un valor que no encaje en
	 * ninguna clave se devuelve tal cual en vez de lanzar una excepción de
	 * `ResourceBundle` en mitad de un catálogo.
	 */
	public static String tipoDeAlojamiento(String valorAlmacenado) {

		switch (valorAlmacenado) {

			case "Todos":
				return t("tipo.todos");
			case "Casa":
				return t("tipo.casa");
			case "Apartamento":
				return t("tipo.apartamento");
			case "Villa":
				return t("tipo.villa");
			case "Cabaña":
				return t("tipo.cabana");
			default:
				return valorAlmacenado;
		}
	}

	/**
	 * Traduce una comodidad para mostrarla. Mismo motivo que
	 * {@link #tipoDeAlojamiento(String)}: {@link Amenity#etiqueta()} vive en
	 * {@code model/entities} y no puede depender de este paquete, así que la
	 * traducción de presentación vive aquí, no allí.
	 */
	public static String etiquetaDe(Amenity amenity) {

		switch (amenity) {

			case POOL:
				return t("amenity.pool");
			case WIFI:
				return t("amenity.wifi");
			case TV:
				return t("amenity.tv");
			case PARKING:
				return t("amenity.parking");
			case AIR_CONDITIONING:
				return t("amenity.airConditioning");
			case BREAKFAST:
				return t("amenity.breakfast");
			case PETS:
				return t("amenity.pets");
			default:
				return amenity.etiqueta();
		}
	}
}
