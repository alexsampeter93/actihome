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

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
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
	private Field tarjeta;
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
		tarjeta.setEtiqueta(Textos.t("reservar.tarjeta"));
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
		tarjeta.setText("");
		error.setText(" ");

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
				"[]" + Space.XXL + "[]" + Space.LG + "[]" + Space.XL + "[]" + Space.SM + "[]" + Space.XL + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());
		panel.add(campoFechas());

		// A diferencia del calendario, esto sí es texto: mantiene el ancho cómodo de
		// lectura de un formulario aunque el panel que lo contiene sea más ancho.
		tarjeta = Field.text(Textos.t("reservar.tarjeta"));
		panel.add(tarjeta, Layout.ancho(Layout.FORMULARIO));

		resumen = Labels.body(" ");
		panel.add(resumen);

		error = Labels.error(" ");
		panel.add(error);

		panel.add(acciones());

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

		LocalDateTime checkIn = entrada.atStartOfDay();
		LocalDateTime checkOut = salida.atStartOfDay();
		String numeroTarjeta = tarjeta.getText().trim();

		try {
			reservationService.reserveHousing(sessionManager.getLoggedInUser().getId(), housingId, numeroTarjeta,
					checkIn, checkOut);

			navigator.ir(ShowMyReservationsFrame.class);

		} catch (WrongCreditCardNumberException ex) {
			error.setText(Textos.t("reservar.error.tarjetaInvalida"));
			tarjeta.requestFocus();

		} catch (MustBeTodayOrAfterException ex) {
			error.setText(Textos.t("reservar.error.entradaPasada"));

		} catch (CheckOutMustBeOneDayAfterException ex) {
			error.setText(Textos.t("reservar.resumen.salidaInvalida"));

		} catch (AlreadyReservedException ex) {
			error.setText(Textos.t("reservar.error.disponibilidadPerdida"));

		} catch (InstanceNotFoundException | NotAuthorizedUserException ex) {
			// InstanceNotFoundException no debería darse: se llega aquí siempre desde un
			// alojamiento que se acaba de cargar. NotAuthorizedUserException tampoco: solo
			// un CUSTOMER ve el botón "Reservar". Cubrirlas igual evita una pantalla muda
			// si algún día cambia esa garantía.
			error.setText(Textos.t("reservar.error.generico"));
		}
	}
}
