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
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Card;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.SwapGlyph;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
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

	private JPanel comparacion;
	private JPanel vacio;
	private JPanel buscador;
	private JPanel acciones;
	private JPanel panelPropio;
	private JPanel panelCandidato;
	private Field codigo;
	private JButton confirmar;
	private JLabel error;

	private JLabel superTitulo;
	private JLabel tituloCabecera;
	private JLabel subtituloCabecera;
	private JButton botonBuscar;
	private JButton botonCancelar;
	private JLabel vacioTitulo;
	private JLabel vacioCuerpo;
	private JButton vacioBoton;

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
			actualizarTextosFijos();
			recargar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1040, 800);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		// "hidemode 3": un componente invisible deja de contar del todo, ni tamaño ni
		// hueco. Sin esto, ocultar la comparación y el buscador para enseñar el estado
		// vacío dejaba sus filas reservadas y el mensaje de Olaz aparecía al fondo de
		// la pantalla, muy por debajo del titular.
		JPanel exterior = new JPanel(new MigLayout(
				"wrap 1, hidemode 3, " + Space.insets(Space.XXL, Space.GIANT, Space.XXL, Space.GIANT), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.LG + "[]" + Space.XS + "[]" + Space.MD + "[]"));
		exterior.setOpaque(false);

		exterior.add(cabecera(), Layout.anchoCentrado(Layout.CONTENIDO));

		comparacion = comparacion();
		exterior.add(comparacion, Layout.anchoCentrado(Layout.CONTENIDO));

		vacio = estadoVacio();
		exterior.add(vacio, Layout.anchoCentrado(Layout.CONTENIDO));

		buscador = buscador();
		exterior.add(buscador, Layout.anchoCentrado(Layout.TEXTO));

		error = Labels.error(" ");
		exterior.add(error, Layout.anchoCentrado(Layout.TEXTO));

		acciones = acciones();
		exterior.add(acciones, Layout.anchoCentrado(Layout.TEXTO));

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volverAlDetalle);
	}

	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		superTitulo = Labels.capsAccent(Textos.t("header.nav.intercambio"));
		panel.add(superTitulo);
		tituloCabecera = Labels.title(Textos.t("intercambio.titulo"));
		panel.add(tituloCabecera);
		subtituloCabecera = Labels.muted(Textos.t("intercambio.subtitulo"));
		panel.add(subtituloCabecera, "gaptop " + Space.XS);

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

		codigo = Field.text(Textos.t("intercambio.codigo"));
		codigo.onEnter(this::buscar);

		panel.add(codigo);
		botonBuscar = Buttons.secondary(Textos.t("intercambio.buscar"), e -> buscar());
		panel.add(botonBuscar, "height 38!, gaptop 18");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		confirmar = Buttons.primary(Textos.t("intercambio.confirmar"), e -> intercambiar());
		fila.add(confirmar, "height 44!");
		botonCancelar = Buttons.link(Textos.t("ajustes.cancelar"), e -> volverAlDetalle());
		fila.add(botonCancelar);

		return fila;
	}

	private void actualizarTextosFijos() {

		superTitulo.setText(Textos.t("header.nav.intercambio"));
		tituloCabecera.setText(Textos.t("intercambio.titulo"));
		subtituloCabecera.setText(Textos.t("intercambio.subtitulo"));
		codigo.setEtiqueta(Textos.t("intercambio.codigo"));
		botonBuscar.setText(Textos.t("intercambio.buscar"));
		confirmar.setText(Textos.t("intercambio.confirmar"));
		botonCancelar.setText(Textos.t("ajustes.cancelar"));
		vacioTitulo.setText(Textos.t("intercambio.vacio.titulo"));
		vacioCuerpo.setText(Textos.t("intercambio.vacio.cuerpo"));
		vacioBoton.setText(Textos.t("intercambio.vacio.boton"));
	}

	/**
	 * Prepara la pantalla.
	 *
	 * <p>
	 * <b>Se puede llegar aquí de dos maneras</b>, y por eso el alojamiento propio no
	 * siempre viene dado: desde la ficha de uno concreto —y entonces ese es el que
	 * se ofrece— o desde la barra de navegación, sin elegir nada. En el segundo caso
	 * se toma el primero de los tuyos, y si no tienes ninguno se enseña el estado
	 * vacío en lugar de una pantalla que no puede hacer nada.
	 */
	private void recargar() {

		propio = resolverAlojamientoPropio();

		candidato = null;
		codigo.setText("");
		error.setText(" ");

		reconstruir();
	}

	/** El alojamiento que se ofrece, o {@code null} si el usuario no tiene ninguno. */
	private Housing resolverAlojamientoPropio() {

		if (housingId != null) {

			try {
				return housingService.findHousing(housingId);

			} catch (InstanceNotFoundException ex) {
				// Se ha quedado sin existir entre que se pidió la pantalla y se abrió.
				// Seguimos abajo y buscamos otro suyo en lugar de mandarlo al catálogo.
				housingId = null;
			}
		}

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return null;
		}

		return housingService.showHousings().stream()
				.filter(h -> h.getOwner() != null && h.getOwner().getId().equals(usuario.getId())).findFirst()
				.orElse(null);
	}

	/**
	 * Decide qué se ve: la comparación normal o el estado vacío.
	 *
	 * <p>
	 * El estado vacío no es un error ni un aviso de que algo ha fallado: es la
	 * pantalla explicando <b>qué falta para poder usarla</b>. Por eso lleva a Olaz y
	 * un botón que lleva directamente a resolverlo, en vez de un mensaje que se
	 * limite a decir que no se puede.
	 */
	private void reconstruir() {

		boolean sinAlojamiento = propio == null;

		comparacion.setVisible(!sinAlojamiento);
		buscador.setVisible(!sinAlojamiento);
		acciones.setVisible(!sinAlojamiento);
		vacio.setVisible(sinAlojamiento);

		if (sinAlojamiento) {
			return;
		}

		pintarPropio();
		pintarCandidato();
	}

	private void pintarPropio() {

		panelPropio.removeAll();
		panelPropio.setLayout(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panelPropio.add(Labels.caps(Textos.t("intercambio.entregas")), "gapbottom " + Space.XS);
		panelPropio.add(tarjeta(propio), "growx");
		panelPropio.revalidate();
		panelPropio.repaint();
	}

	/** Lo que se ve cuando el administrador todavía no ha publicado nada. */
	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL, 0, Space.XXL, 0), "[grow,fill]",
				"[]" + Space.LG + "[]" + Space.XS + "[]" + Space.XL + "[]"));
		panel.setOpaque(false);

		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.ACCION)));
		vacioTitulo = Labels.title(Textos.t("intercambio.vacio.titulo"));
		panel.add(centrar(vacioTitulo));
		vacioCuerpo = Labels.muted(Textos.t("intercambio.vacio.cuerpo"));
		panel.add(centrar(vacioCuerpo));
		vacioBoton = Buttons.primary(Textos.t("intercambio.vacio.boton"), e -> navigator.ir(UploadHousingFrame.class));
		panel.add(centrar(vacioBoton));

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	private void pintarCandidato() {

		panelCandidato.removeAll();
		panelCandidato.setLayout(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panelCandidato.add(Labels.caps(Textos.t("intercambio.recibes")), "gapbottom " + Space.XS);

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
		boolean disponible = housingService.isAvailableNow(housing.getId());

		card.add(new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible") : Textos.t("catalogo.disponibilidad.reservada"),
				disponible, housing.getImage()), "h 84!, growx, gapbottom " + Space.SM);

		card.add(Labels.capsAccent(Textos.t("catalogo.numero") + " " + housing.getHousingCode()));

		JLabel nombre = Labels.cardTitle(housing.getName());
		nombre.setFont(Typography.serifMedium(Typography.CARD_TITLE));
		card.add(nombre, "gaptop " + Space.XXS);

		card.add(Labels.muted(housing.getLocation()), "gaptop " + Space.XXS);
		card.add(Labels.muted(Textos.t("intercambio.titular", housing.getOwner().getUsername())),
				"gaptop " + Space.XXS);
		card.add(Labels.priceSmall(Textos.t("intercambio.precioPorNoche", Formato.precio(housing.getPricePerNight()))),
				"gaptop " + Space.XS);

		if (!disponible) {
			// Es la causa exacta por la que el servicio rechazaría el intercambio, dicha
			// antes de intentarlo.
			card.add(Labels.error(Textos.t("intercambio.error.reservadoNoIntercambiable")), "gaptop " + Space.XS);
		}

		return card;
	}

	private Card tarjetaVacia() {

		Card card = new Card(new MigLayout("wrap 1, " + Space.insets(Space.MD), "[grow,fill]", ""));

		card.add(new ImagePlaceholder(), "h 84!, growx, gapbottom " + Space.SM);
		card.add(Labels.muted(Textos.t("intercambio.tarjetaVacia.linea1")), "gaptop " + Space.XXS);
		card.add(Labels.muted(Textos.t("intercambio.tarjetaVacia.linea2")));

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
			error.setText(Textos.t("intercambio.error.escribeCodigo"));
			return;
		}

		long buscado;

		try {
			buscado = Long.parseLong(texto);

		} catch (NumberFormatException ex) {
			error.setText(Textos.t("intercambio.error.codigoInvalido"));
			return;
		}

		Optional<Housing> encontrado = housingService.showHousings().stream()
				.filter(h -> h.getHousingCode() != null && h.getHousingCode() == buscado).findFirst();

		if (!encontrado.isPresent()) {
			candidato = null;
			// String.valueOf, no el long tal cual: MessageFormat formatea los números con
			// separador de miles por defecto ("10.001"), y aquí es un código, no una
			// cantidad.
			error.setText(Textos.t("intercambio.error.codigoNoEncontrado", String.valueOf(buscado)));
			pintarCandidato();
			return;
		}

		if (encontrado.get().getId().equals(housingId)) {
			candidato = null;
			error.setText(Textos.t("intercambio.error.esElMismo"));
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
			error.setText(Textos.t("intercambio.error.algunoReservado"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("intercambio.error.unoNoExiste"));
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
