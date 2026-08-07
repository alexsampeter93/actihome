package fp.project.actihome.ui.housings;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.exceptions.LocationNotFoundException;
import fp.project.actihome.model.exceptions.NotTheOwnerException;
import fp.project.actihome.model.services.Coordenadas;
import fp.project.actihome.model.services.GeocodingClient;
import fp.project.actihome.model.services.HousingData;
import fp.project.actihome.model.services.HousingService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Columnas;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.FilaFluida;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.HousingPhotos;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Los campos de un alojamiento: datos, pensión y comodidades.
 *
 * <p>
 * Compartido por dar de alta y por editar, igual que {@code ReviewForm} lo es
 * para publicar y editar una reseña. Las dos pantallas eran prácticamente el
 * mismo archivo dos veces —cambiaban el rótulo, el botón, y que una incluye el
 * código del alojamiento y la otra no—, y esa duplicación es exactamente el
 * terreno donde nació el bug B9.
 *
 * <p>
 * <b>Las comodidades son chips, no casillas.</b> El catálogo ya filtra con chips
 * exactamente sobre estas mismas comodidades, así que usar aquí el mismo control
 * hace que el usuario reconozca lo que está marcando: lo que enciende en este
 * formulario es literalmente lo que luego verá encendido en la ficha y en los
 * filtros. {@link Chip} extiende {@code JToggleButton}, así que además se
 * comporta como una casilla sin tener aspecto de formulario administrativo.
 *
 * <p>
 * <b>El desayuno aparece una sola vez.</b> Es el mismo campo del modelo tanto
 * para la pensión como para las comodidades ({@code Amenity.BREAKFAST} lee y
 * escribe {@code breakfast}), así que se recoge con la pensión y se excluye de
 * la fila de chips. Dos controles para un solo dato solo sirven para que se
 * contradigan.
 */
