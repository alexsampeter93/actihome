package fp.project.actihome.ui;

import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Reservation;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReservationService;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Stat;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;

/**
 * Panel de administración global (F14): ocupación e ingresos de <b>toda</b>
 * la plataforma, agrupados por propietario.
 *
 * <p>
 * <b>Distinto de {@link OwnerPanelFrame} (Fase 7.9), no una variante suya.</b>
 * Aquel filtra por el propietario que ha iniciado sesión —lo que ese ADMIN
 * necesita para llevar sus propios alojamientos—; este no filtra nada, es la
 * vista que un rol de plataforma esperaría tener: cuántos alojamientos hay en
 * total, cuántos propietarios los publican, cuánto mueve la plataforma
 * entera. Comparten vocabulario visual —los mismos {@link Stat}, la misma
 * fila con separador— pero no comparten agregación: uno filtra, el otro
 * suma.
 *
 * <p>
 * <b>Sin ningún método de servicio nuevo, mismo criterio que {@link OwnerPanelFrame}.</b>
 * {@code housingService.showHousings()} ya da los alojamientos de todos los
 * propietarios sin filtrar por ninguno, y una llamada a
 * {@code reservationService.showHousingReservations} por alojamiento (N+1) es
 * el mismo trato ya aceptado para el catálogo actual. Las reservas canceladas
 * no cuentan, mismo criterio que en {@code OwnerPanelFrame} y en las reseñas.
 *
 * <p>
 * <b>Accesible a cualquier ADMIN, no a un "superadmin" aparte.</b> El modelo
 * de roles de la aplicación solo distingue ADMIN de CUSTOMER —ver la nota de
 * {@code UserService.changeRole} sobre esta misma simplificación—, así que no
 * hay hoy ninguna forma de reservar esta vista a un subconjunto de
 * administradores sin inventar un tercer rol. Se deja anotado como
 * simplificación consciente, no como descuido.
 */
