package fp.project.actihome.ui;

import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.Message;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.CannotMessageSelfException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.MessageService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;
import fp.project.actihome.ui.components.Rescate;

/**
 * Una conversación (F10): todos los mensajes entre dos personas sobre un
 * alojamiento, del más antiguo arriba al más reciente abajo, con un campo
 * para responder.
 *
 * <p>
 * <b>Se llega con el otro participante y el alojamiento ya en mano</b>, no
 * con sus ids sueltos: tanto {@code HousingDetailsFrame} ("Preguntar al
 * propietario") como {@link MessagesFrame} ya tienen el {@link User} y el
 * {@link Housing} completos en el momento de navegar, así que no hace falta
 * ningún servicio nuevo solo para volver a resolver un nombre. Es una
 * excepción consciente al patrón de "guarda solo el id y recarga" que usan
 * {@code ReviewDetailsFrame} o {@code HousingDetailsFrame}: aquí no hay
 * ningún servicio que devuelva un {@code User} suelto por id —el más cercano,
 * {@code UserService.loginFromId}, existe para restaurar una sesión, no para
 * esto— y el dato mostrado (un nombre de usuario) no es de los que cambian a
 * media sesión.
 *
 * <p>
 * <b>La lista crece hacia abajo, no hacia arriba.</b> Es la única pantalla de
 * la aplicación donde el elemento más reciente importa más que el primero, así
 * que {@code volverAbajo()} hace justo lo contrario de lo que hace
 * {@code volverArriba()} en cualquier otro listado.
 */
