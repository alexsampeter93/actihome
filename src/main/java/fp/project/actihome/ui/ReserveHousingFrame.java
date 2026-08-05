package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CheckOutMustBeOneDayAfterException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.MustBeTodayOrAfterException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.WrongCreditCardNumberException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.CalendarioRango;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Reservar un alojamiento.
 *
 * <p>
 * Un calendario para elegir entrada y salida ({@link fp.project.actihome.ui.components.CalendarioRango},
 * Fase 7.5.2 — sustituye a los dos {@code JSpinner} de fecha que había antes,
 * que dejaban elegir cualquier día sin decir cuáles ya estaban ocupados), la
 * tarjeta, y un resumen que se recalcula mientras se eligen las fechas: precio
 * por noche × noches = total. Ver el total moverse al cambiar la fecha es lo
 * que convierte una resta mental en información inmediata; pedir que se pulse
 * "calcular" sería fricción sin ningún beneficio, porque el cálculo no tiene
 * coste.
 *
 * <p>
 * <b>Los mensajes de error son uno por excepción</b>, no un "Error en los
 * datos" genérico (bug B11, corregido aquí para esta pantalla). El servicio ya
 * distingue tarjeta inválida, fecha de entrada pasada, estancia de cero noches
 * y alojamiento ya reservado con cuatro excepciones distintas; la interfaz
 * anterior las capturaba todas juntas con {@code catch (Exception ex)} y decía
 * siempre lo mismo, así que quien se equivocaba de campo no tenía forma de
 * saber cuál corregir.
 */
