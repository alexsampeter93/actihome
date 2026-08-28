package fp.project.actihome.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.InlineScore;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Comparación lado a lado de hasta {@value #MAXIMO} alojamientos (F16).
 *
 * <p>
 * Se llega solo desde el catálogo, marcando el chip "Comparar" de dos o más
 * fichas: no es un destino de la cabecera, es el resultado de una selección
 * que solo tiene sentido en el momento en que se hizo. Por eso, si se entra
 * aquí sin selección válida —por ejemplo, si un alojamiento marcado se borró
 * mientras tanto y solo queda uno—, la pantalla no se queda a medias: vuelve
 * sola al catálogo.
 *
 * <p>
 * <b>Fila por atributo, no tarjeta por alojamiento.</b> Con tres columnas ya
 * hay precedente de layout —la cuadrícula del catálogo—, pero ahí el objetivo
 * es barrer opciones deprisa; aquí es justo lo contrario, cotejar una a una.
 * Una fila por atributo alinea los precios, las notas y las pensiones de los
 * alojamientos en la misma línea, así que la vista salta horizontalmente sin
 * tener que recordar el dato de la columna anterior.
 */
@Component
@Profile("!test")
@Lazy
public class ComparisonFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * Cuántas columnas caben antes de que una tabla de comparación deje de
	 * leerse de un vistazo. Coincide con el máximo que ya impone el catálogo al
	 * marcar los chips de comparar, así que en la práctica nunca se recorta.
	 */
	private static final int MAXIMO = 3;

	private static final int ALTO_FOTO = 130;

	private final transient HousingService housingService;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private transient List<Long> housingIds = new ArrayList<>();
	private transient List<Housing> housings = new ArrayList<>();

	private JPanel contenido;

	public ComparisonFrame(HousingService housingService, Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	/** Prepara qué alojamientos comparar. La llama el {@link Navigator}. */
	public void loadHousings(List<Long> ids) {
		housingIds = new ArrayList<>(ids);
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
		setSize(1200, 820);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(ShowHousingsFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		contenido = new JPanel();
		contenido.setOpaque(false);

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(contenido), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.volver(ShowHousingsFrame.class));
	}

	/**
	 * Quita un alojamiento de la comparación y redibuja con los que queden.
	 *
	 * <p>
	 * Lo llama el enlace "Quitar" de cada columna. Si tras quitarlo queda menos
	 * de dos, {@link #recargar()} decide volver sola al catálogo: comparar uno
	 * solo consigo mismo no es una pantalla, es un error de estado.
	 */
	private void quitar(Long housingId) {

		housingIds.remove(housingId);
		recargar();
	}

	/**
	 * Vuelve a resolver los alojamientos desde sus ids, en lugar de conservar
	 * los objetos que traía el catálogo: si se llega aquí después de haber
	 * editado uno en otra pantalla de la misma sesión, lo que se compara es su
	 * estado real, no una copia que ya quedó vieja.
	 *
	 * <p>
	 * Un id que ya no existe se descarta en silencio: no es un error del
	 * usuario, es que el alojamiento se borró o cambió de dueño entre que se
	 * marcó y que se pulsó "Comparar".
	 */
	private void recargar() {

		housings = new ArrayList<>();

		for (Long id : housingIds) {

			try {
				housings.add(housingService.findHousing(id));

			} catch (InstanceNotFoundException ex) {
				// Se descarta: ver la nota del método.
			}
		}

		if (housings.size() < 2) {
			navigator.ir(ShowHousingsFrame.class);
			return;
		}

		if (housings.size() > MAXIMO) {
			housings = housings.subList(0, MAXIMO);
		}

		reconstruir();
	}

	private void reconstruir() {

		contenido.removeAll();
		contenido.setLayout(new MigLayout("wrap 1, " + Space.insets(Space.XL, Space.HUGE, Space.XL, Space.HUGE),
				"[grow,fill]", "[]" + Space.LG + "[]" + Space.XL + "[]push"));

		contenido.add(migaDePan(), "growx");
		contenido.add(titular(), "growx");
		contenido.add(tabla(), "grow, " + Layout.anchoCentrado(Layout.CONTENIDO));

		contenido.revalidate();
		contenido.repaint();
	}

	/**
	 * El titular de la comparación, con Olaz al otro extremo.
	 *
	 * <p>
	 * Una de las cuatro pantallas en las que la mascota no estaba, pese a que el
	 * sistema la da por presente en todas. Aquí no cuesta nada: la fila del título
	 * es una sola línea a lo ancho de la ventana, y el extremo derecho estaba
	 * vacío.
	 */
	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		panel.add(Labels.title(Textos.t("comparar.titulo")), "aligny center");
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.ACCION), "aligny center");

		return panel;
	}

	private JPanel migaDePan() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]", "[]"));
		panel.setOpaque(false);
		panel.add(Buttons.link(Textos.t("comparar.volver"), e -> navigator.volver(ShowHousingsFrame.class)));
		return panel;
	}

	// ------------------------------------------------------------------
	// La tabla: una columna de etiquetas y una por alojamiento
	// ------------------------------------------------------------------

	private JPanel tabla() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[160]" + columnasDeValor(), "[]"));
		panel.setOpaque(false);

		panel.add(new JLabel());
		for (int i = 0; i < housings.size(); i++) {
			boolean ultima = i == housings.size() - 1;
			panel.add(cabeceraColumna(housings.get(i)), "aligny top" + (ultima ? ", wrap" : ""));
		}

		separador(panel);

		filaTexto(panel, Textos.t("comparar.atributo.precio"),
				housing -> Formato.precio(housing.getPricePerNight()));
		separador(panel);

		filaNota(panel);
		separador(panel);

		filaTexto(panel, Textos.t("comparar.atributo.habitaciones"),
				housing -> String.valueOf(housing.getNumberOfRooms()));
		separador(panel);

		filaBooleana(panel, Textos.t("catalogo.row.desayuno"), Housing::isBreakfast);
		separador(panel);
		filaBooleana(panel, Textos.t("catalogo.row.comida"), Housing::isLunch);
		separador(panel);
		filaBooleana(panel, Textos.t("catalogo.row.cena"), Housing::isDinner);
		separador(panel);

		filaComodidades(panel);
		separador(panel);

		filaTexto(panel, Textos.t("comparar.atributo.propietario"), housing -> housing.getOwner().getUsername());

		return panel;
	}

	private String columnasDeValor() {

		StringBuilder columnas = new StringBuilder();

		for (int i = 0; i < housings.size(); i++) {
			columnas.append(Space.XL).append("[grow,fill]");
		}

		return columnas.toString();
	}

	private void separador(JPanel panel) {
		panel.add(Hairline.horizontal(), "growx, h 1!, span " + (housings.size() + 1) + ", gapy " + Space.SM + ", wrap");
	}

	private JPanel cabeceraColumna(Housing housing) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.SM + "[]" + Space.XXS + "[]" + Space.SM + "[]"));
		panel.setOpaque(false);

		panel.add(new ImagePlaceholder(Textos.tipoDeAlojamiento(housing.getType()), null, true, housing.getImage()),
				"h " + ALTO_FOTO + "!, growx");

		JLabel nombre = Labels.cardTitle(housing.getName());
		nombre.setFont(Typography.serifMedium(Typography.CARD_TITLE));
		panel.add(nombre);

		panel.add(Labels.muted(housing.getLocation()));

		JPanel acciones = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]", "[]"));
		acciones.setOpaque(false);
		acciones.add(Buttons.link(Textos.t("comparar.verFicha"),
				e -> navigator.ir(HousingDetailsFrame.class, frame -> frame.loadDetails(housing))));
		acciones.add(Buttons.link(Textos.t("comparar.quitar"), e -> quitar(housing.getId())));
		panel.add(acciones);

		return panel;
	}

	private void filaTexto(JPanel panel, String etiqueta, java.util.function.Function<Housing, String> valor) {

		panel.add(Labels.caps(etiqueta), "aligny center");

		for (int i = 0; i < housings.size(); i++) {
			boolean ultima = i == housings.size() - 1;
			panel.add(Labels.body(valor.apply(housings.get(i))), "aligny center" + (ultima ? ", wrap" : ""));
		}
	}

	private void filaBooleana(JPanel panel, String etiqueta, Predicate<Housing> valor) {

		panel.add(Labels.caps(etiqueta), "aligny center");

		for (int i = 0; i < housings.size(); i++) {

			boolean incluida = valor.test(housings.get(i));
			boolean ultima = i == housings.size() - 1;

			JLabel texto = incluida ? Labels.body(Textos.t("detalle.grid.si")) : Labels.muted(Textos.t("comparar.no"));
			panel.add(texto, "aligny center" + (ultima ? ", wrap" : ""));
		}
	}

	private void filaNota(JPanel panel) {

		panel.add(Labels.caps(Textos.t("comparar.atributo.nota")), "aligny center");

		for (int i = 0; i < housings.size(); i++) {
			boolean ultima = i == housings.size() - 1;
			panel.add(new InlineScore(housings.get(i).getScore(), 17f, 60), "aligny center" + (ultima ? ", wrap" : ""));
		}
	}

	private void filaComodidades(JPanel panel) {

		panel.add(Labels.caps(Textos.t("comparar.atributo.comodidades")), "aligny top");

		for (int i = 0; i < housings.size(); i++) {

			boolean ultima = i == housings.size() - 1;
			panel.add(comodidadesDe(housings.get(i)), "growx, aligny top" + (ultima ? ", wrap" : ""));
		}
	}

	private FilaFluida comodidadesDe(Housing housing) {

		FilaFluida fila = new FilaFluida(Space.XS, Space.XXS);

		for (Amenity amenity : Amenity.de(housing)) {
			fila.add(Chip.informativo(Textos.etiquetaDe(amenity)));
		}

		if (fila.getComponentCount() == 0) {
			fila.add(Labels.muted(Textos.t("catalogo.row.sinComodidades")));
		}

		return fila;
	}
}
