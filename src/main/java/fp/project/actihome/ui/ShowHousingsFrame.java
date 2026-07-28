package fp.project.actihome.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.entities.User.RoleType;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.model.services.ReviewService;
import fp.project.actihome.ui.catalog.HousingRow;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.SeasonSelector;
import fp.project.actihome.ui.components.Stat;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Catálogo de alojamientos: la pantalla principal tras iniciar sesión.
 *
 * <p>
 * Cuatro bandas de arriba abajo, y solo una de ellas crece:
 *
 * <ol>
 * <li><b>Cabecera</b> oscura, de alto fijo.</li>
 * <li><b>Hero</b>, también de alto fijo: a la izquierda la frase de la estación
 * y el titular; a la derecha el selector de estación y las tres cifras del
 * catálogo.</li>
 * <li><b>La lista</b>, que es la única que estira y la única con scroll.</li>
 * <li><b>Colofón</b>, de alto fijo: Olaz y la firma de CocoBrain a la
 * izquierda, y el acceso de administrador a la derecha.</li>
 * </ol>
 *
 * <p>
 * <b>Ese reparto es la regla de escritorio del proyecto.</b> Una web se recorre
 * con la rueda y puede permitirse crecer hacia abajo sin fin; una aplicación de
 * escritorio no, porque su ventana tiene un tamaño y el usuario espera ver la
 * pantalla entera. Aquí el marco —cabecera, hero, colofón— está siempre a la
 * vista y lo que se desplaza es únicamente el contenido. Es la diferencia entre
 * una aplicación y una página web metida en una ventana.
 *
 * <p>
 * <b>Las cifras del hero se calculan sobre el catálogo completo</b>, no sobre lo
 * que quede tras filtrar. Son el estado del catálogo, no del resultado: si
 * bajaran al filtrar dejarían de ser un dato y pasarían a ser un eco del propio
 * filtro.
 */
