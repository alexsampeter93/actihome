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
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.ui.theme.Space;
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
 * <b>Selección de dos clics, sin arrastrar.</b> El primer clic en un día libre
 * lo fija como entrada; el segundo, si el tramo hasta él no cruza ningún día
 * ocupado, lo fija como salida. Pulsar un día anterior o igual al de entrada
 * reinicia la selección ahí. Pulsar uno posterior que cruza un día ocupado
 * también reinicia la selección en el día pulsado, en vez de dejar la
 * selección a medias o mostrar un error: el usuario ya ha dicho "quiero
 * empezar aquí", y eso es más útil que un mensaje.
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
 */
public class CalendarioRango extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Locale ES = new Locale("es", "ES");
	private static final String[] DIAS_SEMANA = { "L", "M", "X", "J", "V", "S", "D" };
	private static final int LADO_CELDA = 36;
	private static final int FILAS_REJILLA = 6;

	private final transient Runnable alCambiar;
	private final transient List<Reservation> ocupacion = new ArrayList<>();

	private YearMonth mesVisible = YearMonth.now();
	private LocalDate inicio;
	private LocalDate fin;

	private JLabel etiquetaMes;
	private JButton botonAnterior;
	private JPanel rejilla;

	public CalendarioRango(Runnable alCambiar) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XS + "[]" + Space.XXS + "[]"));

		this.alCambiar = alCambiar;
		setOpaque(false);

		add(cabecera(), "growx");
		add(diasDeLaSemana(), "growx");

		rejilla = new JPanel();
		rejilla.setOpaque(false);
		add(rejilla, "growx");

		pintarMes();
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
		pintarMes();
	}

	/** Fija una selección inicial, p. ej. al abrir la pantalla. */
	public void seleccionar(LocalDate inicio, LocalDate fin) {

		this.inicio = inicio;
		this.fin = fin;
		mesVisible = YearMonth.from(inicio != null ? inicio : LocalDate.now());
		pintarMes();
	}

	public LocalDate getInicio() {
		return inicio;
	}

	public LocalDate getFin() {
		return fin;
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]push[]push[]", "[]"));
		panel.setOpaque(false);

		botonAnterior = Buttons.link("←", e -> cambiarMes(-1));
		etiquetaMes = Labels.body(" ");

		panel.add(botonAnterior);
		panel.add(etiquetaMes, "alignx center");
		panel.add(Buttons.link("→", e -> cambiarMes(1)));

		return panel;
	}

	private JPanel diasDeLaSemana() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]0[]0[]0[]0[]0[]0[]", "[]"));
		fila.setOpaque(false);

		for (String dia : DIAS_SEMANA) {

			JLabel etiqueta = Labels.caps(dia);
			Dimension tamano = new Dimension(LADO_CELDA, etiqueta.getPreferredSize().height);
			etiqueta.setPreferredSize(tamano);
			etiqueta.setMinimumSize(tamano);
			etiqueta.setHorizontalAlignment(JLabel.CENTER);

			fila.add(etiqueta);
		}

		return fila;
	}

	private void cambiarMes(int delta) {

		mesVisible = mesVisible.plusMonths(delta);
		pintarMes();
	}

	/** Reconstruye la cabecera del mes y las 42 celdas de la rejilla (6 semanas fijas). */
	private void pintarMes() {

		String nombreMes = mesVisible.getMonth().getDisplayName(TextStyle.FULL, ES);
		etiquetaMes.setText(Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1) + " " + mesVisible.getYear());
		botonAnterior.setEnabled(mesVisible.isAfter(YearMonth.from(minimoSeleccionable())));

		rejilla.removeAll();
		rejilla.setLayout(new MigLayout("wrap 7, " + Space.insets(0), "[]0[]0[]0[]0[]0[]0[]", ""));

		int diasEnMes = mesVisible.lengthOfMonth();
		// ISO: lunes = 1 ... domingo = 7. La rejilla empieza en lunes.
		int huecosIniciales = mesVisible.atDay(1).getDayOfWeek().getValue() - 1;

		for (int i = 0; i < huecosIniciales; i++) {
			rejilla.add(relleno());
		}

		for (int dia = 1; dia <= diasEnMes; dia++) {
			rejilla.add(new Dia(mesVisible.atDay(dia)));
		}

		int celdasUsadas = huecosIniciales + diasEnMes;
		for (int i = celdasUsadas; i < FILAS_REJILLA * DIAS_SEMANA.length; i++) {
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
		hueco.setMinimumSize(tamano);

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
		return !dia.isBefore(minimoSeleccionable()) && !ocupado(dia);
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

		pintarMes();
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
			setMinimumSize(tamano);

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
