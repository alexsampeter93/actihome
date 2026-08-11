package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.Timer;

import fp.project.actihome.ui.theme.Animacion;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Fábrica de botones del sistema de diseño.
 *
 * <p>
 * Tres niveles de énfasis, y la regla de uso importa tanto como el aspecto:
 * <ul>
 * <li><b>Primario</b> — relleno con el acento. <b>Uno por pantalla.</b> Es la
 * acción que el usuario ha venido a hacer: reservar, publicar, confirmar. Dos
 * botones primarios en la misma pantalla equivalen a ninguno, porque el ojo ya
 * no sabe dónde ir.</li>
 * <li><b>Secundario</b> — solo contorno. Acciones disponibles pero no
 * protagonistas: cancelar, volver, ver detalle.</li>
 * <li><b>Enlace</b> — texto con filete inferior, sin caja. Acciones terciarias
 * dentro de una lista o un pie: "Ver estancia →".</li>
 * </ul>
 *
 * <p>
 * Todos se pintan a mano en lugar de dejárselo a FlatLaf. No es capricho: el
 * diseño pide un relleno concreto, un contorno de una sola línea fina y unos
 * estados que el Look and Feel no sabe hacer, y conseguirlo con propiedades
 * sueltas acabaría en una lista de excepciones difícil de mantener. Además,
 * pintando podemos leer el color de la estación en cada pasada y así el botón
 * cambia de color solo.
 *
 * <h2>El botón grabado</h2>
 *
 * <p>
 * <b>Radio 0 y un filete por dentro.</b> Hasta la revisión del 07-08-2026 el
 * botón era un rectángulo de esquinas redondeadas que se oscurecía catorce
 * puntos al pasar el ratón: correcto, y sin ningún carácter. El sustituto no
 * inventa un lenguaje nuevo, toma el que ya usa el resto del sistema —líneas de
 * un píxel, sin sombras difusas, precisión antes que amabilidad— y lo aplica al
 * único sitio donde no estaba.
 *
 * <p>
 * La referencia es una <b>tarjeta de visita grabada</b>: el filete separado unos
 * puntos del borde es el recurso clásico de la papelería fina, y no aparece en
 * ninguna interfaz de plantilla porque no viene de ahí. Al pasar el ratón el
 * filete <b>se abre hacia el borde</b> en lugar de encenderse un color: el botón
 * responde con geometría, que es más difícil de conseguir y mucho menos común.
 *
 * <p>
 * <b>Las esquinas rectas no son una decisión de gusto.</b> Un radio de 4 sobre
 * un botón de 44 de alto es un gesto tan pequeño que no se lee como intención,
 * solo como "lo que traía el tema por defecto". Cero se lee como una decisión.
 *
 * <h2>La etiqueta en versalita, que era lo que más delataba</h2>
 *
 * <p>
 * <b>Los botones eran el único sitio del sistema con texto en caja baja.</b> El
 * handoff pide versalita con {@code letter-spacing} para toda etiqueta pequeña, y
 * así están los rótulos de campo, los chips, las estadísticas y las miguitas. Los
 * botones se quedaron con una sans normal a 13 puntos — que es, exactamente, la
 * etiqueta que trae por defecto cualquier framework. Un sistema editorial con
 * botones de framework se contradice en el elemento que más veces se mira.
 *
 * <p>
 * El cambio no es solo de aspecto: la versalita <b>separa lo que es una acción de
 * lo que es una frase</b>. "Confirmar reserva" en caja baja compite con el texto
 * que tiene alrededor; "CONFIRMAR RESERVA" espaciada se lee como un rótulo, que es
 * lo que es. Va a 12 puntos y no a 13 a propósito: en caja alta, un cuerpo menor
 * ocupa un ancho parecido, así que las pantallas no cambian de tamaño por esto.
 *
 * <h2>La pulsación es física</h2>
 *
 * <p>
 * Al pulsar, el contenido <b>baja un punto</b> y el filete se apoya en el borde.
 * No cambia de color ni se oscurece un tono: se hunde. Es lo que hace un sello al
 * apoyarse, y cuesta un {@code translate} — la diferencia entre un control que
 * acusa el clic y uno que solo cambia de tinta.
 *
 * <h2>El enlace subrayado</h2>
 *
 * <p>
 * El filete inferior <b>crece de izquierda a derecha</b> al pasar el ratón, en
 * vez de aparecer entero. Es el gesto de un buen periódico digital, rima con el
 * subrayado que ya llevan las pestañas de estación de la cabecera, y —lo que
 * importa— dice en qué dirección se lee.
 */
