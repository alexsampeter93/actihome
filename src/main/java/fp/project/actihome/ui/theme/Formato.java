package fp.project.actihome.ui.theme;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Cómo se escriben los números en la interfaz.
 *
 * <p>
 * Parece un detalle menor y no lo es: el precio aparece en la ficha, en el
 * detalle, en el resumen de la reserva y en "mis reservas". Si cada pantalla lo
 * formatea a su manera, el mismo alojamiento cuesta {@code 75.0} en un sitio,
 * {@code 75,00 €} en otro y {@code 75.00} en un tercero. Eso no se percibe como
 * cuatro pantallas con criterios distintos: se percibe como una aplicación poco
 * fiable, que es mucho peor.
 *
 * <p>
 * <b>Locale español, siempre y explícito.</b> Se pasa {@code new Locale("es",
 * "ES")} en lugar de dejar el idioma del sistema. Sin fijarlo, la misma
 * aplicación escribiría <i>75,00</i> en un Windows español y <i>75.00</i> en uno
 * inglés — con la coma y el punto intercambiados en el precio, que es
 * exactamente el sitio donde una confusión cuesta dinero.
 */
public final class Formato {

	private static final Locale ES = new Locale("es", "ES");

	private Formato() {
	}

	/** Precio con dos decimales y símbolo de euro: {@code 75,00 €}. */
	public static String precio(BigDecimal valor) {

		if (valor == null) {
			return "—";
		}

		NumberFormat formato = NumberFormat.getNumberInstance(ES);
		formato.setMinimumFractionDigits(2);
		formato.setMaximumFractionDigits(2);

		return formato.format(valor) + " €";
	}

	/** Precio sin decimales cuando son cero: {@code 75 €}, {@code 47,50 €}. */
	public static String precioCorto(BigDecimal valor) {

		if (valor == null) {
			return "—";
		}

		NumberFormat formato = NumberFormat.getNumberInstance(ES);
		formato.setMinimumFractionDigits(0);
		formato.setMaximumFractionDigits(2);

		return formato.format(valor) + " €";
	}

	/**
	 * Nota con un decimal: {@code 4,2}. Devuelve un guion cuando no hay nota.
	 *
	 * <p>
	 * El guion es deliberado y no un {@code "0,0"}. Un alojamiento sin reseñas no
	 * tiene un cero: no tiene nota. Escribir {@code 0,0} lo pone el último en
	 * cualquier ordenación y lo hace parecer pésimo cuando en realidad es nuevo.
	 */
	public static String nota(Double valor) {

		if (valor == null) {
			return "—";
		}

		NumberFormat formato = NumberFormat.getNumberInstance(ES);
		formato.setMinimumFractionDigits(1);
		formato.setMaximumFractionDigits(1);

		return formato.format(valor);
	}

	/** Concordancia de singular y plural: {@code 1 reseña} / {@code 14 reseñas}. */
	public static String plural(int cantidad, String singular, String plural) {
		return cantidad + " " + (cantidad == 1 ? singular : plural);
	}

	/**
	 * Un rango de fechas corto, para el resumen del buscador y del filtro de
	 * catálogo: {@code 12 jul – 17 jul}.
	 *
	 * <p>
	 * En el idioma activo, como el resto de fechas de la aplicación (ver
	 * {@code ReviewDetailsFrame.formatoFecha()} para el mismo patrón ES/EN). El
	 * año no aparece: un buscador de estancias solo ofrece fechas futuras
	 * cercanas, y repetirlo en los dos extremos del rango no añade nada que la
	 * persona no sepa ya.
	 */
	public static String rangoDeFechas(LocalDate entrada, LocalDate salida) {

		if (entrada == null || salida == null) {
			return "—";
		}

		DateTimeFormatter formato = Textos.idioma().getLanguage().equals("en")
				? DateTimeFormatter.ofPattern("MMM d", Textos.idioma())
				: DateTimeFormatter.ofPattern("d MMM", Textos.idioma());

		return entrada.format(formato) + " – " + salida.format(formato);
	}
}