@Component
@Profile("!test")
@Lazy
public class ConversationFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient MessageService messageService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private transient User otro;
	private transient Housing housing;

	private JLabel titulo;
	private JLabel subtitulo;
	private JPanel hilo;
	private JScrollPane scroll;
	private Field mensaje;
	private JButton botonEnviar;
	private JLabel error;

	public ConversationFrame(MessageService messageService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.messageService = messageService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/** Prepara con quién y sobre qué alojamiento. La llama el {@link Navigator}. */
	public void setConversacion(User otro, Housing housing) {
		this.otro = otro;
		this.housing = housing;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextosFijos();
			cargar();
			volverAbajo();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(800, 780);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(MessagesFrame.class);

		JPanel raiz = new Page(
				new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[]0[grow,fill]0[]"));

		raiz.add(headerPanel, "growx");
		raiz.add(titular(), "growx");
		raiz.add(zonaDeHilo(), "grow");
		raiz.add(compositor(), "growx");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volver);
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.LG, Space.HUGE, Space.MD, Space.HUGE),
				"[grow,fill]", "[]" + Space.XXS + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		// El enlace va dentro de su propio panel de columna natural, como la miga de
		// pan de ComparisonFrame y ReviewDetailsFrame. Añadido directamente aquí se
		// estiraba de lado a lado —la columna de este panel es "[grow,fill]"— y un
		// JButton estirado centra su texto: el "← Mensajes" aparecía flotando en mitad
		// de la pantalla, encima de un título alineado a la izquierda.
		JPanel migaDePan = new JPanel(new MigLayout(Space.insets(0), "[]", "[]"));
		migaDePan.setOpaque(false);
		migaDePan.add(Buttons.link(Textos.t("mensajes.volver"), e -> volver()));
		panel.add(migaDePan);

		titulo = Labels.title(" ");
		panel.add(titulo);

		subtitulo = Labels.muted(" ");
		panel.add(subtitulo);

		return panel;
	}

	private JScrollPane zonaDeHilo() {

		hilo = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(Space.SM, Space.HUGE, Space.LG, Space.HUGE), "[grow,fill]", "[]"));
		hilo.setOpaque(false);

		// Rescate y no un JScrollPane crudo. La diferencia esta en el Scrollable que
		// Rescate envuelve: sin el, el contenido conserva su ancho preferido en vez de
		// seguir el del visor, y con la barra horizontal desactivada lo que se sale por
		// la derecha NO SE PUEDE ALCANZAR NUNCA, por mucho que se agrande la ventana.
		scroll = Rescate.envolver(hilo);
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

	private JPanel compositor() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.MD, Space.HUGE, Space.XL, Space.HUGE),
				"[grow,fill]" + Space.MD + "[]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		mensaje = Field.textArea(Textos.t("mensajes.campo"), 2);
		panel.add(mensaje, "aligny top");

		// El botón se alinea ABAJO, no arriba. Un Field lleva su etiqueta encima de la
		// caja, así que "aligny top" lo pegaba al borde superior de la etiqueta y el
		// botón quedaba flotando por encima del recuadro de escritura. Abajo, los dos
		// bordes inferiores coinciden — y coinciden a cualquier escalado, porque no
		// depende de cuánto mida la etiqueta sino de dónde acaba la celda.
		botonEnviar = Buttons.primary(Textos.t("mensajes.enviar"), e -> enviar());
		panel.add(botonEnviar, "aligny bottom, height " + Typography.altoDeControl() + "!");

		error = Labels.error(" ");
		panel.add(error, "span 2, growx");

		return panel;
	}

	private void actualizarTextosFijos() {

		mensaje.setEtiqueta(Textos.t("mensajes.campo"));
		botonEnviar.setText(Textos.t("mensajes.enviar"));
	}

	private void volver() {
		navigator.ir(MessagesFrame.class);
	}

	private void volverAbajo() {

		if (scroll == null) {
			return;
		}

		SwingUtilities.invokeLater(() -> {
			JScrollBar barra = scroll.getVerticalScrollBar();
			barra.setValue(barra.getMaximum());
		});
	}

	private void cargar() {

		if (otro == null || housing == null) {
			return;
		}

		titulo.setText(Textos.t("mensajes.conversacion.con", otro.getUsername()));
		subtitulo.setText(housing.getName());
		error.setText(" ");

		ArrayList<Message> mensajes;

		try {
			mensajes = messageService.showConversation(sessionManager.getLoggedInUser().getId(), otro.getId(),
					housing.getId());

		} catch (InstanceNotFoundException ex) {
			navigator.ir(LoginFrame.class);
			return;
		}

		pintar(mensajes);
	}

	private void pintar(ArrayList<Message> mensajes) {

		hilo.removeAll();

		if (mensajes.isEmpty()) {
			hilo.add(Labels.muted(Textos.t("mensajes.conversacion.vacia")));

		} else {

			Long miId = sessionManager.getLoggedInUser().getId();
			boolean primera = true;

			for (Message m : mensajes) {
				hilo.add(filaMensaje(m, m.getSender().getId().equals(miId)), "growx, gaptop " + (primera ? 0 : Space.MD));
				primera = false;
			}
		}

		hilo.revalidate();
		hilo.repaint();
	}

	private JPanel filaMensaje(Message m, boolean esMio) {

		JPanel panel = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		JPanel encabezado = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", ""));
		encabezado.setOpaque(false);
		encabezado.add(esMio ? Labels.capsAccent(Textos.t("mensajes.tu")) : Labels.caps(m.getSender().getUsername()));
		encabezado.add(Labels.muted(formatoFechaHora().format(m.getSentDate())));

		panel.add(encabezado);
		panel.add(new WrappingText(m.getBody()));

		return panel;
	}

	private void enviar() {

		String texto = mensaje.getText().trim();

		if (texto.isEmpty()) {
			error.setText(Textos.t("mensajes.error.vacio"));
			return;
		}

		try {
			messageService.sendMessage(sessionManager.getLoggedInUser().getId(), otro.getId(), housing.getId(), texto);
			mensaje.setText("");
			cargar();
			volverAbajo();

		} catch (InstanceNotFoundException ex) {
			navigator.ir(LoginFrame.class);

		} catch (CannotMessageSelfException ex) {
			// No debería ocurrir: nunca se navega aquí con uno mismo como destinatario.
			error.setText(Textos.t("mensajes.error.generico"));
		}
	}

	/** Fecha y hora, no solo fecha: dentro de una misma conversación varios mensajes pueden compartir día. */
	private static DateTimeFormatter formatoFechaHora() {

		return Textos.idioma().getLanguage().equals("en")
				? DateTimeFormatter.ofPattern("MMM d, HH:mm", Textos.idioma())
				: DateTimeFormatter.ofPattern("d MMM, HH:mm", Textos.idioma());
	}
}