public final class Buttons {

	private Buttons() {
	}

	/** Acción principal de la pantalla. Relleno con el acento. */
	public static JButton primary(String texto, ActionListener accion) {
		return crear(texto, Estilo.PRIMARIO, accion);
	}

	/** Acción secundaria. Solo contorno. */
	public static JButton secondary(String texto, ActionListener accion) {
		return crear(texto, Estilo.SECUNDARIO, accion);
	}

	/** Acción terciaria. Texto con filete inferior, sin caja. */
	public static JButton link(String texto, ActionListener accion) {
		return crear(texto, Estilo.ENLACE, accion);
	}

	/** Enlace con el color de acento, para acciones destacadas dentro de una ficha. */
	public static JButton linkAccent(String texto, ActionListener accion) {
		return crear(texto, Estilo.ENLACE_ACENTO, accion);
	}

	private static JButton crear(String texto, Estilo estilo, ActionListener accion) {

		ThemedButton boton = new ThemedButton(texto, estilo);

		if (accion != null) {
			boton.addActionListener(accion);
		}

		return boton;
	}

	private enum Estilo {
		PRIMARIO, SECUNDARIO, ENLACE, ENLACE_ACENTO
	}

	private static final class ThemedButton extends JButton {

		private static final long serialVersionUID = 1L;

		/** Separación del filete respecto al borde, en reposo. */
		private static final int GRABADO = 3;

		/**
		 * Cuerpo de la etiqueta de los botones con caja.
		 *
		 * <p>
		 * Doce y no trece, aunque ahora vaya en mayúsculas: en caja alta cada letra es
		 * más ancha, así que bajar un punto deja el rótulo ocupando aproximadamente lo
		 * mismo que ocupaba en caja baja. Es lo que permite cambiar la tipografía de los
		 * botones sin que ninguna pantalla necesite otra vez más ancho — que acababa de
		 * costar dos días de ajuste.
		 */
		private static final float CUERPO_DE_ROTULO = 12f;

		private final transient Estilo estilo;

		/**
		 * Cuánto está "encendido" el botón, de 0 a 1.
		 *
		 * <p>
		 * No es un booleano y esa es toda la diferencia: un booleano solo puede
		 * saltar. Guardar el valor intermedio es lo que permite que, si el ratón sale
		 * a mitad de la entrada, la animación de vuelta <b>arranque donde estaba</b> y
		 * no desde el final — que es como se ve un control que da tirones.
		 */
		private transient double encendido;

		/** La animación viva, para poder cancelarla antes de empezar otra. */
		private transient Timer animacion;

		/** El último estado conocido, para no relanzar la animación en cada evento. */
		private transient boolean estabaEncima;

		private ThemedButton(String texto, Estilo estilo) {

			super(texto);
			this.estilo = estilo;

			// Se desactiva todo el dibujado que aporta el Look and Feel: el fondo, el
			// borde y el rectángulo de foco. A partir de aquí pintamos nosotros.
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			boolean esEnlace = estilo == Estilo.ENLACE || estilo == Estilo.ENLACE_ACENTO;

			// Versalita con tracking en los botones con caja; caja baja en los enlaces.
			// La diferencia no es estética: un enlace es texto que va dentro de una frase
			// y debe leerse como texto, mientras que un botón es un rótulo.
			setFont(esEnlace ? Typography.sansSemiBold(Typography.BODY_SM) : Typography.label(CUERPO_DE_ROTULO));
			setBorder(esEnlace
					? BorderFactory.createEmptyBorder(Space.XXS, 0, Space.XXS, 0)
					: BorderFactory.createEmptyBorder(Space.SM, Space.XL, Space.SM, Space.XL));

			// El texto ya se fijó en el constructor de JButton, antes de que existiera
			// "estilo", así que se vuelve a fijar ahora para que pase por el filtro de
			// mayúsculas de setText.
			setText(texto);

			// Se escucha el MODELO y no el ratón directamente. Es lo correcto y además
			// resuelve gratis un caso que con MouseListener habría que programar aparte:
			// un botón desactivado a mitad de la animación deja de estar "rollover" y
			// vuelve solo a su estado de reposo.
			getModel().addChangeListener(e -> comprobarEstado());
		}

