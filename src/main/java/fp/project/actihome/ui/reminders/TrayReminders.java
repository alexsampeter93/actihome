package fp.project.actihome.ui.reminders;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Timer;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.Textos;

/**
 * Recordatorios en la bandeja del sistema (F11): check-in próximo, check-in
 * ya disponible.
 *
 * <p>
 * <b>Por qué aquí y no como parte de ninguna pantalla.</b> Un aviso de
 * bandeja tiene sentido precisamente cuando la aplicación <em>no</em> tiene el
 * foco — es lo que la distingue de un {@code Toast}, que solo se ve si la
 * ventana está delante—. Por eso vive fuera del ciclo de vida de cualquier
 * {@code JFrame}: se arranca una sola vez desde
 * {@code ActihomeApplication.main}, con un temporizador propio que sigue
 * latiendo esté la aplicación minimizada o no.
 *
 * <p>
 * <b>Sondea la sesión en vez de que el login le avise.</b> {@code SessionManager}
 * no tiene ningún mecanismo de observador —a diferencia de {@code Theme}, que sí
 * lo necesita porque decenas de componentes reaccionan a un cambio de
 * estación—, y añadírselo solo para esta pieza habría significado tocar el
 * único punto de estado de sesión de toda la aplicación por una funcionalidad
 * que puede vivir perfectamente comprobando cada pocos minutos si hay alguien
 * conectado. Es la misma idea que ya usa {@code HeaderPanel.refresh()}: mirar
 * el estado real en vez de que alguien se acuerde de avisar.
 *
 * <p>
 * <b>Se apaga sola si el entorno no tiene bandeja de verdad.</b>
 * {@link SystemTray#isSupported()} no es una comprobación de relleno: en
 * ciertos entornos de desarrollo o sin escritorio, no hay ninguna bandeja a
 * la que añadir un icono, y sin esta guarda {@code SystemTray.getSystemTray()}
 * lanzaría una excepción que tumbaría el arranque entero por una
 * funcionalidad secundaria.
 */
@Component
@Profile("!test")
@Lazy
public class TrayReminders {

	/** Cada cuánto se revisa. No hace falta más frecuencia: un aviso con cinco minutos de margen sigue siendo útil. */
	private static final int INTERVALO_MS = 5 * 60 * 1000;

	private final ReservationService reservationService;
	private final SessionManager sessionManager;

	private TrayIcon icono;
	private Long usuarioDeLosAvisos;
	private final Set<Long> checkInProximoAvisado = new HashSet<>();
	private final Set<Long> listaParaCheckInAvisado = new HashSet<>();

	public TrayReminders(ReservationService reservationService, SessionManager sessionManager) {
		this.reservationService = reservationService;
		this.sessionManager = sessionManager;
	}

	/** La llama {@code ActihomeApplication.main} una sola vez, tras abrir la ventana de login. */
	public void iniciar() {

		if (!SystemTray.isSupported()) {
			return;
		}

		BufferedImage imagen = BrandAssets.iconoDeBandeja();

		if (imagen == null) {
			return;
		}

		icono = new TrayIcon(imagen, "ActiHome");
		icono.setImageAutoSize(true);

		Timer temporizador = new Timer(INTERVALO_MS, e -> revisar());
		temporizador.setRepeats(true);
		temporizador.start();

		// Primera revisión inmediata: quien inicia sesión no debería esperar cinco
		// minutos para enterarse de un check-in que ya toca.
		revisar();
	}

	private void revisar() {

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			quitarIcono();
			olvidarAvisosPrevios();
			return;
		}

		if (!usuario.getId().equals(usuarioDeLosAvisos)) {
			// Cambio de cuenta (cerrar sesión y entrar con otra): los avisos ya dados no
			// pertenecen a esta cuenta.
			olvidarAvisosPrevios();
			usuarioDeLosAvisos = usuario.getId();
		}

		ponerIcono();

		List<Reservation> reservas;

		try {
			reservas = reservationService.showMyReservations(usuario.getId());

		} catch (InstanceNotFoundException ex) {
			return;
		}

		LocalDateTime ahora = LocalDateTime.now();

		for (Reservation reserva : reservas) {

			if (ReminderEvaluator.esCheckInProximo(reserva, ahora) && checkInProximoAvisado.add(reserva.getId())) {
				avisar(Textos.t("recordatorios.checkInProximo.titulo"),
						Textos.t("recordatorios.checkInProximo.cuerpo", reserva.getHousing().getName()));
			}

			if (ReminderEvaluator.esListaParaCheckIn(reserva, ahora) && listaParaCheckInAvisado.add(reserva.getId())) {
				avisar(Textos.t("recordatorios.listaParaCheckIn.titulo"),
						Textos.t("recordatorios.listaParaCheckIn.cuerpo", reserva.getHousing().getName()));
			}
		}
	}

	private void olvidarAvisosPrevios() {

		checkInProximoAvisado.clear();
		listaParaCheckInAvisado.clear();
		usuarioDeLosAvisos = null;
	}

	private void ponerIcono() {

		if (icono == null) {
			return;
		}

		SystemTray bandeja = SystemTray.getSystemTray();

		if (Arrays.asList(bandeja.getTrayIcons()).contains(icono)) {
			return;
		}

		try {
			bandeja.add(icono);

		} catch (AWTException ex) {
			// Sin bandeja de verdad no hay dónde avisar. Los recordatorios se silencian
			// sin que el resto de la aplicación se entere.
			icono = null;
		}
	}

	private void quitarIcono() {

		if (icono == null) {
			return;
		}

		SystemTray.getSystemTray().remove(icono);
	}

	private void avisar(String titulo, String cuerpo) {

		if (icono != null) {
			icono.displayMessage(titulo, cuerpo, TrayIcon.MessageType.INFO);
		}
	}
}
