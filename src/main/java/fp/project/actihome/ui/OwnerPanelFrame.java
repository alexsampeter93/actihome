package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
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
import fp.project.actihome.ui.components.Buttons;
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
 * Panel de propietario (Fase 7.9): sus alojamientos, con las reservas y los
 * ingresos de cada uno.
 *
 * <p>
 * <b>Sin ningún método de servicio nuevo.</b> Todo lo que hace falta ya
 * existía: {@code housingService.showHousings()} —filtrado aquí por
 * propietario, el mismo patrón en memoria que ya usa {@code CatalogFilters}
 * para el catálogo— y {@code reservationService.showHousingReservations},
 * que ya alimentaba el calendario de disponibilidad desde la Fase 7.5.2. Una
 * llamada por alojamiento (N+1) es el mismo trato que ya se da a las reseñas
 * en {@code ShowHousingsFrame.contarResenas}: con un catálogo de este tamaño,
 * no compensa la complejidad de una consulta agregada.
 *
 * <p>
 * Las reservas canceladas no cuentan ni para el recuento ni para los
 * ingresos: una reserva cancelada no llegó a ocurrir, el mismo criterio que
 * ya aplica {@code MustHaveStayedException} en las reseñas.
 */
@Component
@Profile("!test")
@Lazy
public class OwnerPanelFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient HousingService housingService;
	private final transient ReservationService reservationService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JLabel superTitulo;
	private JLabel titulo;
	private Stat statAlojamientos;
	private Stat statReservas;
	private Stat statIngresos;
	private JPanel resumen;
	private JPanel lista;
	private JScrollPane scroll;

	public OwnerPanelFrame(HousingService housingService, ReservationService reservationService,
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

	private JPanel titular(){

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

		resumen = new JPanel(new MigLayout(Space.insets(0, Space.HUGE, Space.LG, Space.HUGE),
				"[grow,fill]" + Space.XL + "[grow,fill]" + Space.XL + "[grow,fill]", ""));
		resumen.setOpaque(false);

		statAlojamientos = new Stat("0", " ");
		statReservas = new Stat("0", " ");
		statIngresos = new Stat(Formato.precio(BigDecimal.ZERO), " ", true);

		resumen.add(statAlojamientos);
		resumen.add(statReservas);
		resumen.add(statIngresos);

		return resumen;
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
		// el resumen. Mismo motivo que ya documentan ShowHousingsFrame y
		// ShowMyReservationsFrame.
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("panelPropietario.superTitulo"));
		titulo.setText(Textos.t("panelPropietario.titulo"));
		statAlojamientos.setRotulo(Textos.t("panelPropietario.resumen.alojamientos"));
		statReservas.setRotulo(Textos.t("panelPropietario.resumen.reservas"));
		statIngresos.setRotulo(Textos.t("panelPropietario.resumen.ingresos"));
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

		ArrayList<Housing> propios = new ArrayList<>();

		for (Housing housing : housingService.showHousings()) {
			if (housing.getOwner().getId().equals(actual.getId())) {
				propios.add(housing);
			}
		}

		int reservasTotales = 0;
		BigDecimal ingresosTotales = BigDecimal.ZERO;

		lista.removeAll();

		if (propios.isEmpty()) {
			lista.add(estadoVacio(), "growx");

		} else {
			boolean primera = true;

			for (Housing housing : propios) {

				ArrayList<Reservation> reservas = reservationService.showHousingReservations(housing.getId());

				int reservasDeEste = 0;
				BigDecimal ingresosDeEste = BigDecimal.ZERO;

				for (Reservation reserva : reservas) {
					if (!reserva.isCancelled()) {
						reservasDeEste++;
						ingresosDeEste = ingresosDeEste.add(reserva.getTotalPrice());
					}
				}

				reservasTotales += reservasDeEste;
				ingresosTotales = ingresosTotales.add(ingresosDeEste);

				if (!primera) {
					lista.add(Hairline.horizontal(), "growx, h 1!");
				}

				lista.add(fila(housing, reservasDeEste, ingresosDeEste), "growx");
				primera = false;
			}
		}

		statAlojamientos.setValor(String.valueOf(propios.size()));
		statReservas.setValor(String.valueOf(reservasTotales));
		statIngresos.setValor(Formato.precio(ingresosTotales));

		lista.revalidate();
		lista.repaint();
	}

	private JPanel fila(Housing housing, int reservas, BigDecimal ingresos) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.MD, 0, Space.MD, 0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JPanel identidad = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		identidad.setOpaque(false);
		identidad.add(Labels.cardTitle(housing.getName()));
		identidad.add(Labels.muted(Textos.t("catalogo.numero") + " " + housing.getHousingCode()));

		JPanel cifras = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		cifras.setOpaque(false);
		JLabel importe = Labels.price(Formato.precio(ingresos));
		cifras.add(importe);
		cifras.add(Labels.muted(Formato.plural(reservas, Textos.t("palabra.reserva.singular"),
				Textos.t("palabra.reserva.plural"))));

		panel.add(identidad);
		panel.add(cifras, "aligny center");

		panel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing));
			}
		});

		return panel;
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL, 0, Space.XXL, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]" + Space.XL + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		panel.add(centrar(Labels.title(Textos.t("panelPropietario.vacio.titulo"))));
		panel.add(centrar(Labels.muted(Textos.t("panelPropietario.vacio.cuerpo"))));
		panel.add(centrar(
				Buttons.primary(Textos.t("panelPropietario.vacio.boton"), e -> navigator.ir(UploadHousingFrame.class))));

		return panel;
	}

	private JPanel centrar(java.awt.Component componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);

		return fila;
	}
}
