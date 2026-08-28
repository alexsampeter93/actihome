package fp.project.actihome.ui;

import java.time.LocalDate;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.CalendarioRango;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Columnas;
import fp.project.actihome.ui.components.Contador;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.SearchField;
import fp.project.actihome.ui.nav.ConNombre;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Buscar por destino, fechas y huéspedes: la pantalla de entrada tras iniciar
 * sesión, antes que el catálogo.
 *
 * <p>
 * <b>Por qué existe si ya había un catálogo con filtros.</b> Un catálogo
 * responde "¿qué hay?"; esta pantalla responde "¿qué necesito?" — lugar,
 * cuándo y cuántos, en ese orden, que es como se piensa un viaje de verdad y
 * no como se piensa una lista que hay que recorrer. El catálogo no
 * desaparece: sigue en la cabecera y sigue siendo el resultado de esta
 * búsqueda, con los mismos criterios ya aplicados y todavía editables allí.
 *
 * <p>
 * <b>Tres preguntas, tres controles que ya existían.</b> El calendario de
 * disponibilidad ({@link CalendarioRango}) y los contadores con signo
 * ({@link Contador}) los construyó ya {@code ReserveHousingFrame}; el marco de
 * imprenta del buscador ya lo tiene {@link SearchField}. Nada de esto es una
 * pantalla nueva de verdad: es composición de vocabulario que ya hablaba la
 * aplicación, con Olaz grande porque es una pantalla que <em>pide algo</em>,
 * igual que el login.
 *
 * <p>
 * <b>Bebés y mascotas no son huéspedes que se guarden.</b> Ningún sistema de
 * reservas real cuenta un bebé para el aforo, así que el contador de bebés se
 * enseña por completitud —quien busca aloj­amiento para su familia espera
 * poder decirlo— pero no viaja a ningún sitio ni limita nada. Mascotas
 * tampoco es un número: reutiliza el mismo filtro de comodidad que ya existe
 * en el catálogo ({@code Amenity.PETS}), así que aquí es un interruptor, no
 * un contador.
 */
@Component
@Profile("!test")
@Lazy
public class SearchHousingsFrame extends JFrame implements ConNombre {

	private static final long serialVersionUID = 1L;

	/**
	 * Cuántos destinos de sugerencia se enseñan como mucho.
	 *
	 * <p>
	 * El catálogo de ejemplo trae diez ubicaciones distintas; enseñarlas todas
	 * convertiría una sugerencia rápida en una segunda lista que hay que leer
	 * entera. Cinco es lo que cabe en una fila cómoda sin doblar en la mayoría
	 * de anchos de ventana — y si dobla, {@link FilaFluida} ya sabe hacerlo sin
	 * romper nada.
	 */
	private static final int SUGERENCIAS = 5;

	private final transient HousingService housingService;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JLabel firmaDeMarca;
	private JLabel titulo;

	private JLabel etiquetaLugar;
	private SearchField destino;
	private FilaFluida sugerenciasFila;

	private JLabel etiquetaFechas;
	private CalendarioRango calendario;

	private JLabel etiquetaHuespedes;
	private JLabel etiquetaAdultos;
	private JLabel subtituloAdultos;
	private JLabel etiquetaNinos;
	private JLabel subtituloNinos;
	private JLabel etiquetaBebes;
	private JLabel subtituloBebes;
	private JLabel etiquetaMascotas;
	private Contador adultos;
	private Contador ninos;
	private Contador bebes;
	private Chip mascotas;

	private JButton enlaceRestablecer;
	private JButton botonBuscar;


	public SearchHousingsFrame(HousingService housingService, Navigator navigator, HeaderPanel headerPanel) {

		this.housingService = housingService;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextosFijos();
			cargarSugerencias();
			limpiar();
		}