@Component
@Profile("!test")
@Lazy
public class PlatformPanelFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient HousingService housingService;
	private final transient ReservationService reservationService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JLabel superTitulo;
	private JLabel titulo;
	private Stat statAlojamientos;
	private Stat statPropietarios;
	private Stat statReservas;
	private Stat statIngresos;
	private JPanel lista;
	private JScrollPane scroll;

	public PlatformPanelFrame(HousingService housingService, ReservationService reservationService,
			SessionManager sessionManager, Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.reservationService = reservationService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextosFijos();
			cargar();
			volverArriba();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1000, 780);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(
				new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[]0[grow,fill]"));

		raiz.add(headerPanel, "growx");
		raiz.add(titular(), "growx");
		raiz.add(resumen(), "growx");
		raiz.add(zonaDeLista(), "grow");

		setContentPane(raiz);
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XL, Space.HUGE, Space.MD, Space.HUGE),
				"[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		superTitulo = Labels.capsAccent(" ");
		panel.add(superTitulo);

		titulo = Labels.title(" ");
		panel.add(titulo);

		return panel;
	}

	private JPanel resumen() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0, Space.HUGE, Space.LG, Space.HUGE),
				"[grow,fill]" + Space.XL + "[grow,fill]" + Space.XL + "[grow,fill]" + Space.XL + "[grow,fill]", ""));
		panel.setOpaque(false);

		statAlojamientos = new Stat("0", " ");
		statPropietarios = new Stat("0", " ");
		statReservas = new Stat("0", " ");
		statIngresos = new Stat(Formato.precio(BigDecimal.ZERO), " ", true);

		panel.add(statAlojamientos);
		panel.add(statPropietarios);
		panel.add(statReservas);
		panel.add(statIngresos);

		return panel;
	}

	private JScrollPane zonaDeLista() {

		lista = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE), "[grow,fill]", "[]"));
		lista.setOpaque(false);

		scroll = new JScrollPane(lista);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(24);

		// Mínimo cero: si la ventana se queda corta, cede la lista, no la cabecera ni
		// el resumen. Mismo motivo que ya documentan ShowHousingsFrame y OwnerPanelFrame.
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("panelPlataforma.superTitulo"));
		titulo.setText(Textos.t("panelPlataforma.titulo"));
		statAlojamientos.setRotulo(Textos.t("panelPlataforma.resumen.alojamientos"));
		statPropietarios.setRotulo(Textos.t("panelPlataforma.resumen.propietarios"));
		statReservas.setRotulo(Textos.t("panelPlataforma.resumen.reservas"));
		statIngresos.setRotulo(Textos.t("panelPlataforma.resumen.ingresos"));
	}

	private void volverArriba() {

		if (scroll != null) {
			SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
		}
	}

	private void cargar() {

		User actual = sessionManager.getLoggedInUser();

		if (actual == null) {
			navigator.ir(LoginFrame.class);
			return;
		}

		ArrayList<Housing> todos = housingService.showHousings();

		// LinkedHashMap para que, con ingresos empatados a cero (una plataforma
		// recién sembrada), el orden no dependa de un hash y sea el mismo en cada
		// arranque: primero se ve a quien primero apareció en el catálogo.
		Map<Long, ResumenPropietario> porPropietario = new LinkedHashMap<>();

		for (Housing housing : todos) {

			ResumenPropietario resumen = porPropietario.computeIfAbsent(housing.getOwner().getId(),
					id -> new ResumenPropietario(housing.getOwner()));

			resumen.alojamientos++;

			for (Reservation reserva : reservationService.showHousingReservations(housing.getId())) {

				if (!reserva.isCancelled()) {
					resumen.reservas++;
					resumen.ingresos = resumen.ingresos.add(reserva.getTotalPrice());
				}
			}
		}

		ArrayList<ResumenPropietario> ordenados = new ArrayList<>(porPropietario.values());
		ordenados.sort(Comparator.comparing((ResumenPropietario r) -> r.ingresos).reversed());

		int reservasTotales = ordenados.stream().mapToInt(r -> r.reservas).sum();
		BigDecimal ingresosTotales = ordenados.stream().map(r -> r.ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);

		lista.removeAll();

		if (ordenados.isEmpty()) {
			lista.add(estadoVacio(), "growx");

		} else {
			boolean primera = true;

			for (ResumenPropietario resumenDeUno : ordenados) {

				if (!primera) {
					lista.add(Hairline.horizontal(), "growx, h 1!");
				}

				lista.add(fila(resumenDeUno), "growx");
				primera = false;
			}
		}

		statAlojamientos.setValor(String.valueOf(todos.size()));
		statPropietarios.setValor(String.valueOf(ordenados.size()));
		statReservas.setValor(String.valueOf(reservasTotales));
		statIngresos.setValor(Formato.precio(ingresosTotales));

		lista.revalidate();
		lista.repaint();
	}

	private JPanel fila(ResumenPropietario resumen) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.MD, 0, Space.MD, 0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel identidad = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		identidad.setOpaque(false);
		identidad.add(Labels.cardTitle(resumen.propietario.getUsername()));
		identidad.add(Labels.muted(Formato.plural(resumen.alojamientos, Textos.t("palabra.alojamiento.singular"),
				Textos.t("palabra.alojamiento.plural"))));

		JPanel cifras = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		cifras.setOpaque(false);
		cifras.add(Labels.price(Formato.precio(resumen.ingresos)));
		cifras.add(Labels.muted(Formato.plural(resumen.reservas, Textos.t("palabra.reserva.singular"),
				Textos.t("palabra.reserva.plural"))));

		panel.add(identidad);
		panel.add(cifras, "aligny center");

		return panel;
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL, 0, Space.XXL, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		panel.add(centrar(Labels.title(Textos.t("panelPlataforma.vacio.titulo"))));
		panel.add(centrar(Labels.muted(Textos.t("panelPlataforma.vacio.cuerpo"))));

		return panel;
	}

	private JPanel centrar(java.awt.Component componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);

		return fila;
	}

	/** Acumulador de un propietario mientras se recorren todos los alojamientos. */
	private static final class ResumenPropietario {

		private final User propietario;
		private int alojamientos;
		private int reservas;
		private BigDecimal ingresos = BigDecimal.ZERO;

		ResumenPropietario(User propietario) {
			this.propietario = propietario;
		}
	}
}
