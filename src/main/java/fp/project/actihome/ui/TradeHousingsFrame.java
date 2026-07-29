package fp.project.actihome.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Card;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.SwapGlyph;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Intercambiar la titularidad de dos alojamientos.
 *
 * <p>
 * Composición de dos paneles —el tuyo a la izquierda, el que recibes a la
 * derecha— con el glifo del intercambio en medio.
 *
 * <p>
 * <b>Por qué hay un paso de "buscar" antes de confirmar.</b> La versión anterior
 * era un único campo numérico y un botón: escribías un código a ciegas y
 * confirmabas sin haber visto nunca qué recibías a cambio. En una operación que
 * <b>permuta la propiedad de dos inmuebles</b> y no tiene deshacer, eso es
 * pedirle al usuario que firme sin leer. Buscar primero y enseñar el
 * alojamiento encontrado convierte el código en algo comprobable: si te has
 * equivocado de dígito, lo ves antes y no después.
 *
 * <p>
 * <b>La búsqueda no necesita ningún método nuevo del servicio.</b> El código de
 * alojamiento no es su identificador de base de datos, así que
 * {@code findHousing(id)} no vale; pero {@code showHousings()} ya devuelve todos
 * y encontrar el del código pedido es filtrar esa lista. Con seis alojamientos
 * de ejemplo es intrascendente, y añadir un método al servicio por una necesidad
 * de pantalla sería tocar la capa de negocio sin motivo. Si algún día el
 * catálogo creciera, el sitio de ese arreglo es el DAO —que ya tiene
 * {@code findByHousingCode}— y no esta pantalla.
 */