		super.setVisible(visible);
	}


	private void limpiar() {

		destino.limpiar();
		adultos.setValor(1);
		ninos.setValor(0);
		bebes.setValor(0);
		mascotas.setSelected(false);

		// Las fechas se dejan sin elegir a propósito, a diferencia de
		// ReserveHousingFrame: ahí son obligatorias porque no hay reserva sin
		// noches, aquí no — buscar solo por destino y huéspedes es un uso legítimo,
		// y CalendarioRango ya nace sin selección mientras nadie llame a
		// seleccionar().
	}

	private void actualizarTextosFijos() {

		titulo.setText(Textos.t("buscar.titulo"));
		etiquetaLugar.setText(Textos.t("buscar.lugar.label"));
		destino.setMarcador(Textos.t("buscar.lugar.placeholder"));
		etiquetaFechas.setText(Textos.t("buscar.fechas.label"));
		etiquetaHuespedes.setText(Textos.t("buscar.huespedes.label"));
		etiquetaAdultos.setText(Textos.t("buscar.huespedes.adultos"));
		subtituloAdultos.setText(Textos.t("buscar.huespedes.adultos.sub"));
		etiquetaNinos.setText(Textos.t("buscar.huespedes.ninos"));
		subtituloNinos.setText(Textos.t("buscar.huespedes.ninos.sub"));
		etiquetaBebes.setText(Textos.t("buscar.huespedes.bebes"));
		subtituloBebes.setText(Textos.t("buscar.huespedes.bebes.sub"));
		etiquetaMascotas.setText(Textos.t("buscar.huespedes.mascotas"));
		mascotas.setText(Textos.t("buscar.huespedes.mascotas.chip"));
		enlaceRestablecer.setText(Textos.t("buscar.restablecer"));
		botonBuscar.setText(Textos.t("buscar.buscar"));
	}


	/**
	 * Rellena las sugerencias de destino con ubicaciones reales del catálogo, no
	 * con los ejemplos de una maqueta.
	 *
	 * <p>
	 * Un destino inventado que nadie puede reservar es peor que no sugerir nada:
	 * la primera vez que alguien lo pulsara descubriría un catálogo vacío y
	 * dudaría de si la búsqueda funciona. Se recalcula en cada visita porque el
	 * catálogo cambia entre sesiones.
	 */
	private void cargarSugerencias() {

		sugerenciasFila.removeAll();

		TreeSet<String> ordenadas = new TreeSet<>();
		for (Housing housing : housingService.showHousings()) {
			if (housing.getLocation() != null && !housing.getLocation().isBlank()) {
				ordenadas.add(housing.getLocation());
			}
		}

		int añadidas = 0;
		for (String lugar : ordenadas) {

			if (añadidas >= SUGERENCIAS) {
				break;
			}

			Chip chip = new Chip(lugar);

			// Momentáneo y no un filtro que se queda marcado: pulsar la sugerencia
			// rellena el campo y el chip vuelve a soltarse, porque lo que manda a
			// partir de ahí es el texto escrito, no qué chip se tocó por última vez.
			chip.addActionListener(e -> {
				destino.setTexto(lugar);
				chip.setSelected(false);
			});

			sugerenciasFila.add(chip);
			añadidas++;
		}

		sugerenciasFila.revalidate();
		sugerenciasFila.repaint();
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(1300, 940);
		setLocationRelativeTo(null);

		headerPanel.marcarActual(SearchHousingsFrame.class);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));
		raiz.add(headerPanel, "growx");

		JPanel exterior = new JPanel(new MigLayout(Space.insetsLaterales(Space.GIANT, Space.GIANT), "[grow,fill]",
				Space.margen(Space.GIANT) + "[grow]" + Space.margen(Space.GIANT)));
		exterior.setOpaque(false);
		exterior.add(cuerpo(), Layout.anchoCentrado(Layout.CONTENIDO) + ", aligny center");

		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		// Igual que el hero del catálogo: cambiar de estación no repinta solo, la
		// frase editorial hay que reescribirla.
	}

	private JPanel cuerpo() {

		JPanel panel = new JPanel(
				new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.aire(Space.LG) + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());
		panel.add(tarjeta());

		return panel;
	}

	/** Frase de la estación, titular grande y Olaz — el mismo reparto que el login. */
	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		firmaDeMarca = Labels.capsAccent("By CocoBrain");
		titulos.add(firmaDeMarca);

		titulo = Labels.hero(" ");
		titulos.add(titulo, "gaptop " + Space.XXS);

		panel.add(titulos);

		// MEDIANO y no GRANDE, a diferencia del login: allí Olaz tiene la pantalla
		// entera para respirar, aquí comparte sitio con una tarjeta de tres bloques
		// y un calendario de dos meses que no ceden casi nada. GRANDE (220) hacía
		// que la pantalla no cupiera en un portátil de verdad sin barra —el fallo
		// exacto que el sistema existe para evitar—, y MEDIANO sigue siendo Olaz
		// reconocible, solo que compartiendo protagonismo con el motivo real de
		// la pantalla: buscar.
		panel.add(new MascotSlot(MascotSlot.Tamano.MEDIANO, Pose.BIENVENIDA), "top");

		return panel;
	}

	/**
	 * La tarjeta blanca con las tres preguntas.
	 *
	 * <p>
	 * <b>Las acciones no son una cuarta fila, van al pie de la columna de
	 * huéspedes.</b> Es la misma decisión que ya tomó {@code ReserveHousingFrame}
	 * con su resumen y su botón "Confirmar": en un reparto a dos columnas, el
	 * alto de la fila entera lo pone la columna más alta —aquí, siempre el
	 * calendario, que no cede—, así que cualquier cosa que quepa en la columna
	 * más corta no cuesta nada de alto extra. Ponerlas como fila aparte sumaba
	 * 66 puntos que sobraban: la pantalla no cabía en un portátil de verdad por
	 * un trozo que la columna de huéspedes ya tenía sitio de sobra para
	 * absorber.
	 */
	private JPanel tarjeta() {

		fp.project.actihome.ui.components.Card tarjeta = new fp.project.actihome.ui.components.Card(
				new MigLayout("wrap 1, " + Space.insets(Space.MD, Space.LG, Space.MD, Space.LG), "[grow,fill]",
						"[]" + Space.aire(Space.MD) + "[]" + Space.aire(Space.MD) + "[]"));

		tarjeta.add(bloqueLugar());
		tarjeta.add(fp.project.actihome.ui.components.Hairline.horizontal(), "growx, h 1!");
		tarjeta.add(bloqueFechasYHuespedes());

		return tarjeta;
	}

	/**
	 * El destino: etiqueta, campo de búsqueda y las sugerencias <b>a su lado</b>.
	 *
	 * <p>
	 * <b>Las sugerencias comparten fila con el campo, y eso es una corrección de
	 * tamaño antes que de estética.</b> El campo mide como mucho lo que mide un
	 * formulario (440) dentro de una tarjeta que llega a 940: había medio ancho de
	 * tarjeta vacío a su derecha y, justo debajo, una fila entera gastada en cinco
	 * chips que caben de sobra en ese hueco. Recuperar esos ~46 puntos de alto es
	 * parte de lo que hace que la pantalla quepa en un portátil de 1366×768, donde
	 * antes se recorría con la rueda.
	 *
	 * <p>
	 * Sigue siendo seguro en ventanas estrechas porque ninguna de las dos piezas
	 * exige su ancho: el campo declara mínimo cero ({@link Layout#ancho}) y
	 * {@link FilaFluida} exige sólo el chip más ancho y dobla en más líneas. Cuando
	 * no quepan al lado, el resultado es el de antes.
	 */
	private JPanel bloqueLugar() {

		JPanel panel = new JPanel(new MigLayout("wrap 2, " + Space.insets(0),
				"[]" + Space.aire(Space.XL) + "[grow,fill]", ""));
		panel.setOpaque(false);

		etiquetaLugar = Labels.caps(" ");
		panel.add(etiquetaLugar, "span 2");

		// Redondo, como los chips de sugerencia que van justo al lado y los contadores
		// de huéspedes de debajo: dentro de esta tarjeta todo son cápsulas, y una
		// caja de esquina viva era la única silueta que se salía del conjunto.
		destino = new SearchField(" ", () -> {
			// La búsqueda no filtra en vivo aquí: solo se aplica al pulsar "Buscar
			// alojamientos", que es lo que lleva a una pantalla nueva. Filtrar en vivo
			// tendría que hacerlo sobre algo visible en esta misma pantalla, y aquí no
			// hay ninguna lista que actualizar.
		}, true);
		panel.add(destino, "gaptop " + Space.XS + ", " + Layout.ancho(Layout.FORMULARIO) + ", h "
				+ Typography.altoDeControl() + "!");

		sugerenciasFila = new FilaFluida(Space.XS, Space.XS);
		panel.add(sugerenciasFila, "gaptop " + Space.XS + ", aligny center");

		return panel;
	}

	private JPanel bloqueFechasYHuespedes() {

		Columnas columnas = new Columnas(380, Space.XXL);
		columnas.add(bloqueFechas());
		columnas.add(bloqueHuespedes());

		return columnas;
	}

	private JPanel bloqueFechas() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		etiquetaFechas = Labels.caps(" ");
		panel.add(etiquetaFechas);

		calendario = new CalendarioRango(() -> {
			// Nada que recalcular en vivo: no hay ni precio ni resumen que dependan de
			// la fecha en esta pantalla, solo del catálogo al que se navega después.
		});
		panel.add(calendario, "gaptop " + Space.XS);

		return panel;
	}

	private JPanel bloqueHuespedes() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.aire(Space.MD) + "[]" + Space.aire(Space.MD) + "[]" + Space.aire(Space.MD) + "[]"
						+ Space.aire(Space.LG) + "[]"));
		panel.setOpaque(false);

		etiquetaHuespedes = Labels.caps(" ");
		panel.add(etiquetaHuespedes);

		etiquetaAdultos = Labels.body(" ");
		subtituloAdultos = Labels.muted(" ");
		adultos = new Contador(1, 1, 16, 1, null, true);
		panel.add(filaDeHuesped(etiquetaAdultos, subtituloAdultos, adultos));

		etiquetaNinos = Labels.body(" ");
		subtituloNinos = Labels.muted(" ");
		ninos = new Contador(0, 0, 10, 1, null, true);
		panel.add(filaDeHuesped(etiquetaNinos, subtituloNinos, ninos));

		etiquetaBebes = Labels.body(" ");
		subtituloBebes = Labels.muted(" ");
		bebes = new Contador(0, 0, 5, 1, null, true);
		panel.add(filaDeHuesped(etiquetaBebes, subtituloBebes, bebes));

		etiquetaMascotas = Labels.body(" ");
		mascotas = new Chip(" ");
		panel.add(filaDeMascotas());

		panel.add(acciones(), "gaptop " + Space.XXS);

		return panel;
	}

	/** Una fila de huéspedes: etiqueta y subtítulo a la izquierda, el contador a la derecha. */
	private JPanel filaDeHuesped(JLabel etiqueta, JLabel subtitulo, Contador contador) {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[]", ""));
		fila.setOpaque(false);

		JPanel texto = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]0[]"));
		texto.setOpaque(false);
		texto.add(etiqueta);
		texto.add(subtitulo);

		fila.add(texto, "aligny center");
		fila.add(contador, "aligny center");

		return fila;
	}

	/**
	 * Mascotas no lleva contador: es la comodidad "pets" del catálogo, así que
	 * aquí solo se pregunta sí o no. Un contador de mascotas sugeriría que
	 * importa cuántas, y al alojamiento no le importa: o admite mascotas o no.
	 */
	private JPanel filaDeMascotas() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[]", ""));
		fila.setOpaque(false);

		fila.add(etiquetaMascotas, "aligny center");
		fila.add(mascotas, "aligny center");

		return fila;
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]push[]", ""));
		fila.setOpaque(false);

		enlaceRestablecer = Buttons.link(" ", e -> limpiar());
		fila.add(enlaceRestablecer, "aligny center");

		botonBuscar = Buttons.primary(" ", e -> buscar());
		fila.add(botonBuscar, "height " + Typography.altoDeBoton() + "!, aligny center");

		return fila;
	}

	private void buscar() {

		String texto = destino.getTexto();
		LocalDate entrada = calendario.getInicio();
		LocalDate salida = calendario.getFin();
		int huespedes = adultos.getValor() + ninos.getValor();
		boolean conMascota = mascotas.isSelected();

		navigator.ir(ShowHousingsFrame.class,
				frame -> frame.aplicarBusqueda(texto, entrada, salida, huespedes, conMascota));
	}

	/** El nombre con el que la enseña el enlace de atrás de otra pantalla. */
	@Override
	public String nombreDePantalla() {
		return Textos.t("header.nav.buscar");
	}
}