@Component
@Profile("!test")
@Lazy
public class ReserveHousingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * Duración de la simulación de pago (F13). Ni tan corta que parezca que no
	 * ha pasado nada —esa es la pista de que "reservar" y "pagar" son el mismo
	 * clic sin fricción, que es justo lo que no transmite una pasarela real—, ni
	 * tan larga que se sienta como una espera de verdad.
	 */
	private static final int DURACION_PAGO_SIMULADO_MS = 900;

	private final transient ReservationService reservationService;
	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private Long housingId;
	private transient Housing housing;

	private JLabel superTitulo;
	private JLabel tituloAlojamiento;
	private JLabel subtituloUbicacion;
	private JLabel etiquetaFechas;
	private CalendarioRango calendario;
	private JLabel etiquetaPago;
	private Field titular;
	private Field tarjeta;
	private Field caducidad;
	private Field cvv;
	private JLabel resumen;
	private JLabel error;
	private JButton botonConfirmar;
	private JLabel enlaceCancelar;

	public ReserveHousingFrame(ReservationService reservationService, HousingService housingService,
			SessionManager sessionManager, Navigator navigator) {

		this.reservationService = reservationService;
		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	public void setHousingId(Long id) {
		this.housingId = id;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			actualizarTextosFijos();
			cargarAlojamiento();
			limpiar();
		}

		super.setVisible(visible);
	}

	/**
	 * Vuelve a fijar los textos fijos de la pantalla en el idioma activo
	 * (Fase 7.6). Hace falta porque es un frame singleton: {@code initUI()} solo
	 * se ejecuta una vez en toda la sesión, así que estos textos, fijados en la
	 * construcción, no se enterarían solos de un cambio de idioma posterior.
	 */
	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("reservar.titulo"));
		etiquetaFechas.setText(Textos.t("reservar.entradaYSalida"));
		etiquetaPago.setText(Textos.t("reservar.pago.label"));
		titular.setEtiqueta(Textos.t("reservar.titular"));
		tarjeta.setEtiqueta(Textos.t("reservar.tarjeta"));
		caducidad.setEtiqueta(Textos.t("reservar.caducidad"));
		cvv.setEtiqueta(Textos.t("reservar.cvv"));
		botonConfirmar.setText(Textos.t("reservar.confirmar"));
		enlaceCancelar.setText(Textos.t("reservar.cancelar"));
	}

	private void cargarAlojamiento() {

		if (housingId == null) {
			return;
		}

		try {
			housing = housingService.findHousing(housingId);
			tituloAlojamiento.setText(housing.getName());
			subtituloUbicacion.setText(housing.getLocation());
			calendario.setOcupacion(reservationService.showHousingReservations(housingId));

		} catch (InstanceNotFoundException ex) {
			navigator.ir(ShowHousingsFrame.class);
		}
	}

	private void limpiar() {

		LocalDate manana = LocalDate.now().plusDays(1);

		calendario.seleccionar(manana, manana.plusDays(1));
		titular.setText("");
		tarjeta.setText("");
		caducidad.setText("");
		cvv.setText("");
		error.setText(" ");
		restaurarBotonConfirmar();

		actualizarResumen();
		SwingUtilities.invokeLater(() -> tarjeta.getInput().requestFocus());
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(760, 780);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout(Space.insets(Space.GIANT), "[grow]", "[grow]"));
		exterior.setOpaque(false);

		// El ancho ya no es Layout.FORMULARIO (440): el calendario de dos meses
		// necesita más sitio que un formulario de texto, y esa es precisamente la
		// excepción que el propio sistema de diseño prevé para rejillas ("las
		// rejillas sí crecen"). El campo de tarjeta, que sí es texto, mantiene su
		// ancho cómodo de lectura por su cuenta, en formulario().
		exterior.add(formulario(), Layout.ancho(CalendarioRango.ANCHO_PREFERIDO) + ", aligny center, alignx center");

		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volver);
	}

	private JPanel formulario() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XXL + "[]" + Space.LG + "[]" + Space.LG + "[]" + Space.SM + "[]" + Space.XL + "[]"
						+ Space.SM + "[]" + Space.XL + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());
		panel.add(campoFechas());
		panel.add(campoPago());

		resumen = Labels.body(" ");
		panel.add(resumen);

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

		return panel;
	}

	/**
	 * Los datos de la tarjeta (F13): titular, número, caducidad y CVV.
	 *
	 * <p>
	 * <b>Solo el número viaja al servicio.</b> Caducidad y CVV se validan aquí
	 * mismo, en la pantalla, y no en ningún sitio más — ni se guardan ni se
	 * envían. Es la regla de cualquier pasarela real llevada a sus últimas
	 * consecuencias: el CVV en particular no debería persistir en ningún sitio,
	 * ni siquiera en una simulación, así que la forma más honesta de "no
	 * guardarlo nunca" es no dejar que salga de este formulario.
	 */
	private JPanel campoPago() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XS + "[]" + Space.SM + "[]" + Space.SM + "[]"));
		panel.setOpaque(false);

		etiquetaPago = Labels.caps(Textos.t("reservar.pago.label"));
		panel.add(etiquetaPago);

		// Ancho de formulario y no el del calendario: son campos de texto cortos, y
		// estirarlos al ancho de una rejilla de dos meses los haría incómodos de leer.
		titular = Field.text(Textos.t("reservar.titular"));
		panel.add(titular, Layout.ancho(Layout.FORMULARIO));

		tarjeta = Field.text(Textos.t("reservar.tarjeta"));
		panel.add(tarjeta, Layout.ancho(Layout.FORMULARIO));

		JPanel caducidadYCvv = new JPanel(
				new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[grow,fill]", ""));
		caducidadYCvv.setOpaque(false);

		caducidad = Field.text(Textos.t("reservar.caducidad"));
		caducidadYCvv.add(caducidad);

		cvv = Field.text(Textos.t("reservar.cvv"));
		caducidadYCvv.add(cvv);

		panel.add(caducidadYCvv, Layout.ancho(Layout.FORMULARIO));

		return panel;
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		superTitulo = Labels.capsAccent(Textos.t("reservar.titulo"));
		titulos.add(superTitulo);

		tituloAlojamiento = Labels.title(" ");
		titulos.add(tituloAlojamiento, "gaptop " + Space.XXS);

		subtituloUbicacion = Labels.muted(" ");
		titulos.add(subtituloUbicacion, "gaptop " + Space.XXS);

		panel.add(titulos);
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	private JPanel campoFechas() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		etiquetaFechas = Labels.caps(Textos.t("reservar.entradaYSalida"));
		panel.add(etiquetaFechas);

		calendario = new CalendarioRango(this::actualizarResumen);
		panel.add(calendario, "gaptop " + Space.XS);

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		botonConfirmar = Buttons.primary(Textos.t("reservar.confirmar"), e -> reservar());
		fila.add(botonConfirmar, "height 44!");
		fila.add(enlaceCancelar());

		return fila;
	}

	private JLabel enlaceCancelar() {

		JLabel enlace = Labels.body(Textos.t("reservar.cancelar"));
		enlace.setFont(Typography.sansSemiBold(Typography.BODY_SM));
		enlace.setForeground(Theme.mut());
		enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		enlace.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				volver();
			}
		});

		enlaceCancelar = enlace;
		return enlace;
	}

	private void volver() {
		navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing));
	}

	/**
	 * Recalcula "precio × noches = total" con la selección actual del calendario.
	 *
	 * <p>
	 * Se llama en cada cambio de la selección. No valida nada —esa es
	 * responsabilidad del servicio al confirmar—, solo informa: mientras falte la
	 * fecha de salida, o si por lo que sea el rango no llega a una noche, lo dice
	 * en vez de enseñar un total de 0,00 €, que parecería un error de cálculo en
	 * lugar de una fecha por elegir.
	 */
	private void actualizarResumen() {

		if (housing == null || calendario == null) {
			return;
		}

		LocalDate entrada = calendario.getInicio();
		LocalDate salida = calendario.getFin();

		if (entrada == null || salida == null) {
			resumen.setText(Textos.t("reservar.resumen.eligeFechas"));
			return;
		}

		long noches = ChronoUnit.DAYS.between(entrada, salida);

		if (noches <= 0) {
			resumen.setText(Textos.t("reservar.resumen.salidaInvalida"));
			return;
		}

		BigDecimal total = housing.getPricePerNight().multiply(BigDecimal.valueOf(noches));

		resumen.setText(Textos.t("reservar.resumen.formula", Formato.precio(housing.getPricePerNight()),
				Formato.plural((int) noches, Textos.t("palabra.noche.singular"), Textos.t("palabra.noche.plural")),
				Formato.precio(total)));
	}

	private void reservar() {

		LocalDate entrada = calendario.getInicio();
		LocalDate salida = calendario.getFin();

		if (entrada == null || salida == null) {
			error.setText(Textos.t("reservar.resumen.eligeFechas"));
			return;
		}

		if (!validarPago()) {
			return;
		}

		error.setText(" ");
		procesarPago(entrada, salida);
	}

	/**
	 * Titular, caducidad y CVV: lo que la propia pantalla puede comprobar sin
	 * llamar al servicio, porque ninguno de los tres viaja hasta él (ver la nota
	 * de {@link #campoPago()}). El número de tarjeta se queda fuera de aquí a
	 * propósito — su validación la impone el servicio
	 * ({@link WrongCreditCardNumberException}), no la pantalla, siguiendo la
	 * misma regla del resto de la aplicación: una conversión de formato se
	 * valida en la pantalla, una regla de negocio la valida el servicio.
	 */
	private boolean validarPago() {

		if (titular.getText().trim().isEmpty()) {
			error.setText(Textos.t("reservar.error.titularVacio"));
			titular.requestFocus();
			return false;
		}

		String textoCaducidad = caducidad.getText().trim();

		if (!textoCaducidad.matches("(0[1-9]|1[0-2])/\\d{2}")) {
			error.setText(Textos.t("reservar.error.caducidadInvalida"));
			caducidad.requestFocus();
			return false;
		}

		if (!caducidadVigente(textoCaducidad)) {
			error.setText(Textos.t("reservar.error.tarjetaCaducada"));
			caducidad.requestFocus();
			return false;
		}

		if (!cvv.getText().trim().matches("\\d{3}")) {
			error.setText(Textos.t("reservar.error.cvvInvalido"));
			cvv.requestFocus();
			return false;
		}

		return true;
	}

	/** La tarjeta caduca al final del mes indicado, no al principio: "07/29" sigue siendo válida durante todo julio de 2029. */
	private boolean caducidadVigente(String mmAA) {

		int mes = Integer.parseInt(mmAA.substring(0, 2));
		int anio = 2000 + Integer.parseInt(mmAA.substring(3));

		LocalDate finDelMes = LocalDate.of(anio, mes, 1).plusMonths(1).minusDays(1);

		return !finDelMes.isBefore(LocalDate.now());
	}

	/**
	 * Simula el paso por la pasarela (F13) antes de tocar el servicio.
	 *
	 * <p>
	 * <b>Por qué un {@code Timer} y no una llamada directa.</b> Confirmar y
	 * reservar en el mismo clic, sin ninguna señal intermedia, no se distingue
	 * de rellenar un formulario cualquiera — es la misma fricción cero que tiene
	 * "Guardar cambios" en Ajustes. El dinero pide una pausa deliberada, aunque
	 * sea corta y aunque no haya ningún banco al otro lado: es lo que hace que
	 * el usuario perciba que <em>algo</em> ha pasado con su tarjeta antes de
	 * confirmar la reserva.
	 */
	private void procesarPago(LocalDate entrada, LocalDate salida) {

		botonConfirmar.setEnabled(false);
		botonConfirmar.setText(Textos.t("reservar.procesando"));

		Timer temporizador = new Timer(DURACION_PAGO_SIMULADO_MS, e -> confirmarReserva(entrada, salida));
		temporizador.setRepeats(false);
		temporizador.start();
	}

	private void confirmarReserva(LocalDate entrada, LocalDate salida) {

		LocalDateTime checkIn = entrada.atStartOfDay();
		LocalDateTime checkOut = salida.atStartOfDay();
		String numeroTarjeta = tarjeta.getText().trim();

		try {
			Reservation reserva = reservationService.reserveHousing(sessionManager.getLoggedInUser().getId(),
					housingId, numeroTarjeta, checkIn, checkOut);

			navigator.ir(ShowMyReservationsFrame.class);
			Toast.mostrar(navigator.ventanaVisible(),
					Textos.t("reservar.pago.confirmado", Formato.precio(reserva.getTotalPrice())));

		} catch (WrongCreditCardNumberException ex) {
			restaurarBotonConfirmar();
			error.setText(Textos.t("reservar.error.tarjetaInvalida"));
			tarjeta.requestFocus();

		} catch (MustBeTodayOrAfterException ex) {
			restaurarBotonConfirmar();
			error.setText(Textos.t("reservar.error.entradaPasada"));

		} catch (CheckOutMustBeOneDayAfterException ex) {
			restaurarBotonConfirmar();
			error.setText(Textos.t("reservar.resumen.salidaInvalida"));

		} catch (AlreadyReservedException ex) {
			restaurarBotonConfirmar();
			error.setText(Textos.t("reservar.error.disponibilidadPerdida"));

		} catch (InstanceNotFoundException | NotAuthorizedUserException ex) {
			// InstanceNotFoundException no debería darse: se llega aquí siempre desde un
			// alojamiento que se acaba de cargar. NotAuthorizedUserException tampoco: solo
			// un CUSTOMER ve el botón "Reservar". Cubrirlas igual evita una pantalla muda
			// si algún día cambia esa garantía.
			restaurarBotonConfirmar();
			error.setText(Textos.t("reservar.error.generico"));
		}
	}

	private void restaurarBotonConfirmar() {

		botonConfirmar.setEnabled(true);
		botonConfirmar.setText(Textos.t("reservar.confirmar"));
	}
}
