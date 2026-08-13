package fp.project.actihome.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.TradeProposal;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.AlreadyProposedException;
import fp.project.actihome.model.exceptions.AlreadyReservedException;
import fp.project.actihome.model.exceptions.CannotTradeWithSelfException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.exceptions.ProposalNotPendingException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.TradeProposalService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Card;
import fp.project.actihome.ui.components.Confirmacion;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.FilaFluida;
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

	/**
	 * La foto de cada tarjeta de la comparación: 84 puntos cuando hay sitio, 56
	 * cuando no.
	 *
	 * <p>
	 * <b>Era {@code h 84!}, y ese signo de admiración es lo que impedía que esta
	 * pantalla cupiera en un portátil.</b> Significa "exactamente esto", así que las
	 * dos tarjetas de la comparación —el bloque más alto de la pantalla— no cedían
	 * ni un punto y todo el recorte tenía que salir de otro sitio.
	 *
	 * <p>
	 * Y una foto sí puede ceder: encoge y se sigue viendo lo que es, que es
	 * exactamente lo contrario de lo que le pasa a un campo de texto o a un botón.
	 * Es la misma frontera de siempre —el aire y las imágenes se negocian, los
	 * controles no— aplicada a lo que aquí ocupa el alto de verdad.
	 */
	private static final String ALTO_DE_FOTO = "56:84:84";

	/**
	 * El ancho de esa misma foto, ahora que va al lado del texto y no encima.
	 *
	 * <p>
	 * Rango y no número por lo de siempre: con la ventana estrecha, las dos
	 * tarjetas comparten los 940 puntos de la zona de contenido y la foto cede
	 * hasta 88 antes de que el texto de al lado empiece a apretarse.
	 */
	private static final String ANCHO_DE_FOTO = "88:132:132";

	/**
	 * Cuántos códigos se sugieren como mucho.
	 *
	 * <p>
	 * Enseñar el catálogo entero convertiría una pista rápida en una segunda lista
	 * que hay que leer, así que hay un tope — el mismo criterio que las
	 * sugerencias de destino del buscador, que usan cinco.
	 *
	 * <p>
	 * <b>Aquí son cuatro y no cinco, y el motivo se vio en una captura.</b> Estos
	 * chips viven dentro de la tarjeta de "Recibes", no en una fila a todo lo
	 * ancho: en un portátil esa columna mide unos 390 puntos y cada chip ocupa su
	 * propia línea, así que el quinto empujaba la comparación una línea más alta
	 * — y la comparación es el bloque que decide si esta pantalla cabe. Cuatro
	 * pistas y un botón visible valen más que cinco pistas y un botón fuera de la
	 * ventana.
	 */
	private static final int SUGERENCIAS_DE_CODIGO = 4;

	private final transient HousingService housingService;
	private final transient TradeProposalService tradeProposalService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private Long housingId;
	private transient Housing propio;
	private transient Housing candidato;


	private FilaFluida filaSugerencias;

	private JPanel propuestas;
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
	private WrappingText subtituloCabecera;
	private JButton botonBuscar;
	private JButton botonCancelar;
	private JLabel vacioTitulo;
	private JLabel vacioCuerpo;
	private JButton vacioBoton;

	public TradeHousingsFrame(HousingService housingService, TradeProposalService tradeProposalService,
			SessionManager sessionManager, Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.tradeProposalService = tradeProposalService;
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
				"wrap 1, hidemode 3, " + Space.insetsLaterales(Space.GIANT, Space.GIANT), "[grow,fill]",
				// **"shrink 0" en las filas: aquí lo que cede es el aire, no los bloques.**
				// Los huecos ya están declarados como rango y con eso basta; sin esta marca,
				// cuando faltaban unos pocos puntos MigLayout repartía el déficit entre las
				// filas y se llevaba por delante el subtítulo (11 puntos de alto donde su
				// mínimo era 16) y el precio de las tarjetas (23 donde eran 28). Con ella, si
				// el aire no da para más, se desborda hacia abajo y sale la barra de rescate
				// — que es lo correcto: mejor desplazarse que leer un texto rebanado.
				Space.margen(Space.XXL) + "[shrink 0]" + Space.aire(Space.LG) + "[shrink 0]" + Space.aire(Space.LG)
						+ "[shrink 0]" + Space.aire(Space.MD) + "[shrink 0]" + Space.aire(Space.LG) + "[shrink 0]"
						+ Space.margen(Space.XXL)));
		exterior.setOpaque(false);

		buscador = buscador();

		exterior.add(cabecera(), Layout.anchoCentrado(Layout.CONTENIDO));

		// **Las propuestas van ARRIBA, antes de la comparación.** La primera versión
		// las puso al final, después del botón de enviar, y la captura lo dejó claro:
		// quedaban por debajo del borde de la ventana, así que quien entraba con una
		// propuesta esperando respuesta no se enteraba salvo que se desplazara. Lo que
		// pide una decisión va antes que lo que ofrece una acción — y el bloque se
		// oculta entero cuando no hay ninguna, que es el caso normal, así que no le
		// roba sitio a nadie.
		propuestas = propuestas();
		exterior.add(propuestas, Layout.anchoCentrado(Layout.CONTENIDO));

		comparacion = comparacion();
		exterior.add(comparacion, Layout.anchoCentrado(Layout.CONTENIDO));

		vacio = estadoVacio();
		exterior.add(vacio, Layout.anchoCentrado(Layout.CONTENIDO));

		error = Labels.error(" ");
		exterior.add(error, Layout.anchoCentrado(Layout.TEXTO));

		acciones = acciones();
		exterior.add(acciones, Layout.anchoCentrado(Layout.TEXTO));

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, this::volverAlDetalle);
	}

	/**
	 * Titulares a la izquierda y el buscador por código a la derecha.
	 *
	 * <p>
	 * <b>El buscador estaba en su propia fila, debajo de la comparación</b>, y esa
	 * fila costaba 83 puntos de alto que eran justo los que sacaban esta pantalla
	 * de la ventana de un portátil. Pero moverlo aquí no es un apaño de alto: el
	 * campo de código es <em>el punto de entrada</em> de la pantalla —hasta que no
	 * se busca un alojamiento no hay nada que comparar— y estaba colocado después
	 * del resultado que produce. Arriba, se lee en el orden en que se usa.
	 *
	 * <p>
	 * El ancho del buscador es un rango y no un número por la regla de siempre: en
	 * inglés "Housing code" y "Search" ocupan otra cosa, y una fila rígida con dos
	 * textos variables acaba aplastando al que tenga menos suerte.
	 */
	private JPanel cabecera() {

		// "hidemode 3" también aquí: el buscador se oculta en el estado vacío —sin
		// alojamiento propio no hay nada que ofrecer a cambio— y sin esto seguiría
		// reservando su ancho, dejando el titular estrechado por un hueco invisible.
		JPanel panel = new JPanel(
				new MigLayout("hidemode 3, " + Space.insets(0), "[grow,fill]" + Space.XL + "[]", "[]"));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXS + "[]"));
		titulos.setOpaque(false);

		superTitulo = Labels.capsAccent(Textos.t("header.nav.intercambio"));
		titulos.add(superTitulo);
		tituloCabecera = Labels.title(Textos.t("intercambio.titulo"));
		titulos.add(tituloCabecera);
		// **WrappingText y no Labels.muted, y esto era un fallo latente que solo se
		// destapó al mover el buscador a esta fila.** El subtítulo mide cien caracteres
		// y un JLabel no parte el texto: declara como mínimo la frase entera, 598
		// puntos. Mientras ocupaba una fila para él solo daba igual —había sitio de
		// sobra—, pero compartiendo fila con el buscador ese mínimo imposible empujaba
		// el botón "Buscar" fuera de la ventana en 1024. Es la regla 5 de la
		// adaptabilidad, que este subtítulo llevaba incumpliendo desde siempre sin que
		// se notara.
		subtituloCabecera = WrappingText.muted(Textos.t("intercambio.subtitulo"));
		titulos.add(subtituloCabecera, "gaptop " + Space.aire(Space.XS));

		panel.add(titulos, "aligny top");
		panel.add(buscador, "w 280:380:380, aligny bottom");

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
		panel.add(botonBuscar, "height " + Typography.altoDeControl() + "!, gaptop 18");

		return panel;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		confirmar = Buttons.primary(Textos.t("intercambio.confirmar"), e -> proponer());
		fila.add(confirmar, "height " + Typography.altoDeBoton() + "!");
		botonCancelar = Buttons.link(Textos.t("ajustes.cancelar"), e -> volverAlDetalle());
		fila.add(botonCancelar);

		return fila;
	}

	/**
	 * Los códigos que se pueden pedir, a un clic.
	 *
	 * <p>
	 * <b>El fallo que corrige lo reportó alguien usando la aplicación, y es de
	 * los que no se ven desde dentro:</b> la pantalla pedía "el código del
	 * alojamiento que quieres recibir" y <b>en ningún sitio decía qué códigos
	 * existen</b>. Quien la programó sabe que salen en cada ficha del catálogo;
	 * quien la usa por primera vez se queda delante de un campo numérico sin
	 * ninguna pista de qué escribir, y probar números es exactamente lo que un
	 * formulario no debe pedir nunca.
	 *
	 * <p>
	 * <b>Y la solución no es un texto de ayuda</b> —"los códigos están en el
	 * catálogo"— porque eso manda al usuario a otra pantalla a copiar un número a
	 * mano. Se enseñan aquí los de otros propietarios, con su nombre al lado para
	 * que el número signifique algo, y pulsando uno se rellena el campo y se busca:
	 * el mismo gesto que las sugerencias de destino de la pantalla de búsqueda, y
	 * por el mismo motivo.
	 *
	 * <p>
	 * <b>Primero los abiertos a intercambio.</b> Un propietario que ya ha
	 * declarado que quiere permutar es más probable que acepte, así que ordenarlos
	 * delante no es cosmético: cambia la probabilidad de que la propuesta llegue a
	 * algo.
	 */
	private void pintarSugerencias() {

		filaSugerencias.removeAll();

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return;
		}

		List<Housing> candidatos = housingService.showHousings().stream()
				.filter(h -> h.getOwner() != null && !h.getOwner().getId().equals(usuario.getId()))
				// Los abiertos a intercambio delante. Comparar por el booleano negado
				// ordena "false" antes que "true", así que se niega la condición que
				// queremos primero.
				.sorted(java.util.Comparator.comparing((Housing h) -> !h.isOpenToExchange()))
				.limit(SUGERENCIAS_DE_CODIGO)
				.toList();

		for (Housing candidatoPosible : candidatos) {

			Chip chip = new Chip(Textos.t("catalogo.numero") + " " + candidatoPosible.getHousingCode() + " · "
					+ candidatoPosible.getName());

			// Momentáneo, como los chips de destino del buscador: rellena el campo y se
			// suelta. Lo que manda a partir de ahí es el código escrito, no qué chip se
			// tocó por última vez.
			chip.addActionListener(e -> {
				chip.setSelected(false);
				buscarPorCodigo(String.valueOf(candidatoPosible.getHousingCode()));
			});

			filaSugerencias.add(chip);
		}

		filaSugerencias.revalidate();
		filaSugerencias.repaint();

		// Segundo pase, por lo mismo que el bloque de propuestas: una FilaFluida
		// decide en cuántas líneas se reparte a partir del ancho que ya tiene, y
		// recién construida dentro de una tarjeta cuyo ancho aún no se ha resuelto
		// contesta de menos. Se vio en una captura a 1280×660: la tarjeta se quedaba
		// con sitio para cuatro chips y el quinto no se pintaba — no cortado por la
		// ventana, sino por su propia tarjeta, que es justo el punto ciego que
		// MedirResponsive no cubre.
		SwingUtilities.invokeLater(() -> {
			panelCandidato.revalidate();
			panelCandidato.repaint();
		});
	}

	/**
	 * Las propuestas vivas: las que te han hecho arriba, las que has hecho
	 * debajo.
	 *
	 * <p>
	 * <b>Aquí y no en una pantalla nueva.</b> Una propuesta pendiente es el
	 * intercambio a medio hacer, así que su sitio natural es la pantalla del
	 * intercambio — y quien viene a proponer algo es exactamente quien necesita
	 * enterarse de que le han propuesto otra cosa. Una decimonovena pantalla
	 * habría repartido en dos sitios un mismo asunto y habría necesitado además
	 * su propia entrada en la cabecera.
	 *
	 * <p>
	 * <b>Recibidas primero, y no es un capricho de orden.</b> Las recibidas piden
	 * una decisión tuya; las enviadas solo esperan la de otro. Lo que reclama
	 * acción va antes que lo que informa.
	 *
	 * <p>
	 * El bloque entero desaparece cuando no hay ninguna: un titular
	 * "Propuestas" con nada debajo ocupa alto para decir que no hay nada que
	 * decir.
	 */
	private JPanel propuestas() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, hidemode 3, " + Space.insets(0), "[grow,fill]", "[]"));
		panel.setOpaque(false);
		panel.setVisible(false);

		return panel;
	}

	private void pintarPropuestas() {

		propuestas.removeAll();

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			propuestas.setVisible(false);
			return;
		}

		List<TradeProposal> recibidas;
		List<TradeProposal> enviadas;

		try {
			recibidas = tradeProposalService.showReceived(usuario.getId());
			enviadas = tradeProposalService.showSent(usuario.getId());

		} catch (InstanceNotFoundException ex) {
			// El usuario de la sesión ha dejado de existir. No es un caso que la
			// aplicación pueda provocar hoy, y enseñar la pantalla sin propuestas es
			// más honesto que enseñar un error sobre algo que no se pidió.
			propuestas.setVisible(false);
			return;
		}

		propuestas.setVisible(!recibidas.isEmpty() || !enviadas.isEmpty());

		if (!recibidas.isEmpty()) {

			propuestas.add(Labels.caps(Textos.t("intercambio.propuestas.recibidas")), "gapbottom " + Space.XS);

			for (TradeProposal propuesta : recibidas) {
				propuestas.add(filaDePropuesta(propuesta, true), "growx, gapbottom " + Space.XS);
			}
		}

		if (!enviadas.isEmpty()) {

			propuestas.add(Labels.caps(Textos.t("intercambio.propuestas.enviadas")),
					"gaptop " + Space.LG + ", gapbottom " + Space.XS);

			for (TradeProposal propuesta : enviadas) {
				propuestas.add(filaDePropuesta(propuesta, false), "growx, gapbottom " + Space.XS);
			}
		}

		propuestas.revalidate();
		propuestas.repaint();

		// **Un segundo pase, y no es un apaño supersticioso.** El texto de cada
		// propuesta es un WrappingText, y un JTextArea con ajuste de línea calcula su
		// alto a partir del ancho que YA tiene: recién construido no tiene ninguno,
		// así que la primera vez contesta el alto de un párrafo partido en tantas
		// líneas como quepan en cero puntos — y la tarjeta salía con medio palmo de
		// hueco muerto debajo de una frase de una línea. En cuanto el layout le ha
		// dado su ancho de verdad, la misma pregunta tiene otra respuesta.
		//
		// Es el mismo motivo por el que reiniciar la posición de un scroll va también
		// dentro de un invokeLater: la lista acaba de reconstruirse y su alto todavía
		// no está calculado.
		SwingUtilities.invokeLater(propuestas::revalidate);
	}

	/**
	 * Una propuesta: qué se ofrece a cambio de qué, y qué puedes hacer con ella.
	 *
	 * <p>
	 * El texto va en {@link WrappingText} y no en una etiqueta, por la regla 5 de
	 * la adaptabilidad: lleva dentro dos nombres de alojamiento y un nombre de
	 * usuario, ninguno de longitud conocida, y un {@code JLabel} declararía la
	 * frase entera como ancho mínimo.
	 */
	private Card filaDePropuesta(TradeProposal propuesta, boolean recibida) {

		// **El texto en su propia fila y los botones debajo, no los dos en una.**
		// Compartiendo fila, la tarjeta salía tres veces más alta de lo que su
		// contenido pedía: un JTextArea con ajuste de línea calcula su alto a partir
		// del ancho que ya tiene, y en una celda cuyo ancho depende de lo que ocupen
		// los botones de al lado ese ancho todavía no existe cuando se le pregunta.
		// Ocupando el ancho entero desde el principio, el párrafo sabe en cuántas
		// líneas cabe. Y de paso se lee mejor: la frase completa arriba, lo que se
		// puede hacer con ella debajo.
		Card fila = new Card(
				new MigLayout("wrap 1, " + Space.insets(Space.MD), "[grow,fill]", "[]" + Space.SM + "[]"));

		String texto = recibida
				? Textos.t("intercambio.propuestas.recibida", propuesta.getProposer().getUsername(),
						propuesta.getOffered().getName(), propuesta.getRequested().getName())
				: Textos.t("intercambio.propuestas.enviada", propuesta.getOffered().getName(),
						propuesta.getRequested().getName(), propuesta.getRequested().getOwner().getUsername());

		fila.add(new WrappingText(texto), "growx, wmin 0");

		// El "push" es lo que empuja los botones a la derecha. No vale un "align
		// right" en el componente: la columna de esta tarjeta es "[grow,fill]", y
		// fill anula cualquier alineación —el componente ocupa la celda entera, así
		// que no queda hueco hacia el que alinearlo—. Es la trampa de MigLayout que
		// el manual del proyecto describe para los anchos, vista en el otro eje.
		JPanel botones = new JPanel(new MigLayout(Space.insets(0), "push[]" + Space.SM + "[]", "[]"));
		botones.setOpaque(false);

		if (recibida) {
			botones.add(Buttons.primary(Textos.t("intercambio.propuestas.aceptar"), e -> aceptar(propuesta)),
					"height " + Typography.altoDeBoton() + "!");
			botones.add(Buttons.link(Textos.t("intercambio.propuestas.rechazar"), e -> rechazar(propuesta)));

		} else {
			botones.add(Buttons.link(Textos.t("intercambio.propuestas.retirar"), e -> retirar(propuesta)));
		}

		fila.add(botones, "growx");

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

		// housingId es solo la PETICIÓN ("ábreme el intercambio de este") y puede venir
		// vacía; propio es lo que de verdad se está ofreciendo. Al llegar desde la barra
		// de navegación, resolverAlojamientoPropio elige el primero de los tuyos y
		// housingId se quedaba a null: la tarjeta izquierda se pintaba con normalidad y
		// "Confirmar" mandaba un id nulo al servicio, que reventaba con un error
		// genérico. Se sincronizan aquí, en el único punto donde se resuelve.
		housingId = propio != null ? propio.getId() : null;

		candidato = null;
		codigo.setText("");
		error.setText(" ");

		reconstruir();
	}

	/** El alojamiento que se ofrece, o {@code null} si el usuario no tiene ninguno. */
	private Housing resolverAlojamientoPropio() {

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return null;
		}

		if (housingId != null) {

			try {
				Housing pedido = housingService.findHousing(housingId);

				// Y se comprueba que siga siendo suyo. El frame es singleton, así que
				// housingId sobrevive a la sesión y a las operaciones: justo después de un
				// intercambio apunta a un alojamiento que ya es de otro. Sin esta
				// comprobación la pantalla ofrecería como propia una casa ajena.
				if (pedido.getOwner() != null && pedido.getOwner().getId().equals(usuario.getId())) {
					return pedido;
				}

				housingId = null;

			} catch (InstanceNotFoundException ex) {
				// Se ha quedado sin existir entre que se pidió la pantalla y se abrió.
				// Seguimos abajo y buscamos otro suyo en lugar de mandarlo al catálogo.
				housingId = null;
			}
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

		// Las propuestas se pintan también sin alojamiento propio, y es a propósito:
		// se puede haber quedado sin ninguno justo por haber aceptado un intercambio,
		// y aun así seguir teniendo propuestas enviadas que retirar. Su propio
		// pintado decide si el bloque se ve o no.
		pintarPropuestas();

		if (sinAlojamiento) {
			return;
		}

		pintarPropio();
		pintarCandidato();
		pintarSugerencias();
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
	/**
	 * El estado vacío: explica el mecanismo y enseña que hay gente usándolo.
	 *
	 * <p>
	 * <b>Antes solo bloqueaba.</b> Decía "necesitas un alojamiento" y ofrecía un
	 * botón, que es correcto y no convence a nadie: quien llega aquí sin
	 * alojamientos no sabe todavía qué es intercambiar en ActiHome, y se le está
	 * pidiendo publicar una casa para averiguarlo.
	 *
	 * <p>
	 * El handoff lo resuelve con dos añadidos, y los dos hacen un trabajo
	 * distinto. Los <b>tres pasos</b> contestan «¿cómo funciona esto?». Los
	 * <b>intercambios abiertos ahora mismo</b> contestan la pregunta que de verdad
	 * frena, que es «¿y hay alguien al otro lado?» — y esa no se puede contestar
	 * con texto fijo, hace falta enseñar ofertas reales de otros propietarios.
	 */
	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL, 0, Space.XXL, 0), "[grow,fill]",
				""));
		panel.setOpaque(false);

		// Olaz baja de mediano a pequeño (Fase 8.4). La regla del proyecto es "tamaño
		// según el vacío", y esta pantalla ha dejado de estar vacía: con los tres
		// pasos y la caja de intercambios abiertos debajo, una mascota grande empuja
		// lo que de verdad convence fuera de la ventana.
		panel.add(centrar(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.ACCION)), "gapbottom " + Space.MD);

		vacioTitulo = Labels.title(Textos.t("intercambio.vacio.titulo"));
		panel.add(centrar(vacioTitulo), "gapbottom " + Space.XS);

		vacioCuerpo = Labels.muted(Textos.t("intercambio.vacio.cuerpo"));
		panel.add(centrar(vacioCuerpo), "gapbottom " + Space.XXL);

		panel.add(losTresPasos(), "gapbottom " + Space.XXL);

		// Solo si hay ofertas de otros. Una caja titulada "intercambios abiertos ahora
		// mismo" y vacía debajo diría exactamente lo contrario de lo que pretende.
		JPanel abiertos = intercambiosAbiertos();

		if (abiertos != null) {
			panel.add(abiertos, "gapbottom " + Space.XXL);
		}

		vacioBoton = Buttons.primary(Textos.t("intercambio.vacio.boton"), e -> navigator.ir(UploadHousingFrame.class));
		panel.add(centrar(vacioBoton));

		return panel;
	}

	/** Los tres pasos del mecanismo, en columnas. Texto fijo: es cómo funciona, no datos. */
	private JPanel losTresPasos() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0),
				"[grow,fill]" + Space.XL + "[grow,fill]" + Space.XL + "[grow,fill]", "[]"));
		fila.setOpaque(false);

		fila.add(paso(1, "intercambio.paso1"), "aligny top");
		fila.add(paso(2, "intercambio.paso2"), "aligny top");
		fila.add(paso(3, "intercambio.paso3"), "aligny top");

		return fila;
	}

	private JPanel paso(int numero, String clave) {

		JPanel columna = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XS + "[]"));
		columna.setOpaque(false);

		columna.add(Labels.capsAccent(String.valueOf(numero)));
		columna.add(new WrappingText(Textos.t(clave)), "growx, wmin 0");

		return columna;
	}

	/**
	 * Las ofertas abiertas de otros propietarios, o {@code null} si no hay ninguna.
	 *
	 * <p>
	 * Cada línea lleva a la ficha del alojamiento. No es decoración: alguien que
	 * lee "Loft Barrio Gótico busca casa rural" quiere ver ese loft, y una lista
	 * que enseña algo apetecible y no deja abrirlo es peor que no enseñarlo.
	 */
	private JPanel intercambiosAbiertos() {

		User usuario = sessionManager.getLoggedInUser();

		if (usuario == null) {
			return null;
		}

		List<Housing> abiertos = housingService.showOpenExchanges(usuario.getId());

		if (abiertos.isEmpty()) {
			return null;
		}

		Card tarjeta = new Card(new MigLayout("wrap 1, " + Space.insets(Space.XL), "[grow,fill]", ""));

		tarjeta.add(Labels.capsAccent(Textos.t("intercambio.abiertos.titulo")), "gapbottom " + Space.MD);

		for (Housing abierto : abiertos) {
			tarjeta.add(lineaDeOferta(abierto), "gapbottom " + Space.SM);
		}

		return tarjeta;
	}

	private JPanel lineaDeOferta(Housing abierto) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[grow,fill]", "[]"));
		fila.setOpaque(false);
		fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		fila.add(new Punto(), "w 7!, h 7!, aligny center");

		String texto = abierto.getExchangeWanted() == null
				? Textos.t("intercambio.abiertos.sinDetalle", abierto.getName())
				: Textos.t("intercambio.abiertos.busca", abierto.getName(), abierto.getExchangeWanted());

		fila.add(Labels.body(texto), "aligny center");

		fila.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(abierto));
			}
		});

		return fila;
	}

	/** El disco del acento que hace de viñeta. Se dibuja porque "●" no está en las fuentes. */
	private static class Punto extends JComponent {

		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(Theme.acc());
			g2.fillOval(0, 0, getWidth(), getHeight());

			g2.dispose();
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(7, 7);
		}
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

			// Los chips se rellenan DESPUÉS de construir la tarjeta, porque es ella
			// quien crea la fila que los aloja. Con candidato, esta tarjeta no se pinta
			// y las sugerencias desaparecen con ella, que es lo que se quiere: ya no
			// hay hueco que rellenar.
			panelCandidato.add(tarjetaVacia(), "growx");
			pintarSugerencias();

		} else {
			panelCandidato.add(tarjeta(candidato), "growx");
		}

		panelCandidato.revalidate();
		panelCandidato.repaint();

		// Sin alojamiento encontrado no hay nada que confirmar. Deshabilitar el botón
		// dice lo que falta mejor que dejarlo pulsable para luego dar un error.
		confirmar.setEnabled(candidato != null);
	}

	/**
	 * Una de las dos tarjetas de la comparación: foto a la izquierda, datos a la
	 * derecha.
	 *
	 * <p>
	 * <b>Era vertical —foto arriba, texto debajo— y eso es lo que sacaba el botón
	 * de enviar fuera de la pantalla en un portátil.</b> Apiladas, las dos medidas
	 * se suman: 84 puntos de foto más unos 120 de texto son 204 por tarjeta, y la
	 * comparación es el bloque más alto de la pantalla. En horizontal la altura es
	 * <em>el mayor de los dos</em>, no su suma, y el ancho sobra: la pantalla tiene
	 * 940 puntos y cada tarjeta ocupa la mitad.
	 *
	 * <p>
	 * No es un apaño de última hora, es el mismo reparto que ya usa
	 * {@code HousingRow} en la vista de lista del catálogo, y por el mismo motivo.
	 * <b>La lección general:</b> cuando falta alto y sobra ancho, mirar qué hay
	 * apilado que podría ir al lado — sumar dos medidas siempre cuesta más que
	 * quedarse con la mayor.
	 */
	private Card tarjeta(Housing housing) {

		Card card = new Card(new MigLayout(Space.insets(Space.MD), "[]" + Space.MD + "[grow,fill]", "[]"));
		boolean disponible = housingService.isAvailableNow(housing.getId());

		card.add(new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()),
				disponible ? Textos.t("catalogo.disponibilidad.disponible") : Textos.t("catalogo.disponibilidad.reservada"),
				disponible, housing.getImage()), "w " + ANCHO_DE_FOTO + ", h " + ALTO_DE_FOTO + ", aligny top");

		JPanel datos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		datos.setOpaque(false);

		datos.add(Labels.capsAccent(Textos.t("catalogo.numero") + " " + housing.getHousingCode()));

		JLabel nombre = Labels.cardTitle(housing.getName());
		nombre.setFont(Typography.serifMedium(Typography.CARD_TITLE));
		datos.add(nombre, "gaptop " + Space.XXS);

		datos.add(Labels.muted(housing.getLocation()), "gaptop " + Space.XXS);
		datos.add(Labels.muted(Textos.t("intercambio.titular", housing.getOwner().getUsername())),
				"gaptop " + Space.XXS);
		datos.add(Labels.priceSmall(Textos.t("intercambio.precioPorNoche", Formato.precio(housing.getPricePerNight()))),
				"gaptop " + Space.aire(Space.XS));

		if (!disponible) {
			// Es la causa exacta por la que el servicio rechazaría el intercambio, dicha
			// antes de intentarlo.
			datos.add(Labels.error(Textos.t("intercambio.error.reservadoNoIntercambiable")),
					"gaptop " + Space.aire(Space.XS));
		}

		card.add(datos, "aligny top");

		return card;
	}

	/**
	 * El hueco de "Recibes" mientras no se ha buscado nada: el marcador de foto,
	 * la explicación y <b>los códigos que se pueden pedir</b>.
	 *
	 * <p>
	 * <b>Los chips vivían en una fila propia y se han movido aquí, por dos motivos
	 * que apuntan en la misma dirección.</b> El de medida: esa fila costaba 68
	 * puntos de alto —medidos con {@code MedirPantallas}— y era justo lo que sacaba
	 * el botón de enviar fuera de la ventana en un portátil. Aquí no cuesta
	 * ninguno: esta tarjeta ya ocupa el alto de la comparación y estaba medio
	 * vacía.
	 *
	 * <p>
	 * Y el de sentido, que es el bueno: <b>las sugerencias rellenan este hueco
	 * exactamente</b>. Pulsar un chip sustituye la tarjeta vacía por el alojamiento
	 * que representa, así que verlos dentro del sitio donde va a aparecer el
	 * resultado dice lo que hacen sin necesidad de explicarlo — y desaparecen solos
	 * en cuanto hay un candidato, porque entonces esta tarjeta ya no se pinta.
	 */
	private Card tarjetaVacia() {

		Card card = new Card(new MigLayout(Space.insets(Space.MD), "[]" + Space.MD + "[grow,fill]", "[]"));

		card.add(new ImagePlaceholder(), "w " + ANCHO_DE_FOTO + ", h " + ALTO_DE_FOTO + ", aligny top");

		JPanel datos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		datos.setOpaque(false);

		// **WrappingText y no Labels.muted**, y el cambio es obligado desde que esta
		// tarjeta reparte en dos columnas: un JLabel no parte el texto, así que
		// declara la frase entera como ancho mínimo —313 puntos medidos— y en una
		// columna de 336 eso empuja la tarjeta de al lado fuera de la ventana. Lo
		// cazó MedirResponsive a 1024×600 en cuanto la tarjeta dejó de ocupar todo el
		// ancho. Es la regla 5 de la adaptabilidad, incumplida sin querer al mover el
		// texto a un sitio más estrecho: **un texto que antes cabía no es un texto
		// que quepa.**
		datos.add(WrappingText.muted(Textos.t("intercambio.tarjetaVacia.linea1") + " "
				+ Textos.t("intercambio.tarjetaVacia.linea2")), "growx, wmin 0");

		filaSugerencias = new FilaFluida(Space.XS, Space.XS);
		datos.add(filaSugerencias, "growx, gaptop " + Space.aire(Space.SM));

		card.add(datos, "aligny top");

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

	/**
	 * Envía la propuesta. <b>No permuta nada</b>: eso lo hará quien la reciba, si
	 * quiere.
	 *
	 * <p>
	 * <b>Con confirmación previa, y no por costumbre.</b> La regla del proyecto no
	 * es "pregunta antes de todo" sino "pregunta antes de lo que no se puede
	 * deshacer", y aquí lo irreversible no es enviar —una propuesta se retira—
	 * sino lo que puede pasar después sin volver a preguntarte: que el otro acepte
	 * y tu alojamiento deje de ser tuyo. El diálogo dice exactamente eso, con el
	 * nombre de la persona delante, porque enseñar a quién se lo estás mandando es
	 * la última oportunidad de detectar que te has equivocado de código.
	 */
	private void proponer() {

		if (candidato == null) {
			return;
		}

		String otro = candidato.getOwner().getUsername();

		if (!Confirmacion.preguntar(this, Textos.t("intercambio.proponer.titulo"),
				Textos.t("intercambio.proponer.cuerpo", otro), Textos.t("intercambio.confirmar"))) {
			return;
		}

		try {
			tradeProposalService.propose(sessionManager.getLoggedInUser().getId(), housingId,
					candidato.getHousingCode());

			// Se recarga la pantalla en lugar de navegar a otra: la propuesta acaba de
			// aparecer en "las que has enviado", tres dedos más abajo. Mandar al catálogo
			// —lo que hacía la versión anterior— era justo lo que hacía que pareciera que
			// no había pasado nada.
			recargar();
			Toast.mostrar(this, Textos.t("intercambio.proponer.enviada", otro));

		} catch (AlreadyReservedException ex) {
			error.setText(Textos.t("intercambio.error.algunoReservado"));

		} catch (AlreadyProposedException ex) {
			error.setText(Textos.t("intercambio.error.yaPropuesto"));

		} catch (CannotTradeWithSelfException ex) {
			error.setText(Textos.t("intercambio.error.contigoMismo"));

		} catch (NotTheOwnerException ex) {
			error.setText(Textos.t("intercambio.error.noEsTuyo"));

		} catch (InstanceNotFoundException ex) {
			error.setText(Textos.t("intercambio.error.unoNoExiste"));
		}
	}

	private void aceptar(TradeProposal propuesta) {

		String otro = propuesta.getProposer().getUsername();
		String recibes = propuesta.getOffered().getName();
		String entregas = propuesta.getRequested().getName();

		if (!Confirmacion.preguntar(this, Textos.t("intercambio.propuestas.aceptar.titulo"),
				Textos.t("intercambio.propuestas.aceptar.cuerpo", recibes, entregas, otro),
				Textos.t("intercambio.propuestas.aceptar"))) {
			return;
		}

		try {
			tradeProposalService.accept(sessionManager.getLoggedInUser().getId(), propuesta.getId());

			recargar();
			Toast.mostrar(this, Textos.t("intercambio.propuestas.aceptada", recibes));

		} catch (AlreadyReservedException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.algunoReservado"));

		} catch (NotTheOwnerException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.noEsTuyo"));

		} catch (ProposalNotPendingException ex) {
			// Alguien la retiró mientras estaba en pantalla. Se recarga en silencio: el
			// estado real ha cambiado y la lista de abajo ya lo cuenta.
			recargar();
			error.setText(Textos.t("intercambio.error.yaNoPendiente"));

		} catch (InstanceNotFoundException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.unoNoExiste"));
		}
	}

	private void rechazar(TradeProposal propuesta) {

		try {
			tradeProposalService.reject(sessionManager.getLoggedInUser().getId(), propuesta.getId());

			recargar();
			Toast.mostrar(this, Textos.t("intercambio.propuestas.rechazada"));

		} catch (ProposalNotPendingException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.yaNoPendiente"));

		} catch (NotTheOwnerException | InstanceNotFoundException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.unoNoExiste"));
		}
	}

	private void retirar(TradeProposal propuesta) {

		try {
			tradeProposalService.withdraw(sessionManager.getLoggedInUser().getId(), propuesta.getId());

			recargar();
			Toast.mostrar(this, Textos.t("intercambio.propuestas.retirada"));

		} catch (ProposalNotPendingException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.yaNoPendiente"));

		} catch (NotTheOwnerException | InstanceNotFoundException ex) {
			recargar();
			error.setText(Textos.t("intercambio.error.unoNoExiste"));
		}
	}

	private void volverAlDetalle() {
		navigator.volver(HousingDetailsFrame.class, frame -> frame.loadDetails(propio));
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