		/** Arranca la animación solo cuando el estado cambia de verdad. */
		private void comprobarEstado() {

			boolean encima = getModel().isRollover() && isEnabled();

			if (encima == estabaEncima) {
				return;
			}

			estabaEncima = encima;

			Animacion.cancelar(animacion);
			animacion = Animacion.animar(this, encendido, encima ? 1 : 0, Animacion.CONTROL, v -> encendido = v);
		}

		/**
		 * Pone el rótulo en mayúsculas, salvo en los enlaces.
		 *
		 * <p>
		 * Se hace aquí y no en cada llamada por dos motivos. Uno, que hay setenta y
		 * tantas llamadas y una que se olvidara rompería la uniformidad justo donde más
		 * se nota. Y dos, y más importante, que <b>el texto se vuelve a fijar cada vez
		 * que cambia el idioma</b> ({@code actualizarTextos()} en cada pantalla): un
		 * {@code toUpperCase} en la llamada solo funcionaría hasta que alguien cambiara
		 * a inglés.
		 *
		 * <p>
		 * Con el {@code Locale} activo y no con el de la máquina, que en turco
		 * convierte la "i" en "İ" y dejaría los botones con una letra que nadie escribió.
		 */
		@Override
		public void setText(String texto) {

			boolean esEnlace = estilo == Estilo.ENLACE || estilo == Estilo.ENLACE_ACENTO;

			// estilo es null la primera vez: JButton fija el texto en su constructor,
			// antes de que esta clase haya asignado nada.
			if (estilo == null || esEnlace || texto == null) {
				super.setText(texto);
				return;
			}

			super.setText(texto.toUpperCase(Textos.idioma()));
		}

		@Override
		public Color getForeground() {

			if (estilo == null) {
				return super.getForeground();
			}

			switch (estilo) {
			case PRIMARIO:
				return Theme.onAccent();
			case ENLACE_ACENTO:
				return Theme.accText();
			default:
				return Theme.txt();
			}
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int ancho = getWidth();
			int alto = getHeight();
			boolean pulsado = getModel().isArmed() && getModel().isPressed();

			switch (estilo) {
			case PRIMARIO:
				pintarPrimario(g2, ancho, alto, pulsado);
				break;

			case SECUNDARIO:
				pintarSecundario(g2, ancho, alto, pulsado);
				break;

			default:
				pintarEnlace(g2, ancho, alto);
				break;
			}

			g2.dispose();

			// **El rótulo baja un punto al pulsar.** Es la mitad del gesto: el fondo ya se
			// ha pintado con el filete apoyado en el borde, y mover ahora el texto es lo
			// que convierte las dos cosas en un bloque que se hunde. Se desplaza el
			// Graphics que recibe JButton, no el nuestro, porque la etiqueta la dibuja él.
			if (pulsado) {
				g.translate(0, 1);
				super.paintComponent(g);
				g.translate(0, -1);
				return;
			}

			super.paintComponent(g);
		}

		/** Relleno de acento y filete claro por dentro, que se abre al pasar el ratón. */
		private void pintarPrimario(Graphics2D g2, int ancho, int alto, boolean pulsado) {

			g2.setColor(ajustar(Theme.acc(), pulsado ? -30 : 0));
			g2.fillRect(0, 0, ancho, alto);

			// El filete es del color del texto, muy rebajado: así funciona igual sobre el
			// amarillo de verano —donde el texto es oscuro— que sobre las otras tres,
			// donde es blanco. Un color fijo habría necesitado una excepción por estación.
			g2.setColor(transparente(getForeground(), pulsado ? 0.55 : 0.30 + 0.25 * encendido));
			pintarFilete(g2, ancho, alto);
		}