public class HousingForm extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * El ancho por debajo del cual una columna de este formulario deja de leerse
	 * bien.
	 *
	 * <p>
	 * No es un número redondo: es lo que necesita la fila de chips de comodidades
	 * para no partirse en cuatro líneas, que es el bloque más ancho de las tres
	 * columnas. Poner menos no ahorra nada —lo que se gana de ancho se paga de alto
	 * en cuanto los chips doblan— y poner más deja el formulario en dos columnas en
	 * portátiles donde caben tres.
	 */
	private static final int ANCHO_COMODO_DE_COLUMNA = 320;

	/**
	 * Categorías de alojamiento.
	 *
	 * <p>
	 * Es una lista cerrada y no texto libre porque el catálogo filtra por tipo con
	 * chips fijos: con texto libre, un alojamiento nuevo no aparecería bajo ningún
	 * chip. El nombre comercial va en su propio campo.
	 */
	public static final String[] TIPOS = { "Casa", "Apartamento", "Villa", "Cabaña" };

	/**
	 * Estaciones ideales, con {@code null} en primera posición.
	 *
	 * <p>
	 * Ese {@code null} no es un descuido: es la opción "Cualquiera", y tiene que
	 * ser la primera para que sea la que sale por defecto en un alojamiento nuevo.
	 * Declarar estación es opcional, y quien no lo haga no debería tener que
	 * deshacer una elección que nunca hizo.
	 */
	private static final User.EstacionPreferida[] ESTACIONES = { null, User.EstacionPreferida.PRIMAVERA,
			User.EstacionPreferida.VERANO, User.EstacionPreferida.OTONO, User.EstacionPreferida.INVIERNO };

	private final Field codigo;
	private final Field nombre;
	private final JComboBox<String> tipo;

	/** Estacion ideal, con null como primera opcion ("Cualquiera"). */
	private final JComboBox<User.EstacionPreferida> estacionIdeal;

	/** Oferta de intercambio y, si se activa, qué se busca a cambio (Fase 8.4). */
	private final Chip intercambio = new Chip(Textos.t("alojamientoForm.intercambio"));
	private final Field queBusca = Field.text(Textos.t("alojamientoForm.queBusca"));
	private final Field habitaciones;
	private final Field precio;
	private final Field ubicacion;
	private final Field descripcion;
	private JLabel etiquetaTipo;
	private JLabel etiquetaEstacion;
	private JLabel etiquetaPension;
	private JLabel etiquetaComodidades;

	private final Chip desayuno = new Chip(Textos.t("catalogo.row.desayuno"));
	private final Chip comida = new Chip(Textos.t("catalogo.row.comida"));
	private final Chip cena = new Chip(Textos.t("catalogo.row.cena"));

	private final Map<Amenity, Chip> comodidades = new EnumMap<>(Amenity.class);

	private final ImagePlaceholder previsualizacion = new ImagePlaceholder();
	private JLabel etiquetaFoto;
	private JButton botonElegirFoto;
	private JButton botonQuitarFoto;
	private JLabel errorFoto;

	/** La foto recién elegida en esta sesión de edición, o {@code null} si no se ha tocado. */
	private File fotoElegida;

	/** Fotos de galería elegidas y todavía sin guardar (Fase 8.4). */
	private final transient List<File> fotosDeGaleria = new ArrayList<>();
	private JButton botonAnadirGaleria;
	private JButton botonQuitarGaleria;
	private JLabel resumenGaleria;

	/** El nombre que ya tenía guardado {@code Housing.image}, o {@code null} en un alojamiento nuevo. */
	private String imagenExistente;

	/**
	 * El buscador de coordenadas (F17), o {@code null} si esta pantalla se
	 * construyó sin él.
	 *
	 * <p>
	 * Admitir el nulo no es dejadez: {@code ThemePreview} y las herramientas de
	 * medida construyen formularios sin contexto de Spring, y no tiene sentido que
	 * dejen de compilar por una función accesoria. Sin buscador, el botón
	 * sencillamente no aparece.
	 */
	private final transient GeocodingClient geocoder;

	private JButton botonLocalizar;
	private JLabel estadoUbicacion;

	/**
	 * Dónde está el alojamiento, si se sabe (F17).
	 *
	 * <p>
	 * Se guardan aquí y no se leen del campo de texto porque <b>no están escritas
	 * en ninguna parte de la pantalla</b>: son el resultado de una consulta, no un
	 * dato que el usuario teclee. Ver {@link #olvidarCoordenadas()} para la regla
	 * que las mantiene sincronizadas con el texto.
	 */
	private Double latitud;
	private Double longitud;

	/**
	 * @param conCodigo si se pide el código del alojamiento. Al dar de alta sí; al
	 *                  editar no, porque el código es el identificador público del
	 *                  alojamiento y {@code updateHousing} no lo modifica
	 */
	public HousingForm(boolean conCodigo) {
		this(conCodigo, null);
	}

	/**
	 * @param conCodigo si se pide el código del alojamiento
	 * @param geocoder  buscador de coordenadas (F17), o {@code null} para construir
	 *                  el formulario sin la función de localizar
	 */
	public HousingForm(boolean conCodigo, GeocodingClient geocoder) {

		// **Tres columnas, y antes eran dos.** El reparto en dos tenía sentido de
		// lectura pero no de alto: medido, la izquierda pedía 1042 puntos y la derecha
		// 391. Y en dos columnas **el alto lo pone la más alta, no la media**, así que
		// aquello equivalía a no haber repartido nada — el formulario seguía pidiendo
		// mil puntos en una ventana que da 672 y era, con diferencia, la pantalla más
		// desbordada de la aplicación.
		//
		// El corte en tres sale de las tres preguntas que se contestan al publicar un
		// alojamiento, y por eso no es un troceado a ojo: **qué es** (código, nombre,
		// tipo, tamaño y precio), **dónde está y cómo se ve** (ubicación y fotos) y
		// **qué ofrece** (descripción, intercambio, pensión y comodidades). Cada
		// columna se puede rellenar entera sin mirar a las otras dos.
		//
		// Nada se esconde: no hay pasos ni pestañas. En un formulario de alta, un campo
		// obligatorio detrás de una pestaña es un campo que alguien va a dejar vacío
		// sin saber que existía.
		super(new java.awt.BorderLayout());
		setOpaque(false);

		this.geocoder = geocoder;

		codigo = Field.text(Textos.t("alojamientoForm.codigo"));
		nombre = Field.text(Textos.t("alojamientoForm.nombre"));
		tipo = new JComboBox<>(TIPOS);
		tipo.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(Textos.tipoDeAlojamiento((String) value));
				return this;
			}
		});
		// Estación ideal (Fase 8.4). La primera entrada es "Cualquiera" y representa
		// el nulo: no declarar estación es una respuesta válida, y el alojamiento
		// simplemente no lleva distintivo. Un desplegable que obligara a elegir
		// llenaría el catálogo de etiquetas puestas al azar, que es peor que no
		// tenerlas porque dejarían de significar algo.
		estacionIdeal = new JComboBox<>(ESTACIONES);
		estacionIdeal.setFont(Typography.sans(Typography.BODY));
		estacionIdeal.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(value == null ? Textos.t("alojamientoForm.estacionIdeal.cualquiera")
						: Textos.t("season." + ((User.EstacionPreferida) value).name().toLowerCase() + ".nombre"));
				return this;
			}
		});

		habitaciones = Field.text(Textos.t("alojamientoForm.habitaciones"));
		precio = Field.text(Textos.t("alojamientoForm.precio"));
		ubicacion = Field.text(Textos.t("alojamientoForm.ubicacion"));
		descripcion = Field.textArea(Textos.t("alojamientoForm.descripcion"), 4);

		// **Cuántas columnas se ven no lo decide esta clase, lo decide el ancho.** Con
		// tres escritas a mano, el formulario cabía de alto en un portátil y se salía
		// de ancho en una ventana de 1024: los chips de comodidades quedaban dibujados
		// fuera. Con Columnas son tres en un monitor, dos en un portátil estrecho y una
		// en una ventana mínima, sin ningún umbral escrito.
		Columnas columnas = new Columnas(ANCHO_COMODO_DE_COLUMNA, Space.XL);

		columnas.add(columnaQueEs(conCodigo));
		columnas.add(columnaDondeYComoSeVe());
		columnas.add(columnaQueOfrece());

		add(columnas, java.awt.BorderLayout.CENTER);
	}

	/** Lo que identifica al alojamiento: código, nombre, tipo, tamaño y precio. */
	private JPanel columnaQueEs(boolean conCodigo) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		if (conCodigo) {
			panel.add(codigo, "gapbottom " + Space.aire(Space.MD));
		}

		panel.add(nombre, "gapbottom " + Space.aire(Space.MD));
		panel.add(campoTipo(), "gapbottom " + Space.aire(Space.MD));
		panel.add(dosColumnas(habitaciones, precio));

		return panel;
	}

	/** Dónde está y qué se ve de él: la ubicación y las fotos. */
	private JPanel columnaDondeYComoSeVe() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		panel.add(campoUbicacion(), "gapbottom " + Space.aire(Space.LG));
		panel.add(campoFoto());

		return panel;
	}

	/**
	 * Elegir foto (Fase 7.9), con una previsualización a la izquierda.
	 *
	 * <p>
	 * Sin este campo, la foto de un alojamiento publicado desde la aplicación
	 * solo podía ser el placeholder tintado: no había ningún flujo de subida (ver
	 * decisión #7 de {@code PLAN.md}, reabierta en la Fase 7.9). Se lee el
	 * archivo elegido al momento —solo para la previsualización, con
	 * {@code ImageIO.read} tal cual— y la reducción de verdad ({@link
	 * HousingPhotos#guardar}) se pospone a {@link #guardarFotoSiHaceFalta}, que
	 * llama quien tiene la pantalla: solo entonces se sabe que el resto del
	 * formulario es válido y merece la pena escribir el archivo.
	 */
	/**
	 * La ubicación, con el botón que la sitúa en el mapa (F17).
	 *
	 * <p>
	 * <b>Por qué un botón y no dos campos para escribir la latitud y la
	 * longitud.</b> Nadie sabe de memoria las coordenadas de su casa. Pedirlas
	 * convertiría una función útil en un trámite imposible, y el campo se
	 * quedaría vacío siempre.
	 *
	 * <p>
	 * <b>Y por qué un botón y no una búsqueda automática al terminar de escribir.</b>
	 * Porque esto sale por internet. Una pantalla que se conecta sola, sin que
	 * nadie se lo haya pedido y mientras se teclea, es a la vez una sorpresa y un
	 * goteo de peticiones a un servicio gratuito: cada letra escrita en el campo
	 * sería una consulta. Un botón deja claro qué pasa y cuándo.
	 *
	 * <p>
	 * <b>Es opcional de principio a fin.</b> Si no se pulsa, si no hay red o si el
	 * sitio no está en el índice, el alojamiento se publica igual y simplemente no
	 * tiene coordenadas; lo que depende de ellas —la previsión de la ficha—
	 * desaparece sin decir nada. Hacerlo obligatorio habría atado publicar un
	 * alojamiento a que hubiera internet en ese momento.
	 */
	private JPanel campoUbicacion() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		panel.add(ubicacion);

		if (geocoder == null) {
			return panel;
		}

		botonLocalizar = Buttons.secondary(Textos.t("alojamientoForm.ubicacion.localizar"), e -> localizar());
		estadoUbicacion = Labels.muted(" ");

		// FilaFluida y no una fila rígida: el estado es un texto de ancho muy variable
		// —desde un espacio en blanco hasta "Granada, Andalucía, España"— y una fila
		// rígida exigiría la suma del botón más el texto más largo. Es la regla 1 de
		// la adaptabilidad, y el pie del login es lo que pasa por saltársela.
		FilaFluida fila = new FilaFluida(Space.SM, Space.XXS);
		fila.add(botonLocalizar);
		fila.add(estadoUbicacion);

		panel.add(fila, "gaptop " + Space.XXS);

		// **Cambiar el texto invalida las coordenadas, y esto no es un detalle.** Sin
		// ello se puede localizar "Granada", cambiar el texto a "Bilbao" y guardar: el
		// alojamiento diría Bilbao y su previsión sería la de Granada, sin que nada en
		// la pantalla lo delatara. Un dato derivado que sobrevive a su origen es peor
		// que no tener el dato.
		ubicacion.getInput().getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				olvidarCoordenadas();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				olvidarCoordenadas();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				olvidarCoordenadas();
			}
		});

		return panel;
	}

	/**
	 * Busca las coordenadas del texto escrito, sin bloquear la pantalla.
	 *
	 * <p>
	 * <b>El {@code SwingWorker} es obligatorio aquí y no una elección de estilo.</b>
	 * Swing tiene un único hilo para pintar y para atender al usuario; una llamada
	 * de red en ese hilo congela la ventana entera —ni se puede cancelar, ni se
	 * redibuja— hasta que el otro extremo conteste, y puede no contestar nunca.
	 * {@code doInBackground} corre fuera del hilo de la interfaz y {@code done}
	 * vuelve a él para tocar los componentes, que es la única forma segura de
	 * hacerlo.
	 *
	 * <p>
	 * El botón se desactiva mientras tanto: sin eso, tres clics impacientes son
	 * tres peticiones simultáneas cuyo orden de llegada nadie controla.
	 */
	private void localizar() {

		String lugar = ubicacion.getText().trim();

		if (lugar.isEmpty()) {
			estadoUbicacion.setText(Textos.t("alojamientoForm.ubicacion.vacia"));
			return;
		}

		botonLocalizar.setEnabled(false);
		estadoUbicacion.setText(Textos.t("alojamientoForm.ubicacion.buscando"));

		new SwingWorker<Coordenadas, Void>() {

			@Override
			protected Coordenadas doInBackground() throws LocationNotFoundException {
				return geocoder.localizar(lugar);
			}

			@Override
			protected void done() {

				botonLocalizar.setEnabled(true);

				try {
					Coordenadas punto = get();

					// El texto pudo cambiar mientras se esperaba: si ya no es el mismo, el
					// resultado que ha llegado corresponde a otro sitio y guardarlo sería
					// exactamente el desajuste que el DocumentListener existe para evitar.
					if (!ubicacion.getText().trim().equals(lugar)) {
						return;
					}

					latitud = punto.latitud();
					longitud = punto.longitud();
					estadoUbicacion.setText(Textos.t("alojamientoForm.ubicacion.localizada") + " " + punto.etiqueta());

				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					estadoUbicacion.setText(" ");

				} catch (ExecutionException ex) {
					// Todo lo que lanza doInBackground llega envuelto aquí. No se distingue "no
					// existe ese sitio" de "no hay red" porque el usuario no puede hacer nada
					// distinto en cada caso, y en los dos el alojamiento se publica igual.
					olvidarCoordenadas();
					estadoUbicacion.setText(Textos.t("alojamientoForm.ubicacion.noEncontrada"));
				}
			}
		}.execute();
	}

	/** Deja el alojamiento como no localizado. */
	private void olvidarCoordenadas() {

		latitud = null;
		longitud = null;

		if (estadoUbicacion != null) {
			estadoUbicacion.setText(" ");
		}
	}

	private JPanel campoFoto() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.MD + "[grow,fill]", ""));
		panel.setOpaque(false);

		// El ancho es exacto y el alto un rango: la miniatura tiene que ocupar siempre
		// la misma columna —si no, los botones de al lado bailan de sitio según haya
		// foto o no— pero puede perder alto sin dejar de enseñar lo que enseña.
		panel.add(previsualizacion, "w 150!, h 72:104:104, aligny top");

		JPanel acciones = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		acciones.setOpaque(false);

		etiquetaFoto = Labels.caps(Textos.t("alojamientoForm.foto.label"));
		acciones.add(etiquetaFoto, "gapbottom " + Space.aire(Space.XS));

		botonElegirFoto = Buttons.secondary(Textos.t("alojamientoForm.foto.elegir"), e -> elegirFoto());
		acciones.add(botonElegirFoto, "gapbottom " + Space.XXS);

		botonQuitarFoto = Buttons.link(Textos.t("alojamientoForm.foto.quitar"), e -> quitarFoto());
		acciones.add(botonQuitarFoto);

		errorFoto = Labels.error(" ");
		acciones.add(errorFoto, "gaptop " + Space.XXS);

		panel.add(acciones, "aligny top");
		panel.add(filaDeGaleria(), "newline, span 2, gaptop " + Space.aire(Space.MD));

		return panel;
	}

	/** Añadir fotos de galería: un botón, el recuento y un enlace para vaciar. */
	private JPanel filaDeGaleria() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]" + Space.SM + "[]", "[]"));
		fila.setOpaque(false);

		botonAnadirGaleria = Buttons.secondary(Textos.t("alojamientoForm.galeria.anadir"),
				e -> elegirFotosDeGaleria());

		resumenGaleria = Labels.muted(" ");

		botonQuitarGaleria = Buttons.link(Textos.t("alojamientoForm.galeria.vaciar"), e -> {
			fotosDeGaleria.clear();
			actualizarResumenDeGaleria();
		});

		fila.add(botonAnadirGaleria, "aligny center");
		fila.add(resumenGaleria, "aligny center");
		fila.add(botonQuitarGaleria, "aligny center");

		actualizarResumenDeGaleria();

		return fila;
	}

	private void elegirFoto() {

		JFileChooser selector = new JFileChooser();
		selector.setFileFilter(
				new FileNameExtensionFilter(Textos.t("alojamientoForm.foto.filtro"), "jpg", "jpeg", "png"));

		if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File elegido = selector.getSelectedFile();

		try {
			BufferedImage leida = ImageIO.read(elegido);

			if (leida == null) {
				throw new IOException("formato no reconocido");
			}

			fotoElegida = elegido;
			previsualizacion.setFoto(leida);
			errorFoto.setText(" ");

		} catch (IOException ex) {
			errorFoto.setText(Textos.t("alojamientoForm.foto.error.noSeLee"));
		}
	}

	private void quitarFoto() {

		fotoElegida = null;
		imagenExistente = null;
		previsualizacion.setFoto(null);
		errorFoto.setText(" ");
	}

	/**
	 * Elige varias fotos de galería de una vez (Fase 8.4).
	 *
	 * <p>
	 * {@code setMultiSelectionEnabled} es todo lo que hace falta para que el
	 * diálogo del sistema admita selección múltiple. Elegirlas de una tanda y no de
	 * una en una no es solo comodidad: quien sube fotos de una casa las tiene todas
	 * juntas en la misma carpeta, y obligar a repetir el diálogo cuatro veces es
	 * exactamente el tipo de fricción que hace que la gente suba una sola.
	 *
	 * <p>
	 * Las que no se puedan leer se descartan <b>y se cuentan</b>: aceptar en
	 * silencio una selección de cinco de la que solo entran tres es peor que
	 * decirlo, porque el usuario se entera al ver la galería y ya no sabe cuál
	 * falló.
	 */
	private void elegirFotosDeGaleria() {

		JFileChooser selector = new JFileChooser();
		selector.setMultiSelectionEnabled(true);
		selector.setFileFilter(
				new FileNameExtensionFilter(Textos.t("alojamientoForm.foto.filtro"), "jpg", "jpeg", "png"));

		if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		int descartadas = 0;

		for (File elegido : selector.getSelectedFiles()) {

			try {
				if (ImageIO.read(elegido) == null) {
					throw new IOException("formato no reconocido");
				}

				fotosDeGaleria.add(elegido);

			} catch (IOException ex) {
				descartadas++;
			}
		}

		errorFoto.setText(descartadas == 0 ? " " : Textos.t("alojamientoForm.foto.error.algunasNoSeLeen", descartadas));
		actualizarResumenDeGaleria();
	}

	private void actualizarResumenDeGaleria() {

		resumenGaleria.setText(fotosDeGaleria.isEmpty() ? Textos.t("alojamientoForm.galeria.ninguna")
				: Textos.t("alojamientoForm.galeria.elegidas", fotosDeGaleria.size()));

		botonQuitarGaleria.setVisible(!fotosDeGaleria.isEmpty());
	}

	/**
	 * Guarda en disco las fotos de galería elegidas y las registra en el servicio.
	 *
	 * <p>
	 * Va después de guardar el alojamiento y no dentro del formulario, por lo mismo
	 * que la foto de una reseña (F15): hasta que la entidad no existe no hay
	 * {@code housingId} al que asociarlas. El nombre del archivo sí se puede
	 * calcular antes, porque se deriva del <em>código</em> del alojamiento, que lo
	 * escribe el usuario y no la base de datos.
	 *
	 * <p>
	 * Las excepciones de negocio no se capturan aquí: quien llama acaba de guardar
	 * el alojamiento y está en mejor posición para decidir qué enseñar si algo
	 * falla.
	 */
	public void guardarFotosDeGaleria(Long housingId, Long housingCode, Long ownerId, HousingService servicio)
			throws IOException, InstanceNotFoundException, NotTheOwnerException, NotAuthorizedUserException {

		int siguiente = servicio.showHousingPhotos(housingId).size();

		for (File foto : fotosDeGaleria) {

			siguiente++;
			String nombre = housingCode + "-" + siguiente + ".jpg";

			HousingPhotos.guardar(nombre, foto);
			servicio.addHousingPhoto(housingId, ownerId, nombre);
		}

		fotosDeGaleria.clear();
	}

	/** Lo que ofrece: la descripción, el intercambio, la pensión y las comodidades. */
	private JPanel columnaQueOfrece() {

		// "hidemode 3" porque el "qué busco" del intercambio aparece y desaparece con
		// su chip, y sin esto seguiría reservando su hueco vacío.
		JPanel panel = new JPanel(new MigLayout("wrap 1, hidemode 3, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		panel.add(descripcion, "gapbottom " + Space.aire(Space.LG));

		panel.add(intercambio);
		panel.add(queBusca, "gaptop " + Space.XXS + ", gapbottom " + Space.aire(Space.LG));

		intercambio.addActionListener(e -> queBusca.setVisible(intercambio.isSelected()));
		queBusca.setVisible(false);

		etiquetaPension = Labels.caps(Textos.t("catalogo.row.pension"));
		panel.add(etiquetaPension, "gapbottom " + Space.XS);
		panel.add(fila(desayuno, comida, cena), "gapbottom " + Space.aire(Space.LG));

		etiquetaComodidades = Labels.caps(Textos.t("catalogo.filtro.comodidades"));
		panel.add(etiquetaComodidades, "gapbottom " + Space.XS);
		panel.add(filaDeComodidades());

		return panel;
	}

	/**
	 * El tipo y la estación ideal.
	 *
	 * <p>
	 * <b>El intercambio ya no vive aquí.</b> Estaba pegado a estos dos desplegables
	 * por vecindad de código, no por parentesco: el tipo y la estación dicen
	 * <em>qué es</em> el alojamiento, mientras que ofrecerlo a intercambio dice
	 * <em>qué se hace</em> con él, que es la pregunta de la tercera columna. Al
	 * separarlos, este bloque bajó de 337 puntos a poco más de cien.
	 */
	private JPanel campoTipo() {

		// **Los dos desplegables van uno al lado del otro, no apilados.** Apilados eran
		// 169 puntos —dos rótulos, dos cajas y el hueco entre pares— y este bloque solo
		// era el que decidía el alto de su columna, y por tanto el de la pantalla
		// entera. En fila son 84 y no se pierde nada: los dos son listas cerradas de
		// una palabra ("Villa", "Otoño"), así que ninguno necesita el ancho completo.
		//
		// Es la misma decisión que ya estaba tomada dos bloques más abajo para
		// habitaciones y precio, y por el mismo motivo.
		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[grow,fill]", ""));
		panel.setOpaque(false);

		tipo.setFont(Typography.sans(Typography.BODY));

		JPanel columnaTipo = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		columnaTipo.setOpaque(false);
		etiquetaTipo = Labels.caps(Textos.t("catalogo.filtro.tipo"));
		columnaTipo.add(etiquetaTipo);
		columnaTipo.add(tipo, "gaptop " + Space.XXS + ", height " + Typography.altoDeControl() + "!");

		JPanel columnaEstacion = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		columnaEstacion.setOpaque(false);
		etiquetaEstacion = Labels.caps(Textos.t("alojamientoForm.estacionIdeal"));
		columnaEstacion.add(etiquetaEstacion);
		columnaEstacion.add(estacionIdeal, "gaptop " + Space.XXS + ", height " + Typography.altoDeControl() + "!");

		panel.add(columnaTipo, "aligny top");
		panel.add(columnaEstacion, "aligny top");

		return panel;
	}

	/**
	 * Dos campos cortos en la misma fila.
	 *
	 * <p>
	 * Habitaciones y precio son números de pocos dígitos: darles el ancho completo
	 * del formulario haría un campo enorme para escribir un "3", y alargaría el
	 * formulario una fila de más sin ganar nada.
	 */
	private JPanel dosColumnas(Field izquierda, Field derecha) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]" + Space.MD + "[grow,fill]", ""));
		panel.setOpaque(false);
		panel.add(izquierda);
		panel.add(derecha);

		return panel;
	}

	private JPanel fila(Chip... chips) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "", "[]"));
		panel.setOpaque(false);

		for (Chip chip : chips) {
			panel.add(chip, "gapright " + Space.XS);
		}

		return panel;
	}

	private JPanel filaDeComodidades() {

		JPanel panel = new JPanel(new MigLayout("wrap 4, " + Space.insets(0), "", ""));
		panel.setOpaque(false);

		for (Amenity amenity : Amenity.values()) {

			// El desayuno ya se recoge arriba con la pensión: es el mismo campo.
			if (amenity == Amenity.BREAKFAST) {
				continue;
			}

			Chip chip = new Chip(Textos.etiquetaDe(amenity));
			comodidades.put(amenity, chip);
			panel.add(chip, "gapright " + Space.XS + ", gapbottom " + Space.XS);
		}

		return panel;
	}

	/**
	 * Vuelve a fijar los textos fijos del formulario en el idioma activo (Fase
	 * 7.6). Lo llaman {@code UploadHousingFrame}/{@code UpdateHousingFrame} desde
	 * su propio {@code setVisible(true)}: este formulario es un campo de esas
	 * pantallas singleton, así que no se reconstruye solo con el idioma. El
	 * desplegable de tipo no necesita nada aquí: su renderer ya traduce el valor
	 * seleccionado en cada repintado.
	 */
	public void actualizarTextos() {

		codigo.setEtiqueta(Textos.t("alojamientoForm.codigo"));
		nombre.setEtiqueta(Textos.t("alojamientoForm.nombre"));
		etiquetaTipo.setText(Textos.t("catalogo.filtro.tipo"));
		etiquetaEstacion.setText(Textos.t("alojamientoForm.estacionIdeal"));
		intercambio.setText(Textos.t("alojamientoForm.intercambio"));
		queBusca.setEtiqueta(Textos.t("alojamientoForm.queBusca"));
		habitaciones.setEtiqueta(Textos.t("alojamientoForm.habitaciones"));
		precio.setEtiqueta(Textos.t("alojamientoForm.precio"));
		ubicacion.setEtiqueta(Textos.t("alojamientoForm.ubicacion"));

		if (botonLocalizar != null) {
			botonLocalizar.setText(Textos.t("alojamientoForm.ubicacion.localizar"));
		}
		descripcion.setEtiqueta(Textos.t("alojamientoForm.descripcion"));
		etiquetaPension.setText(Textos.t("catalogo.row.pension"));
		etiquetaComodidades.setText(Textos.t("catalogo.filtro.comodidades"));
		desayuno.setText(Textos.t("catalogo.row.desayuno"));
		comida.setText(Textos.t("catalogo.row.comida"));
		cena.setText(Textos.t("catalogo.row.cena"));
		comodidades.forEach((amenity, chip) -> chip.setText(Textos.etiquetaDe(amenity)));
		tipo.repaint();
		estacionIdeal.repaint();

		etiquetaFoto.setText(Textos.t("alojamientoForm.foto.label"));
		botonElegirFoto.setText(Textos.t("alojamientoForm.foto.elegir"));
		botonQuitarFoto.setText(Textos.t("alojamientoForm.foto.quitar"));
	}

	/** Vuelca en el formulario los datos de un alojamiento existente, para editarlo. */
	public void precargar(Housing housing) {

		codigo.setText(String.valueOf(housing.getHousingCode()));
		nombre.setText(housing.getName() == null ? "" : housing.getName());
		tipo.setSelectedItem(housing.getType());
		estacionIdeal.setSelectedItem(housing.getIdealSeason());
		intercambio.setSelected(housing.isOpenToExchange());
		queBusca.setText(housing.getExchangeWanted() == null ? "" : housing.getExchangeWanted());
		queBusca.setVisible(housing.isOpenToExchange());
		habitaciones.setText(String.valueOf(housing.getNumberOfRooms()));
		precio.setText(housing.getPricePerNight() == null ? "" : housing.getPricePerNight().toPlainString());
		ubicacion.setText(housing.getLocation() == null ? "" : housing.getLocation());
		descripcion.setText(housing.getDescription() == null ? "" : housing.getDescription());

		// **Después del setText de arriba, no antes.** Escribir en el campo dispara el
		// DocumentListener, que borra las coordenadas por diseño; ponerlas primero sería
		// perderlas y dejar sin previsión cualquier alojamiento que se abriera a editar.
		// Es la trampa de siempre de precargar un formulario: el orden de las
		// asignaciones importa cuando unas disparan efectos sobre otras.
		latitud = housing.getLatitude();
		longitud = housing.getLongitude();

		if (estadoUbicacion != null) {
			estadoUbicacion.setText(housing.estaLocalizado() ? Textos.t("alojamientoForm.ubicacion.yaLocalizada") : " ");
		}

		desayuno.setSelected(housing.isBreakfast());
		comida.setSelected(housing.isLunch());
		cena.setSelected(housing.isDinner());

		comodidades.forEach((amenity, chip) -> chip.setSelected(amenity.presenteEn(housing)));

		fotoElegida = null;
		imagenExistente = housing.getImage();
		previsualizacion.setFoto(BrandAssets.fotoDeAlojamiento(imagenExistente));
		errorFoto.setText(" ");
	}

	/** Deja el formulario en blanco, para dar de alta uno nuevo. */
	public void limpiar() {

		codigo.setText("");
		nombre.setText("");
		tipo.setSelectedIndex(0);
		estacionIdeal.setSelectedIndex(0);
		intercambio.setSelected(false);
		queBusca.setText("");
		queBusca.setVisible(false);
		habitaciones.setText("");
		precio.setText("");
		ubicacion.setText("");
		descripcion.setText("");
		olvidarCoordenadas();

		desayuno.setSelected(false);
		comida.setSelected(false);
		cena.setSelected(false);

		comodidades.values().forEach(chip -> chip.setSelected(false));

		fotoElegida = null;
		imagenExistente = null;
		previsualizacion.setFoto(null);
		errorFoto.setText(" ");
	}

	/**
	 * Lo que hay escrito, ya convertido y listo para el servicio.
	 *
	 * <p>
	 * Devuelve el {@link HousingData} <b>completo</b>, no solo los campos que hayan
	 * cambiado: {@code updateHousing} escribe todo lo que recibe, así que enviarlo
	 * a medias borraría el resto. Es la lección del bug B9 escrita en la firma del
	 * método.
	 *
	 * @throws DatosInvalidos si algún número no se puede leer
	 */
	public HousingData datos() throws DatosInvalidos {

		Long housingCode = entero(codigo.getText(), Textos.t("alojamientoForm.campo.codigo"));

		HousingData data = HousingData
				.basico(housingCode, nombre.getText().trim(), (String) tipo.getSelectedItem(),
						entero(habitaciones.getText(), Textos.t("alojamientoForm.campo.habitaciones")).intValue(),
						decimal(precio.getText()), ubicacion.getText().trim())
				.description(descripcion.getText().trim())
				.breakfast(desayuno.isSelected())
				.lunch(comida.isSelected())
				.dinner(cena.isSelected())
				.image(fotoElegida != null ? nombreParaFotoNueva(housingCode) : imagenExistente)
				.idealSeason((User.EstacionPreferida) estacionIdeal.getSelectedItem())
				.openToExchange(intercambio.isSelected())
				.exchangeWanted(queBusca.getText().trim().isEmpty() ? null : queBusca.getText().trim())
				.coordenadas(latitud, longitud);

		comodidades.forEach((amenity, chip) -> data.amenity(amenity, chip.isSelected()));

		return data;
	}

	/**
	 * Igual que {@link #datos()} pero conservando el código que ya tenía el
	 * alojamiento, para la pantalla de edición, que no lo pide.
	 */
	public HousingData datosCon(Long housingCode) throws DatosInvalidos {

		codigo.setText(String.valueOf(housingCode));
		return datos();
	}

	/**
	 * Si se eligió una foto nueva, la reduce y la guarda donde
	 * {@code Housing.image} la va a buscar después. No hace nada si no se tocó la
	 * foto —incluida la edición de un alojamiento que ya tenía una: se conserva
	 * tal cual, no se vuelve a escribir.
	 *
	 * <p>
	 * Se llama <b>después</b> de {@link #datos()}/{@link #datosCon}, nunca antes:
	 * si el resto del formulario no es válido no tiene sentido gastar tiempo
	 * reduciendo una imagen que no se va a usar.
	 *
	 * @param housingCode el mismo código que ya lleva el {@link HousingData}
	 *                     devuelto por esta invocación de {@link #datos()}
	 */
	public void guardarFotoSiHaceFalta(Long housingCode) throws IOException {

		if (fotoElegida != null) {
			HousingPhotos.guardar(nombreParaFotoNueva(housingCode), fotoElegida);
		}
	}

	private static String nombreParaFotoNueva(Long housingCode) {
		return housingCode + ".jpg";
	}

	private static Long entero(String texto, String queEs) throws DatosInvalidos {

		try {
			return Long.valueOf(texto.trim());

		} catch (NumberFormatException ex) {
			throw new DatosInvalidos(Textos.t("alojamientoForm.error.revisaEntero", queEs));
		}
	}

	private static BigDecimal decimal(String texto) throws DatosInvalidos {

		try {
			// Se admite la coma además del punto: en español el teclado numérico escribe
			// coma, y rechazar "85,50" por eso sería absurdo.
			return new BigDecimal(texto.trim().replace(',', '.'));

		} catch (NumberFormatException ex) {
			throw new DatosInvalidos(Textos.t("alojamientoForm.error.revisaPrecio"));
		}
	}

	/**
	 * Un campo numérico no se puede leer.
	 *
	 * <p>
	 * Es una excepción de <b>formulario</b>, no de negocio, y por eso vive aquí y
	 * no en {@code model.exceptions}: no expresa ninguna regla del dominio, solo
	 * que lo tecleado todavía no es un número. Lleva ya el mensaje que verá el
	 * usuario porque solo quien conoce el campo sabe decir cuál falla.
	 */
	public static class DatosInvalidos extends Exception {

		private static final long serialVersionUID = 1L;

		public DatosInvalidos(String mensaje) {
			super(mensaje);
		}
	}
}
