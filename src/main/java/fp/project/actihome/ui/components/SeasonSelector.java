package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Selector de estación: la pieza que reambienta la aplicación entera.
 *
 * <p>
 * Cuatro pestañas en versalita —Primavera, Verano, Otoño, Invierno— bajo la
 * etiqueta "Viajar en". La activa va en el color de acento y subrayada; las
 * demás en el color secundario.
 *
 * <p>
 * <b>Es el control más importante del rediseño</b> y no porque haga mucho, sino
 * por lo que provoca: un clic cambia los siete colores del sistema, la
 * ilustración de Olaz y el glifo de la cabecera, de golpe y en toda la
 * aplicación. Es el argumento visual de todo el proyecto reducido a un gesto.
 *
 * <p>
 * <b>Cómo se propaga el cambio.</b> Este componente no conoce a nadie: se limita
 * a llamar a {@link Theme#cambiarA(Season)}. A partir de ahí, quien se haya
 * apuntado se entera —incluido el tema de FlatLaf, que recalcula la paleta de
 * todos los controles estándar— y los componentes propios, que resuelven su
 * color en cada pintado, salen repintados con el color nuevo sin haberse
 * suscrito a nada. Es el patrón observador aplicado con cabeza: <b>el emisor no
 * sabe cuántos receptores hay ni quiénes son</b>.
 *
 * <p>
 * Sin subrayado en las inactivas no bastaría con el color: el subrayado da un
 * segundo canal —posición y peso, no solo tinta— para que la estación activa se
 * reconozca también sin distinguir bien los colores.
 */
public class SeasonSelector extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Separación entre pestañas, del handoff. */
	private static final int GAP = 22;

	public SeasonSelector() {
		this(false);
	}

	/**
	 * @param sobreCabecera versión compacta para la barra de navegación oscura: sin
	 *                      el rótulo "Viajar en" y con los colores del fondo oscuro.
	 *                      Vive ahí desde que se comprobó que el selector solo
	 *                      existía en el catálogo, así que estando en el detalle o en
	 *                      un formulario <b>no había forma de cambiar de estación</b>
	 *                      — siendo la característica más distintiva de la
	 *                      aplicación. Subirlo a la cabecera lo hace alcanzable desde
	 *                      las diecisiete pantallas y de paso devuelve al catálogo el
	 *                      alto que ocupaba
	 */
	public SeasonSelector(boolean sobreCabecera) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				sobreCabecera ? "[]" : "[]" + Space.SM + "[]"));
		setOpaque(false);

		if (!sobreCabecera) {
			add(alinearDerecha(Labels.caps("Viajar en")));
		}

		int gap = sobreCabecera ? Space.MD : GAP;

		JPanel pestanas = new JPanel(new MigLayout(Space.insets(0), "push[]" + gap + "[]" + gap + "[]" + gap + "[]", ""));
		pestanas.setOpaque(false);

		for (Season estacion : Season.values()) {
			pestanas.add(new Pestana(estacion, sobreCabecera));
		}

		add(pestanas);
	}

	/**
	 * El bloque del hero está alineado a la derecha, y una etiqueta dentro de una
	 * celda "fill" se dibuja pegada a la izquierda por defecto.
	 */
	private static JPanel alinearDerecha(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	/**
	 * Una de las cuatro pestañas.
	 *
	 * <p>
	 * <b>Segunda versión, y la primera dibujaba el texto a mano con
	 * {@code drawString}.</b> Eso es justo lo que producía el temblor que reportó
	 * el usuario: "las vocales de primavera, invierno, verano se mueven cuando
	 * interactúas con ellas". Se probó primero a fijar los hints de antialiasing y a
	 * repintar la fila entera en vez del ítem suelto, y no bastó — el temblor seguía.
	 *
	 * <p>
	 * La razón de fondo es que <b>ningún otro texto de la aplicación tiembla</b>:
	 * "By CocoBrain", el nombre de usuario, cualquier {@code JLabel} normal, se
	 * repinta sin problema decenas de veces por segundo con las partículas de fondo
	 * animándose por encima. La diferencia no estaba en los hints, estaba en que
	 * esos textos pasan por el motor de pintado <b>estándar</b> de Swing
	 * ({@code BasicLabelUI}), que ya resuelve de forma consistente cada detalle que
	 * aquí se intentaba fijar a mano, y este no. En vez de perseguir el hint exacto
	 * que faltaba, se deja de pintar texto a mano: la pestaña es ahora un
	 * {@code JLabel} de verdad más una barra fina para el subrayado. El color en
	 * hover/activo se cambia con {@code setForeground}, que es la vía normal.
	 */
	private static class Pestana extends JPanel {

		private static final long serialVersionUID = 1L;

		private static final int GROSOR_SUBRAYADO = 2;

		private final transient Season estacion;
		private final boolean sobreCabecera;
		private final javax.swing.JLabel etiqueta;
		private final JPanel subrayado;
		private boolean encima;

		Pestana(Season estacion, boolean sobreCabecera) {

			super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
			this.estacion = estacion;
			this.sobreCabecera = sobreCabecera;

			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(estacion.etiqueta());

			etiqueta = new javax.swing.JLabel(estacion.nombre().toUpperCase());
			etiqueta.setFont(Typography.label(sobreCabecera ? 10f : 12f));

			// El relleno sí se dibuja a mano, pero es un color plano sin texto: no hay
			// glifos que puedan salir distintos entre un repintado y otro.
			subrayado = new JPanel();
			subrayado.setOpaque(true);

			// Sin esto, un JPanel recién creado informa un mínimo de 10x10 -el de
			// FlowLayout vacío-, muy por encima de los 2px reales que le fuerza el "h
			// GROSOR_SUBRAYADO!" de abajo. MigLayout respeta igualmente el alto exacto
			// porque el "!" no negocia, así que no se veía nada roto, pero MedirResponsive
			// sí lo señalaba: un mínimo que miente sobre el tamaño real es la misma trampa
			// que ya está documentada en el proyecto, aunque aquí no llegue a doler.
			subrayado.setMinimumSize(new Dimension(0, GROSOR_SUBRAYADO));

			add(etiqueta, "alignx center");
			add(subrayado, "growx, h " + GROSOR_SUBRAYADO + "!");

			MouseAdapter interaccion = new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					Theme.cambiarA(estacion);
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					actualizarColores();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					actualizarColores();
				}
			};

			// En las dos capas: el ratón puede entrar directamente sobre la etiqueta de
			// texto sin pasar por el panel contenedor primero.
			addMouseListener(interaccion);
			etiqueta.addMouseListener(interaccion);

			actualizarColores();
		}

		private boolean esActiva() {
			return Theme.estacion() == estacion;
		}

		/**
		 * Recalcula el color del texto y la visibilidad del subrayado.
		 *
		 * <p>
		 * {@code setForeground} en un {@code JLabel} real dispara el repintado estándar
		 * de Swing, con los mismos hints que usa cualquier otra etiqueta de la
		 * aplicación — no hay ningún {@code Graphics2D} propio de por medio que pueda
		 * discrepar de un repintado a otro.
		 */
		private void actualizarColores() {

			boolean activa = esActiva();

			// Sobre la cabecera oscura no valen ni acc() ni mut(): el primero puede ser el
			// amarillo de verano, que contra el marrón oscuro pierde fuerza, y el segundo
			// está pensado para fondo claro y ahí queda casi ilegible. Se usa el blanco y
			// su versión atenuada, que es lo que ya hace el resto de la barra.
			Color tinta;

			if (sobreCabecera) {
				tinta = activa || encima ? Color.WHITE : Theme.mutSobreOscuro();
			} else {
				tinta = activa ? Theme.acc() : encima ? Theme.txt() : Theme.mut();
			}

			etiqueta.setForeground(tinta);

			// Sobre la cabecera, el subrayado usa el mismo blanco que ya usa el texto
			// activo (línea de arriba), no Theme.acc(): medido con MedirContraste
			// (entrada 032 del diario), el acento como barra sobre la cabecera oscura
			// daba 2,2-2,4:1 en tres de las cuatro estaciones —por debajo del 3:1 que
			// exige un componente de interfaz— y solo pasaba en verano. Fuera de la
			// cabecera (rama sin uso hoy, ver la nota de clase) se mantiene el acento,
			// que ahí sí tiene contraste de sobra contra el fondo claro.
			subrayado.setBackground(activa ? (sobreCabecera ? Color.WHITE : Theme.acc()) : getBackground());
			subrayado.setOpaque(activa);
			subrayado.repaint();
		}

		/**
		 * Se resuelve el color en cada repintado, no solo al hacer clic.
		 *
		 * <p>
		 * {@code Theme.cambiarA} no repinta directamente: dispara
		 * {@code FlatLaf.updateUI()}, que recorre las ventanas vivas y las obliga a
		 * repintarse, y es <em>esa</em> cascada la que tiene que enterarse de cuál de
		 * las cuatro pestañas es ahora la activa. Igual que antes hacía
		 * {@code esActiva()} dentro de {@code paintComponent}, aquí se vuelve a
		 * calcular el estado justo antes de delegar en el pintado estándar de Swing —
		 * la diferencia es que ya no hay ningún {@code drawString} de por medio.
		 */
		@Override
		public void paint(Graphics g) {

			actualizarColores();
			super.paint(g);
		}
	}
}