@Component
@Profile("!test")
@Lazy
public class ShowHousingsFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Tamaño del titular del hero. Ver la nota en {@code titular()}. */
	private static final float TITULAR = 40f;

	private final transient HousingService housingService;
	private final transient ReviewService reviewService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JPanel lista;
	private JLabel tituloPrimera;
	private JLabel tituloSegunda;
	private JLabel fraseEstacional;
	private Stat enCatalogo;
	private Stat disponibles;
	private Stat media;
	private JPanel accionAdmin;

	/**
	 * Testigo de la suscripción a los cambios de estación.
	 *
	 * <p>
	 * {@link Theme#alCambiar} devuelve un objeto con el que darse de baja. Guardarlo
	 * no es opcional: un oyente que nunca se retira mantiene viva la pantalla
	 * entera aunque se cierre, y eso es una fuga de memoria de manual.
	 */
	private transient Object suscripcion;

	public ShowHousingsFrame(HousingService housingService, ReviewService reviewService, SessionManager sessionManager,
			Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.reviewService = reviewService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			refrescarAccionAdmin();
			cargarAlojamientos();
			actualizarTextosEstacionales();
		}

		super.setVisible(visible);
	}

	@Override
	public void dispose() {

		Theme.olvidar(suscripcion);
		super.dispose();
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1240, 840);
		setMinimumSize(new Dimension(1040, 700));
		setLocationRelativeTo(null);

		headerPanel.marcarActual(ShowHousingsFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]",
				"[]0[]0[]0[grow,fill]0[]"));

		raiz.add(headerPanel, "growx");
		raiz.add(hero(), "growx");
		raiz.add(Hairline.horizontal(), "growx, h 1!");
		raiz.add(zonaDeLista(), "grow");
		raiz.add(colofon(), "growx");

		setContentPane(raiz);

		// Cambiar de estación no solo cambia colores: cambia también las palabras (la
		// frase editorial de la estación). Los colores se resuelven solos al repintar;
		// el texto hay que reescribirlo.
		suscripcion = Theme.alCambiar(estacion -> actualizarTextosEstacionales());

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				ajustarEscalaDeDisplay();
			}
		});
	}

	// ------------------------------------------------------------------
	// Hero
	// ------------------------------------------------------------------

	private JPanel hero() {

		// Los márgenes del hero son menores que los del handoff (48/44/30). En una
		// ventana de escritorio el alto es el recurso escaso: cada píxel que se lleva
		// el hero se lo quita a la lista, que es lo único que el usuario ha venido a
		// mirar. El aire lateral se mantiene, que es el que se nota.
		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.XXXL, Space.HUGE, Space.LG, Space.HUGE),
				"[grow]" + Space.XXXL + "[]", "[]"));
		panel.setOpaque(false);

		panel.add(titular(), Layout.ancho(Layout.TEXTO) + ", aligny bottom");
		panel.add(controles(), "aligny bottom");

		return panel;
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.MD + "[]0[]"));
		panel.setOpaque(false);

		fraseEstacional = Labels.capsAccent(Theme.estacion().etiqueta());
		panel.add(fraseEstacional);

		// Dos etiquetas en lugar de una con salto de línea: el renderizado HTML de
		// Swing calcula sus tamaños por su cuenta y se lleva mal con las fuentes
		// registradas en tiempo de ejecución.
		//
		// El titular va a 40px y no a los 46 del handoff, por el mismo motivo que las
		// filas son más bajas: en escritorio, seis píxeles de titular por línea son
		// doce píxeles menos de catálogo visible. La segunda línea en cursiva es la
		// que hace el trabajo editorial, y esa no se toca.
		tituloPrimera = Labels.hero("Elige dónde quieres");
		tituloPrimera.setFont(Typography.serifMedium(TITULAR));

		tituloSegunda = Labels.hero("despertar");
		tituloSegunda.setFont(Typography.serifItalic(TITULAR));

		panel.add(tituloPrimera);
		panel.add(tituloSegunda, "gaptop -8");

		return panel;
	}

	private JPanel controles() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.XXL + "[]"));
		panel.setOpaque(false);

		panel.add(new SeasonSelector());
		panel.add(cifras());

		return panel;
	}

	private JPanel cifras() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0),
				"push[]" + Space.XL + "[1!]" + Space.XL + "[]" + Space.XL + "[1!]" + Space.XL + "[]", "[]"));
		panel.setOpaque(false);

		enCatalogo = new Stat("0", "en catálogo");
		disponibles = new Stat("0", "disponibles");
		media = new Stat("—", "media", true);

		panel.add(enCatalogo);
		panel.add(Hairline.vertical(), "growy");
		panel.add(disponibles);
		panel.add(Hairline.vertical(), "growy");
		panel.add(media);

		return panel;
	}

	// ------------------------------------------------------------------
	// Lista
	// ------------------------------------------------------------------

	private JScrollPane zonaDeLista() {

		lista = new JPanel(new MigLayout("wrap 1, " + Space.insets(0, Space.HUGE, Space.XXL, Space.HUGE),
				"[grow,fill]", "[]"));
		lista.setOpaque(false);

		JScrollPane scroll = new JScrollPane(lista);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		// Los dos bordes, no solo uno: JScrollPane tiene un borde propio y otro para el
		// viewport, y FlatLaf pone una línea en el segundo. Quitando solo el primero
		// queda una raya vertical pegada al contenido que parece un fallo de pintado.
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		// El salto por defecto de Swing es de un píxel por muesca de rueda, que en una
		// lista de filas de 290px se percibe como que el scroll no funciona.
		scroll.getVerticalScrollBar().setUnitIncrement(24);

		return scroll;
	}

	private void cargarAlojamientos() {

		lista.removeAll();

		List<Housing> alojamientos = housingService.showHousings();

		actualizarCifras(alojamientos);

		if (alojamientos.isEmpty()) {
			lista.add(estadoVacio(), "growx");

		} else {
			boolean primera = true;

			for (Housing housing : alojamientos) {

				if (!primera) {
					lista.add(Hairline.horizontal(), "growx, h 1!");
				}

				lista.add(fila(housing), "growx");
				primera = false;
			}
		}

		lista.revalidate();
		lista.repaint();
	}

	private HousingRow fila(Housing housing) {

		Runnable abrir = () -> navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing));

		// El intercambio es cosa del ADMIN propietario del alojamiento, igual que en el
		// detalle. Se decide aquí y no dentro de la fila: la fila pinta lo que le den,
		// no consulta la sesión. Un componente visual que sabe quién ha iniciado sesión
		// es un componente que ya no se puede reutilizar ni probar por separado.
		Runnable intercambiar = puedeIntercambiar(housing)
				? () -> navigator.ir(TradeHousingsFrame.class)
				: null;

		return new HousingRow(housing, contarResenas(housing), abrir, intercambiar);
	}

	private boolean puedeIntercambiar(Housing housing) {

		User usuario = sessionManager.getLoggedInUser();

		return usuario != null && usuario.getRole() == RoleType.ADMIN
				&& housing.getOwner().getId().equals(usuario.getId());
	}

	/**
	 * Cuántas reseñas tiene un alojamiento.
	 *
	 * <p>
	 * Se pide al servicio la lista y se mide, en lugar de contar en la base de
	 * datos. Es una consulta por fila —lo que en jerga se llama el problema "N+1"— y
	 * con un catálogo de seis alojamientos es irrelevante. Se deja así a propósito:
	 * contar en la base exigiría un método nuevo en el DAO, y la capa de interfaz no
	 * habla con los DAOs. Cuando el catálogo crezca, la solución correcta será un
	 * método de servicio que devuelva los recuentos de una vez, no saltarse la capa.
	 */
	private int contarResenas(Housing housing) {

		try {
			return reviewService.showHousingReviews(housing.getId()).size();

		} catch (Exception ex) {
			return 0;
		}
	}

	private JPanel estadoVacio() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.MAX, 0, Space.MAX, 0), "[grow,fill]",
				"[]" + Space.XL + "[]" + Space.XS + "[]"));
		panel.setOpaque(false);

		JPanel centrado = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		centrado.setOpaque(false);
		centrado.add(new MascotSlot(MascotSlot.Tamano.MEDIANO));

		panel.add(centrado);
		panel.add(centrar(Labels.title("Todavía no hay estancias")));
		panel.add(centrar(Labels.muted("Cuando un anfitrión publique la primera, aparecerá aquí.")));

		return panel;
	}

	private JPanel centrar(JComponent componente) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "push[]push", ""));
		fila.setOpaque(false);
		fila.add(componente);
		return fila;
	}

	// ------------------------------------------------------------------
	// Colofón
	// ------------------------------------------------------------------

	private JPanel colofon() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.MD, Space.HUGE, Space.MD, Space.HUGE),
				"[]" + Space.MD + "[]push[]", "[]"));
		panel.setOpaque(false);

		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO), "w 56!, h 56!");
		panel.add(Labels.caps("por CocoBrain"));

		accionAdmin = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", "[]"));
		accionAdmin.setOpaque(false);
		panel.add(accionAdmin);

		return panel;
	}

	/**
	 * El acceso a publicar alojamiento, solo para ADMIN.
	 *
	 * <p>
	 * Se reconstruye en cada visita en lugar de solo mostrarse u ocultarse. El
	 * motivo es el bug B2: esta pantalla es un singleton de Spring, así que añadir
	 * el botón —y su escucha— en cada {@code setVisible} sin limpiar antes dejaría
	 * dos escuchas la segunda vez y tres la tercera.
	 */
	private void refrescarAccionAdmin() {

		accionAdmin.removeAll();

		User usuario = sessionManager.getLoggedInUser();

		if (usuario != null && usuario.getRole() == RoleType.ADMIN) {

			accionAdmin.add(Labels.caps("Registrar alojamiento"), "aligny center");
			accionAdmin.add(new BotonMas(() -> navigator.ir(UploadHousingFrame.class)), "w 44!, h 44!");
		}

		accionAdmin.revalidate();
		accionAdmin.repaint();
	}

	// ------------------------------------------------------------------
	// Refrescos
	// ------------------------------------------------------------------

	private void actualizarCifras(List<Housing> alojamientos) {

		int total = alojamientos.size();
		int libres = 0;
		double suma = 0;
		int conNota = 0;

		for (Housing housing : alojamientos) {

			if (housing.isAvailable()) {
				libres++;
			}

			if (housing.getScore() != null) {
				suma += housing.getScore();
				conNota++;
			}
		}

		enCatalogo.setValor(String.valueOf(total));
		disponibles.setValor(String.valueOf(libres));
		media.setValor(conNota == 0 ? "—" : Formato.nota(suma / conNota));
	}

	private void actualizarTextosEstacionales() {

		fraseEstacional.setText(Theme.estacion().etiqueta());
		repaint();
	}

	/** Los titulares crecen con la ventana; el cuerpo de texto nunca. */
	private void ajustarEscalaDeDisplay() {

		int ancho = getWidth();

		tituloPrimera.setFont(Typography.serifMedium(Layout.display(TITULAR, ancho)));
		tituloSegunda.setFont(Typography.serifItalic(Layout.display(TITULAR, ancho)));

		revalidate();
		repaint();
	}

	/** Botón circular de "añadir", del handoff: sin degradados ni animaciones. */
	private static class BotonMas extends JComponent {

		private static final long serialVersionUID = 1L;

		private final transient Runnable accion;
		private boolean encima;

		BotonMas(Runnable accion) {

			this.accion = accion;

			setPreferredSize(new Dimension(44, 44));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Registrar alojamiento");

			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					accion.run();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					encima = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e) {
					encima = false;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int lado = Math.min(getWidth(), getHeight());
			Color fondo = encima ? Theme.acc() : Theme.hdr();

			g2.setColor(fondo);
			g2.fillOval(0, 0, lado, lado);

			g2.setColor(Theme.bg());
			int centro = lado / 2;
			int brazo = lado / 6;
			g2.fillRect(centro - brazo, centro - 1, brazo * 2, 2);
			g2.fillRect(centro - 1, centro - brazo, 2, brazo * 2);

			g2.dispose();
		}
	}
}
