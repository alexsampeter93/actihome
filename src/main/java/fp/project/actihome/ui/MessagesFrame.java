package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JComponent;
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

import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.ConversationSummary;
import fp.project.actihome.model.services.MessageService;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.components.Rescate;

/**
 * Bandeja de entrada (F10): una fila por cada persona con la que hay
 * mensajes, agrupados por alojamiento, con el más reciente primero.
 *
 * <p>
 * Mismo reparto que {@link ShowMyReservationsFrame}: cabecera fija, lista con
 * scroll ocupando el resto. Cada fila lleva un {@link Chip} con el número de
 * mensajes sin leer cuando hay alguno — el mismo componente que ya usan los
 * filtros del catálogo, aquí como insignia en vez de como conmutador.
 */
@Component
@Profile("!test")
@Lazy
public class MessagesFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Caracteres del extracto del último mensaje. Una línea corta basta para reconocer la conversación. */
	private static final int LIMITE_EXTRACTO = 90;

	private final transient MessageService messageService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JLabel titulo;
	private JLabel error;
	private JPanel lista;
	private JScrollPane scroll;

	public MessagesFrame(MessageService messageService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.messageService = messageService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			titulo.setText(Textos.t("header.nav.mensajes"));
			cargar();
			volverArriba();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1000, 780);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(MessagesFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[grow,fill]"));

		raiz.add(headerPanel, "growx");
		raiz.add(titular(), "growx");
		raiz.add(zonaDeLista(), "grow");

		setContentPane(raiz);
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XL, Space.HUGE, Space.LG, Space.HUGE),
				"[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		titulo = Labels.title(" ");
		panel.add(titulo);

		error = Labels.error(" ");
		panel.add(error);

		return panel;
	}

	private JScrollPane zonaDeLista() {

		lista = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE), "[grow,fill]", "[]"));
		lista.setOpaque(false);

		// Rescate y no un JScrollPane crudo. La diferencia esta en el Scrollable que
		// Rescate envuelve: sin el, el contenido conserva su ancho preferido en vez de
		// seguir el del visor, y con la barra horizontal desactivada lo que se sale por
		// la derecha NO SE PUEDE ALCANZAR NUNCA, por mucho que se agrande la ventana.
		scroll = Rescate.envolver(lista);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		scroll.setMinimumSize(new Dimension(0, 0));

		return scroll;
	}

	private void volverArriba() {

		if (scroll != null) {
			SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
		}
	}

	private void cargar() {

		lista.removeAll();
		error.setText(" ");

		ArrayList<ConversationSummary> conversaciones;

		try {
			conversaciones = messageService.showConversations(sessionManager.getLoggedInUser().getId());

		} catch (InstanceNotFoundException ex) {
			navigator.ir(LoginFrame.class);
			return;
		}

		if (conversaciones.isEmpty()) {
			lista.add(estadoVacio(), "growx");

		} else {
			boolean primera = true;

			for (ConversationSummary resumen : conversaciones) {

				if (!primera) {
					lista.add(Hairline.horizontal(), "growx, h 1!");
				}

				lista.add(fila(resumen), "growx");
				primera = false;
			}
		}

		lista.revalidate();
		lista.repaint();
	}

	private JPanel fila(ConversationSummary resumen) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.MD, 0, Space.MD, 0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JPanel identidad = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.XXS + "[]"));
		identidad.setOpaque(false);

		JPanel encabezado = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", ""));
		encabezado.setOpaque(false);
		encabezado.add(Labels.cardTitle(resumen.getHousing().getName()));

		if (resumen.getUnreadCount() > 0) {
			encabezado.add(Chip.informativo(String.valueOf(resumen.getUnreadCount())));
		}

		identidad.add(encabezado);
		identidad.add(Labels.muted(Textos.t("mensajes.con", resumen.getOtherUser().getUsername()) + " · "
				+ extracto(resumen.getLastMessage().getBody())));

		JLabel fecha = Labels.muted(formatoFecha().format(resumen.getLastMessage().getSentDate()));

		panel.add(identidad);
		panel.add(fecha, "aligny top");

		panel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(ConversationFrame.class,
						frame -> frame.setConversacion(resumen.getOtherUser(), resumen.getHousing()));
			}
		});

		return panel;
	}

	/** Mismo patrón que {@code ReviewRow.formatoFecha()}: el idioma decide el patrón, no solo el {@link Locale}. */
	private static DateTimeFormatter formatoFecha() {

		return Textos.idioma().getLanguage().equals("en")
				? DateTimeFormatter.ofPattern("MMM d, yyyy", Textos.idioma())
				: DateTimeFormatter.ofPattern("d MMM yyyy", Textos.idioma());
	}

	private static String extracto(String cuerpo) {

		if (cuerpo == null) {
			return "";
		}

		String limpio = cuerpo.trim().replace('\n', ' ');

		if (limpio.length() <= LIMITE_EXTRACTO) {
			return limpio;
		}

		int corte = limpio.lastIndexOf(' ', LIMITE_EXTRACTO);

		return limpio.substring(0, corte < 0 ? LIMITE_EXTRACTO : corte) + "…";
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.HUGE, 0, Space.HUGE, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		panel.add(centrar(Labels.title(Textos.t("mensajes.vacio.titulo"))));
		panel.add(centrar(Labels.muted(Textos.t("mensajes.vacio.cuerpo"))));

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}
}
