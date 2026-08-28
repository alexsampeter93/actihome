package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Selector de rango de fechas en forma de calendario, para elegir entrada y
 * salida al reservar.
 *
 * <p>
 * <b>Sustituye a dos {@code JSpinner} de fecha</b> ({@code ReserveHousingFrame}),
 * que dejaban elegir cualquier fecha sin decir cuáles ya estaban ocupadas: el
 * primer aviso de que dos reservas se pisaban llegaba al pulsar "Confirmar" y
 * recibir {@code AlreadyReservedException}. Aquí el día ocupado se ve —y no se
 * puede pulsar— antes de intentarlo.
 *
 * <p>
 * <b>Dos meses a la vez: el actual y el siguiente.</b> Es la petición que
 * resuelve el caso más incómodo de un calendario de un solo mes: reservar del
 * 28 de un mes al 3 del siguiente exigiría pulsar "siguiente" a medio camino y
 * perder de vista el día de entrada. Con los dos meses lado a lado, ese tramo
 * se ve entero y se elige con los mismos dos clics que cualquier otro.
 *
 * <p>
 * <b>Selección de dos clics, sin arrastrar.</b> El primer clic en un día libre
 * lo fija como entrada; el segundo, si el tramo hasta él no cruza ningún día
 * ocupado, lo fija como salida —da igual que los dos clics caigan en el mismo
 * bloque de mes o en bloques distintos, es la misma fecha para las dos cosas—.
 * Pulsar un día anterior o igual al de entrada reinicia la selección ahí.
 * Pulsar uno posterior que cruza un día ocupado también reinicia la selección
 * en el día pulsado, en vez de dejar la selección a medias o mostrar un
 * error: el usuario ya ha dicho "quiero empezar aquí", y eso es más útil que
 * un mensaje.
 *
 * <p>
 * <b>Un día ocupado no se puede elegir ni como entrada ni como salida</b>, ni
 * siquiera el mismo día en que otra reserva empieza. El servicio sí lo permite
 * —una reserva que sale la mañana en que otra entra no se pisa con ella,
 * ver {@code ReservationDao.existsOverlappingReservation}—, pero pedirle a
 * quien reserva que entienda esa sutileza mirando una rejilla de días
 * simplemente iguales no compensa: ningún día que el calendario ofrece como
 * pulsable puede terminar en un {@code AlreadyReservedException} de todos
 * modos, que es lo que de verdad importa.
 *
 * <p>
 * <b>Cada celda es un {@link JButton}</b>, no un {@code JPanel} con oyentes de
 * ratón. Es el mismo criterio que ya usa {@link Chip}: un botón trae gratis de
 * Swing el alcance con Tab, la activación con Espacio o Intro, y el estado
 * deshabilitado —usado aquí para los días pasados y los ocupados— sin tener
 * que reimplementar nada de eso a mano.
 *
 * <p>
 * <b>Ancho fijo, no un formulario.</b> Los dos meses miden {@link #ANCHO_PREFERIDO}
 * de ancho entre los dos, por encima de lo que {@code Layout.FORMULARIO} (440)
 * considera cómodo para un campo de texto — pero esa regla es para texto, y
 * esto es una rejilla: la misma excepción que ya vale para el catálogo
 * ("las rejillas sí crecen"). Quien coloque este componente debe darle su
 * propio ancho con {@code Layout.ancho(CalendarioRango.ANCHO_PREFERIDO)}, sin
 * heredar el límite de 440 que sí debe seguir aplicando al resto del
 * formulario.
 */
public class CalendarioRango extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Cuántos días tiene una semana. El texto de cada uno se traduce, esto no. */
	private static final int DIAS_SEMANA = 7;

	private static final int LADO_CELDA = 36;

	/**
	 * Lo más bajo que puede quedar una casilla cuando falta alto en la pantalla.
	 *
	 * <p>
	 * <b>El mínimo era igual que el preferido, y eso es exactamente lo que el
	 * manual desaconseja</b>: un componente que declara los dos iguales no cede
	 * nada, así que el calendario —seis filas de casillas por dos meses— era un
	 * bloque de 333 puntos rígido en mitad de una pantalla que tenía que caber en
	 * un portátil. Quien cedía en su lugar era todo lo demás.
	 *
	 * <p>
	 * Seis puntos por fila son 42 en total, que es justo lo que le faltaba a
	 * Buscar. Se cede <b>solo el alto</b>: el ancho sigue siendo {@link
	 * #LADO_CELDA} porque de él depende que quepan siete columnas de días, y 30
	 * puntos siguen siendo un objetivo de pulsación cómodo. Como el día se pinta
	 * midiendo su propia caja —rectángulo redondeado sobre {@code getHeight()} y
	 * número centrado con {@code FontMetrics}— la casilla solo queda algo más
	 * achatada, sin nada descolocado.
	 */
	private static final int ALTO_MINIMO_CELDA = 30;
	private static final int FILAS_REJILLA = 6;
	private static final int ANCHO_MES = DIAS_SEMANA * LADO_CELDA;
	private static final int ANCHO_FLECHA = 28;
	private static final int MESES_VISIBLES = 2;

	/** Ancho total que necesita el componente: úsalo para darle su propia columna. */
	public static final int ANCHO_PREFERIDO = 2 * ANCHO_FLECHA + 2 * Space.SM + MESES_VISIBLES * ANCHO_MES
			+ (MESES_VISIBLES - 1) * Space.XL;

	private final transient Runnable alCambiar;
	private final transient List<Reservation> ocupacion = new ArrayList<>();

	/** El primero de los dos meses visibles; el segundo es siempre el siguiente. */
	private YearMonth mesVisible = YearMonth.now();
	private LocalDate inicio;
	private LocalDate fin;

	/** Modo informativo: se ve la ocupación pero no se elige nada. Ver {@link #soloLectura()}. */
	private boolean soloLectura;

	private JButton botonAnterior;
	private final JLabel[] etiquetasMes = new JLabel[MESES_VISIBLES];
	private final JPanel[] rejillas = new JPanel[MESES_VISIBLES];
	private final JLabel[][] etiquetasDia = new JLabel[MESES_VISIBLES][DIAS_SEMANA];

	public CalendarioRango(Runnable alCambiar) {

		super(new MigLayout(Space.insets(0),
				"[]" + Space.SM + "[]" + Space.XL + "[]" + Space.SM + "[]", "[]"));

		this.alCambiar = alCambiar;
		setOpaque(false);

		botonAnterior = Buttons.link("←", e -> cambiarMes(-1));
		add(botonAnterior, "aligny top, w " + ANCHO_FLECHA + "!");

		for (int i = 0; i < MESES_VISIBLES; i++) {
			add(bloqueMes(i), "aligny top");
		}

		JButton botonSiguiente = Buttons.link("→", e -> cambiarMes(1));
		add(botonSiguiente, "aligny top, w " + ANCHO_FLECHA + "!");

		pintarMeses();
	}

	/** El día más temprano que se puede elegir como entrada: mañana, no hoy. */
	private static LocalDate minimoSeleccionable() {

		// La reserva rechaza una entrada anterior a "ahora" (MustBeTodayOrAfterException),
		// y comparar contra la medianoche de hoy fallaría esa prueba salvo que la
		// reserva se confirme antes de las 00:00. Es la misma razón por la que el
		// formulario anterior con JSpinner ya arrancaba en "mañana", no en "hoy".
		return LocalDate.now().plusDays(1);
	}

	/**
	 * Marca qué días están ya ocupados, a partir de las reservas del alojamiento.
	 * Se llama una vez al cargar la pantalla, no en cada repintado.
	 */
	public void setOcupacion(List<Reservation> reservas) {

		ocupacion.clear();
		ocupacion.addAll(reservas);
		pintarMeses();
	}

	/** Fija una selección inicial, p. ej. al abrir la pantalla. */
	public void seleccionar(LocalDate inicio, LocalDate fin) {

		this.inicio = inicio;
		this.fin = fin;
		mesVisible = YearMonth.from(inicio != null ? inicio : LocalDate.now());
		pintarMeses();
	}

	public LocalDate getInicio() {
		return inicio;
	}

	public LocalDate getFin() {
		return fin;
	}

	/** Un bloque de mes: su propio rótulo, la fila de días de la semana y la rejilla. */
	private JPanel bloqueMes(int indice) {

		JPanel bloque = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XS + "[]" + Space.XXS + "[]"));
		bloque.setOpaque(false);

		JLabel etiqueta = Labels.body(" ");
		etiquetasMes[indice] = etiqueta;
		bloque.add(etiqueta, "alignx center");

		bloque.add(diasDeLaSemana(indice));

		JPanel rejilla = new JPanel();
		rejilla.setOpaque(false);
		rejillas[indice] = rejilla;
		bloque.add(rejilla);

		return bloque;
	}

	/**
	 * Las claves i18n de los siete días, lunes primero (la rejilla empieza en
	 * lunes, no en domingo).
	 */
	private static final String[] CLAVES_DIA = { "calendario.dia.lun", "calendario.dia.mar", "calendario.dia.mie",
			"calendario.dia.jue", "calendario.dia.vie", "calendario.dia.sab", "calendario.dia.dom" };

	private JPanel diasDeLaSemana(int indiceBloque) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]0[]0[]0[]0[]0[]0[]", "[]"));
		fila.setOpaque(false);

		for (int i = 0; i < DIAS_SEMANA; i++) {

			JLabel etiqueta = Labels.caps(Textos.t(CLAVES_DIA[i]));
			Dimension tamano = new Dimension(LADO_CELDA, etiqueta.getPreferredSize().height);
			etiqueta.setPreferredSize(tamano);
			etiqueta.setMinimumSize(tamano);
			etiqueta.setHorizontalAlignment(JLabel.CENTER);

			etiquetasDia[indiceBloque][i] = etiqueta;
			fila.add(etiqueta);
		}

		return fila;
	}

	/**
	 * Vuelve a fijar el nombre de los días de la semana en el idioma activo.
	 * Se llama desde {@link #pintarMeses()}, que ya se invoca en cada visita a
	 * la pantalla (vía {@code setOcupacion}/{@code seleccionar}), así que no
	 * hace falta ningún gancho aparte para el idioma.
	 */
	private void actualizarDiasDeLaSemana() {

		for (JLabel[] bloque : etiquetasDia) {
			for (int i = 0; i < DIAS_SEMANA; i++) {
				bloque[i].setText(Textos.t(CLAVES_DIA[i]));
			}
		}
	}

	private void cambiarMes(int delta) {

		mesVisible = mesVisible.plusMonths(delta);
		pintarMeses();
	}

	/** Reconstruye los rótulos y las rejillas de los {@link #MESES_VISIBLES} meses. */
	private void pintarMeses() {

		botonAnterior.setEnabled(mesVisible.isAfter(YearMonth.from(minimoSeleccionable())));
		actualizarDiasDeLaSemana();

		for (int i = 0; i < MESES_VISIBLES; i++) {
			pintarMes(mesVisible.plusMonths(i), etiquetasMes[i], rejillas[i]);
		}
	}

	private void pintarMes(YearMonth mes, JLabel etiquetaMes, JPanel rejilla) {

		String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, Textos.idioma());
		etiquetaMes.setText(Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1) + " " + mes.getYear());

		rejilla.removeAll();
		rejilla.setLayout(new MigLayout("wrap 7, " + Space.insets(0), "[]0[]0[]0[]0[]0[]0[]", ""));

		int diasEnMes = mes.lengthOfMonth();
		// ISO: lunes = 1 ... domingo = 7. La rejilla empieza en lunes.
		int huecosIniciales = mes.atDay(1).getDayOfWeek().getValue() - 1;

		for (int i = 0; i < huecosIniciales; i++) {
			rejilla.add(relleno());
		}

		for (int dia = 1; dia <= diasEnMes; dia++) {
			rejilla.add(new Dia(mes.atDay(dia)));
		}

		int celdasUsadas = huecosIniciales + diasEnMes;
		for (int i = celdasUsadas; i < FILAS_REJILLA * DIAS_SEMANA; i++) {
			rejilla.add(relleno());
		}

		rejilla.revalidate();
		rejilla.repaint();
	}

	private JPanel relleno() {

		JPanel hueco = new JPanel();
		hueco.setOpaque(false);

		Dimension tamano = new Dimension(LADO_CELDA, LADO_CELDA);
		hueco.setPreferredSize(tamano);
		hueco.setMinimumSize(new Dimension(LADO_CELDA, ALTO_MINIMO_CELDA));

		return hueco;
	}

	private boolean ocupado(LocalDate dia) {

		for (Reservation reserva : ocupacion) {

			LocalDate checkIn = reserva.getCheckIn().toLocalDate();
			LocalDate checkOut = reserva.getCheckOut().toLocalDate();

			if (!dia.isBefore(checkIn) && dia.isBefore(checkOut)) {
				return true;
			}
		}

		return false;
	}

	private boolean seleccionable(LocalDate dia) {
		return !soloLectura && !dia.isBefore(minimoSeleccionable()) && !ocupado(dia);
	}

	/**
	 * Convierte el calendario en informativo: se ve la ocupación pero no se puede
	 * elegir nada (Fase 8.4).
	 *
	 * <p>
	 * Lo usa la ficha del alojamiento, que enseña qué días están cogidos como parte
	 * de la información —igual que el precio o las comodidades— pero no es el sitio
	 * donde se reserva. Elegir fechas aquí llevaría a un callejón: no hay ningún
	 * botón de confirmar al lado.
	 *
	 * <p>
	 * Es un modo del mismo componente y no un calendario aparte a propósito. Toda
	 * la parte difícil —qué día cae en qué columna, qué semana empieza el mes, qué
	 * días cubre una reserva— es idéntica, y de un componente gemelo lo que se
	 * acaba obteniendo es que un día los dos discrepen sobre qué está ocupado.
	 */
	public void soloLectura() {
		this.soloLectura = true;
		pintarMeses();
	}

	/**
	 * Un día libre entre dos fechas, incluidos los dos extremos.
	 *
	 * <p>
	 * Se llama solo cuando ya hay una entrada elegida y se pulsa una posible
	 * salida: si algún día del tramo está ocupado, el tramo pedido se solaparía
	 * con esa reserva y el servicio lo rechazaría igualmente.
	 */
	private boolean tramoLibre(LocalDate desde, LocalDate hasta) {

		for (LocalDate cursor = desde; !cursor.isAfter(hasta); cursor = cursor.plusDays(1)) {

			if (ocupado(cursor)) {
				return false;
			}
		}

		return true;
	}

	private void alPulsarDia(LocalDate dia) {

		if (inicio == null || fin != null || !dia.isAfter(inicio) || !tramoLibre(inicio, dia)) {
			// Empieza una selección nueva: no había entrada, ya había un rango
			// completo, se ha pulsado un día igual o anterior a la entrada, o el
			// tramo hasta aquí cruza un día ocupado.
			inicio = dia;
			fin = null;

		} else {
			fin = dia;
		}

		pintarMeses();
		alCambiar.run();
	}

	/** Una celda del calendario: un día del mes, pulsable si está libre y no ha pasado. */
	private class Dia extends JButton {

		private static final long serialVersionUID = 1L;

		private final transient LocalDate fecha;

		Dia(LocalDate fecha) {

			this.fecha = fecha;
			boolean seleccionable = seleccionable(fecha);

			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setEnabled(seleccionable);
			setCursor(Cursor.getPredefinedCursor(seleccionable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

			Dimension tamano = new Dimension(LADO_CELDA, LADO_CELDA);
			setPreferredSize(tamano);
			setMinimumSize(new Dimension(LADO_CELDA, ALTO_MINIMO_CELDA));

			addActionListener(e -> alPulsarDia(fecha));
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			boolean extremo = fecha.equals(inicio) || fecha.equals(fin);
			boolean enRango = inicio != null && fin != null && fecha.isAfter(inicio) && fecha.isBefore(fin);

			if (extremo) {
				g2.setColor(Theme.acc());
				g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 8, 8);

			} else if (enRango) {
				Color acc = Theme.acc();
				g2.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 55));
				g2.fillRect(0, 3, getWidth(), getHeight() - 6);

			} else if (isEnabled() && getModel().isRollover()) {
				g2.setColor(Theme.HAIRLINE);
				g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 8, 8);
			}

			Color tinta;

			if (extremo) {
				tinta = Theme.onAccent();
			} else if (!isEnabled()) {
				Color mut = Theme.mut();
				tinta = new Color(mut.getRed(), mut.getGreen(), mut.getBlue(), 105);
			} else if (fecha.equals(LocalDate.now())) {
				tinta = Theme.accText();
			} else {
				tinta = Theme.txt();
			}

			g2.setColor(tinta);
			g2.setFont(Typography.sansMedium(13f));

			FontMetrics fm = g2.getFontMetrics();
			String texto = String.valueOf(fecha.getDayOfMonth());
			int x = (getWidth() - fm.stringWidth(texto)) / 2;
			int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
			g2.drawString(texto, x, y);

			g2.dispose();
		}

		@Override
		public void paint(Graphics g) {

			super.paint(g);
			Foco.pintarAnillo((Graphics2D) g, this);
		}
	}
}
