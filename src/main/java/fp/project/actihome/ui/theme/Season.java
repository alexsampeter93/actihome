package fp.project.actihome.ui.theme;

import java.awt.Color;
import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Las cuatro paletas estacionales de ActiHome.
 *
 * <p>
 * Es el núcleo del sistema de diseño: toda la interfaz se pinta a partir de la
 * estación activa, y cambiar de estación reambienta la aplicación entera. Cada
 * estación aporta siete colores con un papel fijo — lo que en el mundo del
 * diseño se llama <b>design tokens</b>.
 *
 * <p>
 * La gracia de los tokens es que una pantalla nunca escribe un color: pide un
 * <em>papel</em>. En vez de "pinta esto de #C0870F" dice "pinta esto con el
 * color de acento". Así, las cuatro paletas y las diecisiete pantallas son
 * independientes entre sí: se pueden añadir estaciones sin tocar pantallas, y
 * pantallas sin tocar estaciones.
 *
 * <p>
 * Los valores están tomados literalmente del handoff de diseño; no se
 * improvisan ni se "afinan a ojo".
 */
public enum Season {

	PRIMAVERA("Primavera", "Estancias de primavera — Brotes, campo verde y días largos",
			0x4E7A3E, 0xF3F1E6, 0x2A3A20, 0x232A1B, 0x7D8570, 0xE4E6D5, new Color(120, 150, 80, 41)),

	VERANO("Verano", "Estancias de verano — Sol alto, luz dorada y sombra fresca",
			0xC0870F, 0xFBF3E1, 0x33291A, 0x2B2513, 0x8C7F60, 0xF2E6C2, new Color(245, 195, 70, 46)),

	OTONO("Otoño", "Estancias de otoño — Hojas, viñedos y tardes doradas",
			0xA9701F, 0xF4EDE0, 0x38291A, 0x29200F, 0x8A7D66, 0xEADFC9, new Color(170, 110, 30, 46)),

	INVIERNO("Invierno", "Estancias de invierno — Nieve, chimenea y luz de dusk",
			0x5A5B86, 0xECECF1, 0x242539, 0x22233A, 0x7C7C93, 0xDDDCE6, new Color(96, 98, 150, 38));

	private final String nombre;
	private final String etiqueta;

	private final Color acc;
	private final Color bg;
	private final Color hdr;
	private final Color txt;
	private final Color mut;
	private final Color img;
	private final Color imgTint;

	Season(String nombre, String etiqueta, int acc, int bg, int hdr, int txt, int mut, int img, Color imgTint) {
		this.nombre = nombre;
		this.etiqueta = etiqueta;
		this.acc = new Color(acc);
		this.bg = new Color(bg);
		this.hdr = new Color(hdr);
		this.txt = new Color(txt);
		this.mut = new Color(mut);
		this.img = new Color(img);
		this.imgTint = imgTint;
	}

	/** Nombre corto para el selector de estación ("Primavera"). */
	public String nombre() {
		return nombre;
	}

	/** Frase editorial que encabeza el catálogo en esta estación. */
	public String etiqueta() {
		return etiqueta;
	}

	/** Acento: botones, elementos activos, precios destacados. */
	public Color acc() {
		return acc;
	}

	/** Fondo de página. */
	public Color bg() {
		return bg;
	}

	/** Barra de navegación (oscura). */
	public Color hdr() {
		return hdr;
	}

	/** Texto principal. */
	public Color txt() {
		return txt;
	}

	/** Texto secundario y líneas finas. */
	public Color mut() {
		return mut;
	}

	/** Color base de los huecos de imagen. */
	public Color img() {
		return img;
	}

	/** Velo translúcido que se superpone a las fotos para unificarlas. */
	public Color imgTint() {
		return imgTint;
	}

	/** El color en formato {@code #RRGGBB}, que es como lo esperan las propiedades de FlatLaf. */
	public static String hex(Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}

	/**
	 * La estación que corresponde a la fecha de hoy en el hemisferio norte.
	 *
	 * <p>
	 * Se usan las fechas astronómicas aproximadas (los equinoccios y solsticios
	 * varían un día según el año; esa precisión no aporta nada aquí). Es el valor
	 * con el que arranca la aplicación: abrirla en noviembre y encontrarla en
	 * otoño es parte de la idea.
	 */
	public static Season actual() {
		return actualPara(LocalDate.now());
	}

	/** Variante con fecha explícita: permite probar el cálculo sin depender del reloj. */
	static Season actualPara(LocalDate fecha) {

		MonthDay dia = MonthDay.from(fecha);

		if (dentro(dia, MonthDay.of(3, 20), MonthDay.of(6, 20))) {
			return PRIMAVERA;
		}
		if (dentro(dia, MonthDay.of(6, 21), MonthDay.of(9, 22))) {
			return VERANO;
		}
		if (dentro(dia, MonthDay.of(9, 23), MonthDay.of(12, 20))) {
			return OTONO;
		}
		return INVIERNO;
	}

	private static boolean dentro(MonthDay dia, MonthDay desde, MonthDay hasta) {
		return dia.compareTo(desde) >= 0 && dia.compareTo(hasta) <= 0;
	}
}
