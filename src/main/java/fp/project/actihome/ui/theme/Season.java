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
			0x4E7A3E, 0x4E7A3E, 0xF3F1E6, 0x2A3A20, 0x232A1B, 0x7D8570, 0xE4E6D5, new Color(120, 150, 80, 41),
			new Color(0xD9, 0x6A, 0x96)),

	// Verano se aparta del handoff (ADR-008). Ver la nota extensa sobre accText más
	// abajo: el relleno es un amarillo natural y el texto, un oro oscuro. El
	// original (#C0870F para todo) daba 2,83:1 como texto sobre el crema, muy por
	// debajo del mínimo legible.
	VERANO("Verano", "Estancias de verano — Sol alto, luz dorada y sombra fresca",
			0xE0AC1B, 0x806210, 0xFBF3E1, 0x33291A, 0x2B2513, 0x8C7F60, 0xF2E6C2, new Color(245, 195, 70, 46),
			new Color(0xD9, 0xA6, 0x2A)),

	// Otoño se aparta del handoff a propósito (ADR-005). Los valores originales
	// (acc #A9701F, hdr #38291A, mut #8A7D66) eran casi los mismos que los de
	// verano: siete grados de matiz de diferencia en el acento y cabeceras
	// prácticamente idénticas. Dos estaciones que se ven igual vacían de sentido el
	// selector. Aquí el matiz se desplaza hacia el terracota y se baja la
	// saturación: verano es oro de sol, otoño es tierra y hoja seca.
	//
	// Ajustado después hacia el rojo (#9C5A2E → #A34526) por preferencia del
	// usuario: hoja de otoño en vez de barro. Sigue sin ser rojo vivo —la
	// saturación se mantiene contenida— y gana contraste como texto (4,49 → 5,12).
	OTONO("Otoño", "Estancias de otoño — Hojas, viñedos y tardes doradas",
			0xA34526, 0xA34526, 0xF1EAE0, 0x3D2820, 0x2E211A, 0x8A7566, 0xE6DACB, new Color(150, 85, 45, 51),
			new Color(0xB5, 0x4E, 0x2A)),

	INVIERNO("Invierno", "Estancias de invierno — Nieve, chimenea y luz de dusk",
			0x5A5B86, 0x5A5B86, 0xECECF1, 0x242539, 0x22233A, 0x7C7C93, 0xDDDCE6, new Color(96, 98, 150, 38),
			new Color(0xAE, 0xB2, 0xCC));

	private final String nombre;
	private final String etiqueta;

	private final Color acc;
	private final Color accText;
	private final Color bg;
	private final Color hdr;
	private final Color txt;
	private final Color mut;
	private final Color img;
	private final Color imgTint;
	private final Color particula;

	Season(String nombre, String etiqueta, int acc, int accText, int bg, int hdr, int txt, int mut, int img,
			Color imgTint, Color particula) {
		this.nombre = nombre;
		this.etiqueta = etiqueta;
		this.acc = new Color(acc);
		this.accText = new Color(accText);
		this.bg = new Color(bg);
		this.hdr = new Color(hdr);
		this.txt = new Color(txt);
		this.mut = new Color(mut);
		this.img = new Color(img);
		this.imgTint = imgTint;
		this.particula = particula;
	}

	/** Nombre corto para el selector de estación ("Primavera"). */
	public String nombre() {
		return nombre;
	}

	/** Frase editorial completa: "Estancias de verano — Sol alto, luz dorada y sombra fresca". */
	public String etiqueta() {
		return etiqueta;
	}

	/**
	 * Solo la parte poética de la frase: "Sol alto, luz dorada y sombra fresca".
	 *
	 * <p>
	 * En sitios estrechos —el panel de marca del login, por ejemplo— la frase
	 * entera no cabe y se corta con puntos suspensivos, que es la peor forma de
	 * mostrar una frase escrita con cuidado. La primera mitad ("Estancias de
	 * verano") además suele ser redundante, porque la estación ya se sabe por el
	 * contexto.
	 */
	public String frase() {

		int guion = etiqueta.indexOf('—');
		return guion < 0 ? etiqueta : etiqueta.substring(guion + 1).trim();
	}

	/** Acento de <b>relleno</b>: fondo de botones, discos, chips activos. */
	public Color acc() {
		return acc;
	}

	/**
	 * Acento de <b>texto</b>: precios, versalitas y enlaces sobre fondo claro.
	 *
	 * <p>
	 * <b>Por qué hacen falta dos acentos y no uno</b> (ADR-008). Un color de relleno
	 * y un color de texto tienen requisitos opuestos: el relleno puede ser luminoso
	 * —basta con que la etiqueta que va encima contraste con él— mientras que el
	 * texto tiene que contrastar con el fondo crema de la página, y para eso
	 * necesita ser oscuro.
	 *
	 * <p>
	 * Con un solo token, verano era el caso imposible: su acento daba <b>2,83:1</b>
	 * como texto sobre el crema, muy por debajo del 4,5 que necesita una versalita
	 * de 10px, y aun así no era un amarillo de verdad. Separándolos, el relleno
	 * puede ser el amarillo natural que pide el diseño (#E0AC1B, con 7,3:1 frente a
	 * la etiqueta oscura) y el texto un oro oscuro legible (#806210, 5,2:1 sobre el
	 * crema).
	 *
	 * <p>
	 * En las otras tres estaciones los dos valores coinciden: su acento ya era
	 * suficientemente oscuro para las dos cosas. El token existe igualmente para que
	 * la regla sea la misma en todas y no haya que recordar cuál es la excepción.
	 */
	public Color accText() {
		return accText;
	}

	/**
	 * Color de las partículas que flotan por el fondo.
	 *
	 * <p>
	 * No se deriva del acento, y cada estación explica por qué:
	 *
	 * <ul>
	 * <li><b>Primavera</b>: los pétalos son de cerezo, y un pétalo de cerezo es
	 * rosa. Con el acento serían verdes, que no es ningún pétalo.</li>
	 * <li><b>Otoño</b>: una hoja seca es más viva que el terracota de la interfaz;
	 * usar el acento las dejaba apagadas contra el fondo.</li>
	 * <li><b>Invierno</b>: es el caso que obligó a crear este token. La nieve
	 * blanca sobre el fondo lila de invierno da <b>1,18:1</b>, o sea, es
	 * literalmente invisible. Un blanco azulado más profundo sí se ve, y además es
	 * como se ve la nieve de verdad contra un cielo claro: gris, no blanca.</li>
	 * </ul>
	 */
	public Color particula() {
		return particula;
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