@Component
@Profile("!test")
@Lazy
public class TradeHousingsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient HousingService housingService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long housingId;
	private transient Housing propio;
	private transient Housing candidato;

	private JPanel panelPropio;
	private JPanel panelCandidato;
	private Field codigo;
	private JButton confirmar;
	private JLabel error;

	public TradeHousingsFrame(HousingService housingService, SessionManager sessionManager, Navigator navigator,
			HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/** Prepara qué alojamiento propio se ofrece. La llama el {@link Navigator}. */
	public void setHousingId(Long housingId) {
		this.housingId = housingId;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			recargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1040, 800);
		setMinimumSize(new Dimension(880, 680));
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL, Space.GIANT, Space.XXL, Space.GIANT), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.LG + "[]" + Space.XS + "[]" + Space.MD + "[]"));
		exterior.setOpaque(false);

		exterior.add(cabecera(), Layout.anchoCentrado(Layout.CONTENIDO));
		exterior.add(comparacion(), Layout.anchoCentrado(Layout.CONTENIDO));
		exterior.add(buscador(), Layout.anchoCentrado(Layout.TEXTO));

		error = Labels.error(" ");
		exterior.add(error, Layout.anchoCentrado(Layout.TEXTO));

		exterior.add(acciones(), Layout.anchoCentrado(Layout.TEXTO));

		raiz.add(headerPanel, "growx");
		raiz.add(exterior, "grow");

		setContentPane(raiz);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		panel.add(Labels.capsAccent("Intercambio"));
		panel.add(Labels.title("Cambia tu estancia por otra"));
		panel.add(Labels.muted(
				"Los dos alojamientos deben estar disponibles. Al confirmar, se permuta la titularidad de ambos."),
				"gaptop " + Space.XS);

		return panel;
	}

	/** Tu alojamiento · glifo · el que recibes. */
	private JPanel comparacion() {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(0), "[grow,fill]" + Space.XL + "[]" + Space.XL + "[grow,fill]", "[]"));
		panel.setOpaque(false);

		panelPropio = new JPanel();
		panelPropio.setOpaque(false);

		panelCandidato = new JPanel();
		panelCandidato.setOpaque(false);

		// "top" en los dos paneles: si no, MigLayout los centra verticalmente y, como
		// las dos tarjetas rara vez miden lo mismo, los rótulos "Entregas" y "Recibes"
		// quedaban a distinta altura y la comparación dejaba de leerse como tal.
		panel.add(panelPropio, "growx, aligny top");
		panel.add(discoDeIntercambio(), "w 44!, h 44!, aligny center");
		panel.add(panelCandidato, "growx, aligny top");

		return panel;
	}

	private JPanel buscador() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[]", "[]"));
		panel.setOpaque(false);

		codigo = Field.text("Código del alojamiento que quieres recibir");
		codigo.onEnter(this::buscar);

		panel.add(codigo);
		panel.add(Buttons.secondary("Buscar", e -> buscar()), "height 38!, gaptop 18");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		confirmar = Buttons.primary("Confirmar intercambio", e -> intercambiar());
		fila.add(confirmar, "height 44!");
		fila.add(Buttons.link("Cancelar", e -> volverAlDetalle()));

		return fila;
	}

	private void recargar() {

		if (housingId == null) {
			return;
		}

		try {
			propio = housingService.findHousing(housingId);

		} catch (InstanceNotFoundException ex) {
			navigator.ir(ShowHousingsFrame.class);
			return;
		}

		candidato = null;
		codigo.setText("");
		error.setText(" ");

		pintarPropio();
		pintarCandidato();
	}

	private void pintarPropio() {

		panelPropio.removeAll();
		panelPropio.setLayout(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panelPropio.add(Labels.caps("Entregas"), "gapbottom " + Space.XS);
		panelPropio.add(tarjeta(propio), "growx");
		panelPropio.revalidate();
		panelPropio.repaint();
	}

	private void pintarCandidato() {

		panelCandidato.removeAll();
		panelCandidato.setLayout(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panelCandidato.add(Labels.caps("Recibes"), "gapbottom " + Space.XS);

		if (candidato == null) {
			panelCandidato.add(tarjetaVacia(), "growx");
		} else {
			panelCandidato.add(tarjeta(candidato), "growx");
		}

		panelCandidato.revalidate();
		panelCandidato.repaint();

		// Sin alojamiento encontrado no hay nada que confirmar. Deshabilitar el botón
		// dice lo que falta mejor que dejarlo pulsable para luego dar un error.
		confirmar.setEnabled(candidato != null);
	}

	private Card tarjeta(Housing housing) {

		Card card = new Card(new MigLayout("wrap 1, " + Space.insets(Space.MD), "[grow,fill]", ""));

		card.add(new ImagePlaceholder(housing.getType(), housing.isAvailable() ? "Disponible" : "Reservada",
				housing.isAvailable(), housing.getImage()), "h 100!, growx, gapbottom " + Space.SM);

		card.add(Labels.capsAccent("Nº " + housing.getHousingCode()));

		JLabel nombre = Labels.cardTitle(housing.getName());
		nombre.setFont(Typography.serifMedium(Typography.CARD_TITLE));
		card.add(nombre, "gaptop " + Space.XXS);

		card.add(Labels.muted(housing.getLocation()), "gaptop " + Space.XXS);
		card.add(Labels.muted("Titular: " + housing.getOwner().getUsername()), "gaptop " + Space.XXS);
		card.add(Labels.priceSmall(Formato.precio(housing.getPricePerNight()) + " / noche"),
				"gaptop " + Space.XS);

		if (!housing.isAvailable()) {
			// Es la causa exacta por la que el servicio rechazaría el intercambio, dicha
			// antes de intentarlo.
			card.add(Labels.error("Está reservado: no se puede intercambiar."), "gaptop " + Space.XS);
		}

		return card;
	}

	private Card tarjetaVacia() {

		Card card = new Card(new MigLayout("wrap 1, " + Space.insets(Space.MD), "[grow,fill]", ""));

		card.add(new ImagePlaceholder(), "h 100!, growx, gapbottom " + Space.SM);
		card.add(Labels.muted("Escribe abajo el código del alojamiento que quieres"), "gaptop " + Space.XXS);
		card.add(Labels.muted("y pulsa Buscar para verlo aquí antes de confirmar."));

		return card;
	}

	/**
	 * Rellena el código y busca, como si el usuario lo hubiera tecleado y pulsado
	 * "Buscar".
	 *
	 * <p>
	 * Existe para {@code ScreenSnapshots}: el paso de buscar es lo que distingue a
	 * esta pantalla de la anterior, y una captura del estado inicial no lo
	 * enseñaría. Es el mismo criterio por el que {@code Segmented} expone
	 * {@code setActivo}: mejor un método honesto que diga "haz esto" que simular un
	 * clic sobre una coordenada.
	 */
	public void buscarPorCodigo(String codigoDeAlojamiento) {

		codigo.setText(codigoDeAlojamiento);
		buscar();
	}

	private void buscar() {

		String texto = codigo.getText().trim();

		if (texto.isEmpty()) {
			error.setText("Escribe un código de alojamiento.");
			return;
		}

		long buscado;

		try {
			buscado = Long.parseLong(texto);

		} catch (NumberFormatException ex) {
			error.setText("El código es un número, sin letras ni espacios.");
			return;
		}

		Optional<Housing> encontrado = housingService.showHousings().stream()
				.filter(h -> h.getHousingCode() != null && h.getHousingCode() == buscado).findFirst();

		if (!encontrado.isPresent()) {
			candidato = null;
			error.setText("No hay ningún alojamiento con el código " + buscado + ".");
			pintarCandidato();
			return;
		}

		if (encontrado.get().getId().equals(housingId)) {
			candidato = null;
			error.setText("Ese es el alojamiento que estás ofreciendo. Busca otro.");
			pintarCandidato();
			return;
		}

		candidato = encontrado.get();
		error.setText(" ");
		pintarCandidato();
	}

	private void intercambiar() {

		if (candidato == null) {
			return;
		}

		try {
			housingService.tradeHousings(sessionManager.getLoggedInUser().getId(), housingId,
					candidato.getHousingCode());

			navigator.ir(ShowHousingsFrame.class);

		} catch (AlreadyReservedException ex) {
			error.setText("Alguno de los dos alojamientos está reservado. Los dos deben estar disponibles.");

		} catch (InstanceNotFoundException ex) {
			error.setText("Uno de los alojamientos ya no existe.");
		}
	}

	private void volverAlDetalle() {
		navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(propio));
	}

	/**
	 * El símbolo del intercambio dentro de un disco de acento.
	 *
	 * <p>
	 * El disco se pinta aquí y el símbolo lo aporta {@link SwapGlyph}, que lo
	 * dibuja con geometría porque el carácter "⇄" no existe en las fuentes del
	 * proyecto y Swing lo sustituiría por un rectángulo vacío.
	 */
	private JComponent discoDeIntercambio() {

		JPanel disco = new JPanel(new MigLayout("fill, " + Space.insets(0), "[grow,fill]", "[grow,fill]")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {

				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int d = Math.min(getWidth(), getHeight());
				g2.setColor(Theme.acc());
				g2.fillOval(0, 0, d, d);

				g2.dispose();
				super.paintComponent(g);
			}
		};

		disco.setOpaque(false);
		disco.add(new SwapGlyph(24, true), "align center");

		return disco;
	}
}