		/**
		 * Escuadras de imprenta que se cierran hasta formar el marco.
		 *
		 * <p>
		 * <b>Antes era un rectángulo completo que se lavaba un 5 % al pasar el ratón</b>
		 * — correcto, invisible y exactamente igual que el botón de contorno de
		 * cualquier framework. El problema de un marco ya cerrado es que no tiene a
		 * dónde ir: lo único que puede hacer es cambiar de color.
		 *
		 * <p>
		 * Con {@link Escuadras}, el reposo son cuatro ángulos y el marco se
		 * <b>termina de dibujar</b> al acercarse. Es un gesto de geometría, no de tinta,
		 * y viene del taller de imprenta en vez de la librería de turno. El lavado del
		 * fondo se mantiene, muy leve, porque el área pulsable tiene que notarse también
		 * en el centro y no solo en el contorno.
		 */
		private void pintarSecundario(Graphics2D g2, int ancho, int alto, boolean pulsado) {

			// **Sin lavado de fondo, en ningún estado.** Rellenar la caja de gris es lo
			// que convertía este botón en el botón de contorno de cualquier framework: el
			// gesto pasaba a ser "se pinta de gris" y las escuadras quedaban de adorno.
			// Lo único que cambia aquí es la línea —y, al pulsar, el rótulo que baja un
			// punto—, que es de lo que va este control.

			// Trazo de un punto y remate recto: el mismo que los iconos dibujados del
			// sistema. Un remate redondeado dejaría las esquinas romas y las escuadras
			// perderían su parecido con una marca de corte.
			g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			g2.setColor(Animacion.mezclar(Theme.FIELD_BORDER, Theme.txt(), pulsado ? 0.75 : 0.15 + 0.45 * encendido));

			Escuadras.pintar(g2, 0.5, 0.5, ancho - 1.0, alto - 1.0, pulsado ? 1 : encendido);
		}

		/**
		 * El filete grabado: un rectángulo de una línea, separado del borde.
		 *
		 * <p>
		 * La separación va de {@link #GRABADO} a cero según lo encendido que esté el
		 * botón, así que al pasar el ratón el filete <b>se abre</b> hasta apoyarse en
		 * el borde. Es el movimiento de un sello que encaja.
		 */
		private void pintarFilete(Graphics2D g2, int ancho, int alto) {

			int margen = (int) Math.round(GRABADO * (1 - encendido));

			// Por debajo de un cierto tamaño el filete y el borde se pisan y el resultado
			// es una línea gorda y sucia en vez de dos finas. Más vale no pintarlo.
			if (ancho - 2 * margen < 8 || alto - 2 * margen < 8) {
				return;
			}

			g2.drawRect(margen, margen, ancho - 2 * margen - 1, alto - 2 * margen - 1);
		}

		/**
		 * Texto con un filete inferior que crece de izquierda a derecha.
		 *
		 * <p>
		 * <b>La línea empieza donde empieza el texto, no en x=0</b>, y esa era una
		 * diferencia que se veía fea. Un JButton centra su etiqueta, así que en cuanto
		 * el layout le da más ancho del que el texto necesita —una fila con otros
		 * elementos, una columna con "grow"— el texto se va al centro y el subrayado se
		 * quedaba pegado al borde izquierdo: una raya suelta a la izquierda de la
		 * palabra.
		 *
		 * <p>
		 * En reposo la línea está entera pero muy rebajada, y al pasar el ratón crece
		 * una segunda a plena intensidad por encima. Así el enlace <b>siempre parece un
		 * enlace</b> —sin depender de que alguien lo señale, que es la trampa de los
		 * subrayados que solo aparecen al hover— y aun así responde.
		 */
		private void pintarEnlace(Graphics2D g2, int ancho, int alto) {

			int base = getBaseline(ancho, alto);

			if (base <= 0) {
				return;
			}

			int anchoTexto = getFontMetrics(getFont()).stringWidth(getText());
			int x = Math.max(0, (ancho - anchoTexto) / 2);
			int y = base + 2;

			g2.setColor(transparente(getForeground(), 0.35));
			g2.fillRect(x, y, anchoTexto, 1);

			if (encendido > 0) {
				g2.setColor(getForeground());
				g2.fillRect(x, y, (int) Math.round(anchoTexto * encendido), 1);
			}
		}

		/**
		 * Aclara u oscurece un color. Se usa para el estado de pulsado, en lugar de
		 * definir un color más por estación: así el sistema de tokens no crece y los
		 * estados salen solos en las cuatro paletas.
		 */
		private static Color ajustar(Color color, int delta) {

			return new Color(
					Math.max(0, Math.min(255, color.getRed() + delta)),
					Math.max(0, Math.min(255, color.getGreen() + delta)),
					Math.max(0, Math.min(255, color.getBlue() + delta)),
					color.getAlpha());
		}

		/** El mismo color con la opacidad dada, de 0 a 1. */
		private static Color transparente(Color color, double opacidad) {

			int alfa = (int) Math.round(255 * Math.max(0, Math.min(1, opacidad)));

			return new Color(color.getRed(), color.getGreen(), color.getBlue(), alfa);
		}
	}
}
