package fp.project.actihome.ui.housings;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Amenity;
import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.model.services.HousingData;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Labels;
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
	 * Categorías de alojamiento.
	 *
	 * <p>
	 * Es una lista cerrada y no texto libre porque el catálogo filtra por tipo con
	 * chips fijos: con texto libre, un alojamiento nuevo no aparecería bajo ningún
	 * chip. El nombre comercial va en su propio campo.
	 */
	public static final String[] TIPOS = { "Casa", "Apartamento", "Villa", "Cabaña" };

	private final Field codigo;
	private final Field nombre;
	private final JComboBox<String> tipo;
	private final Field habitaciones;
	private final Field precio;
	private final Field ubicacion;
	private final Field descripcion;
	private JLabel etiquetaTipo;
	private JLabel etiquetaPension;
	private JLabel etiquetaComodidades;

	private final Chip desayuno = new Chip(Textos.t("catalogo.row.desayuno"));
	private final Chip comida = new Chip(Textos.t("catalogo.row.comida"));
	private final Chip cena = new Chip(Textos.t("catalogo.row.cena"));

	private final Map<Amenity, Chip> comodidades = new EnumMap<>(Amenity.class);

	/**
	 * @param conCodigo si se pide el código del alojamiento. Al dar de alta sí; al
	 *                  editar no, porque el código es el identificador público del
	 *                  alojamiento y {@code updateHousing} no lo modifica
	 */
	public HousingForm(boolean conCodigo) {

		// Dos columnas y no una sola larga. Con doce controles apilados, el formulario
		// no cabía en la ventana y los botones quedaban bajo el pliegue —lo que la
		// regla de escritorio del proyecto no permite—, y además dejaba media pantalla
		// vacía a los lados. Repartido en dos, cabe entero y el reparto tiene sentido
		// propio: a la izquierda lo que identifica el alojamiento, a la derecha lo que
		// lo describe y lo que ofrece.
		super(new MigLayout(Space.insets(0), "[grow,fill]" + Space.XXL + "[grow,fill]", "[]"));
		setOpaque(false);

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
		habitaciones = Field.text(Textos.t("alojamientoForm.habitaciones"));
		precio = Field.text(Textos.t("alojamientoForm.precio"));
		ubicacion = Field.text(Textos.t("alojamientoForm.ubicacion"));
		descripcion = Field.textArea(Textos.t("alojamientoForm.descripcion"), 4);

		add(columnaIzquierda(conCodigo), "aligny top");
		add(columnaDerecha(), "aligny top");
	}

	private JPanel columnaIzquierda(boolean conCodigo) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		if (conCodigo) {
			panel.add(codigo, "gapbottom " + Space.MD);
		}

		panel.add(nombre, "gapbottom " + Space.MD);
		panel.add(campoTipo(), "gapbottom " + Space.MD);
		panel.add(dosColumnas(habitaciones, precio), "gapbottom " + Space.MD);
		panel.add(ubicacion);

		return panel;
	}

	private JPanel columnaDerecha() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		panel.add(descripcion, "gapbottom " + Space.LG);

		etiquetaPension = Labels.caps(Textos.t("catalogo.row.pension"));
		panel.add(etiquetaPension, "gapbottom " + Space.XS);
		panel.add(fila(desayuno, comida, cena), "gapbottom " + Space.LG);

		etiquetaComodidades = Labels.caps(Textos.t("catalogo.filtro.comodidades"));
		panel.add(etiquetaComodidades, "gapbottom " + Space.XS);
		panel.add(filaDeComodidades());

		return panel;
	}

	private JPanel campoTipo() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		panel.setOpaque(false);

		tipo.setFont(Typography.sans(Typography.BODY));

		etiquetaTipo = Labels.caps(Textos.t("catalogo.filtro.tipo"));
		panel.add(etiquetaTipo);
		panel.add(tipo, "gaptop " + Space.XXS + ", height 38!");

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
		habitaciones.setEtiqueta(Textos.t("alojamientoForm.habitaciones"));
		precio.setEtiqueta(Textos.t("alojamientoForm.precio"));
		ubicacion.setEtiqueta(Textos.t("alojamientoForm.ubicacion"));
		descripcion.setEtiqueta(Textos.t("alojamientoForm.descripcion"));
		etiquetaPension.setText(Textos.t("catalogo.row.pension"));
		etiquetaComodidades.setText(Textos.t("catalogo.filtro.comodidades"));
		desayuno.setText(Textos.t("catalogo.row.desayuno"));
		comida.setText(Textos.t("catalogo.row.comida"));
		cena.setText(Textos.t("catalogo.row.cena"));
		comodidades.forEach((amenity, chip) -> chip.setText(Textos.etiquetaDe(amenity)));
		tipo.repaint();
	}

	/** Vuelca en el formulario los datos de un alojamiento existente, para editarlo. */
	public void precargar(Housing housing) {

		codigo.setText(String.valueOf(housing.getHousingCode()));
		nombre.setText(housing.getName() == null ? "" : housing.getName());
		tipo.setSelectedItem(housing.getType());
		habitaciones.setText(String.valueOf(housing.getNumberOfRooms()));
		precio.setText(housing.getPricePerNight() == null ? "" : housing.getPricePerNight().toPlainString());
		ubicacion.setText(housing.getLocation() == null ? "" : housing.getLocation());
		descripcion.setText(housing.getDescription() == null ? "" : housing.getDescription());

		desayuno.setSelected(housing.isBreakfast());
		comida.setSelected(housing.isLunch());
		cena.setSelected(housing.isDinner());

		comodidades.forEach((amenity, chip) -> chip.setSelected(amenity.presenteEn(housing)));
	}

	/** Deja el formulario en blanco, para dar de alta uno nuevo. */
	public void limpiar() {

		codigo.setText("");
		nombre.setText("");
		tipo.setSelectedIndex(0);
		habitaciones.setText("");
		precio.setText("");
		ubicacion.setText("");
		descripcion.setText("");

		desayuno.setSelected(false);
		comida.setSelected(false);
		cena.setSelected(false);

		comodidades.values().forEach(chip -> chip.setSelected(false));
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

		HousingData data = HousingData
				.basico(entero(codigo.getText(), Textos.t("alojamientoForm.campo.codigo")).longValue(),
						nombre.getText().trim(), (String) tipo.getSelectedItem(),
						entero(habitaciones.getText(), Textos.t("alojamientoForm.campo.habitaciones")).intValue(),
						decimal(precio.getText()), ubicacion.getText().trim())
				.description(descripcion.getText().trim())
				.breakfast(desayuno.isSelected())
				.lunch(comida.isSelected())
				.dinner(cena.isSelected());

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
