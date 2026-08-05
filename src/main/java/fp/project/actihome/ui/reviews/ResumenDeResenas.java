package fp.project.actihome.ui.reviews;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Review;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * La caja de resumen que va encima de la lista de reseñas (Fase 8.4).
 *
 * <p>
 * <b>Qué problema resuelve.</b> Antes de esto, la pantalla enseñaba la nota
 * media y debajo la lista. Con dos reseñas eso basta; con veinte, un 4,2 no
 * dice si es «todo el mundo le pone un 4» o «la mitad le pone un 5 y la otra
 * mitad un 3», que son alojamientos completamente distintos. El histograma
 * contesta esa pregunta de un vistazo, y es la razón por la que cualquier sitio
 * de reservas lo lleva.
 *
 * <p>
 * <b>Todo sale de datos que ya existen.</b> No hay ningún campo nuevo ni
 * ninguna consulta de más: el histograma agrupa las reseñas que la pantalla ya
 * ha cargado, y los distintivos de «lo mejor / lo peor» promedian las cinco
 * sub-notas que cada reseña guarda desde siempre. Es una lectura nueva de lo
 * mismo.
 *
 * <p>
 * <b>Los distintivos solo aparecen si hay diferencia real.</b> Con una sola
 * reseña, o cuando las cinco categorías puntúan casi igual, señalar una como
 * «lo mejor» y otra como «lo peor» sería inventar una conclusión que los datos
 * no sostienen — y sale gratis equivocarse hacia el lado de no decir nada.
 */
public class ResumenDeResenas extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Diferencia mínima entre la mejor y la peor categoría para atreverse a señalarlas. */
	private static final double DIFERENCIA_SIGNIFICATIVA = 0.5;

	/** Por debajo de esto no hay muestra suficiente para hablar de tendencias. */
	private static final int RESENAS_MINIMAS_PARA_DISTINTIVOS = 2;

	/** Las cinco categorías que puntúa una reseña, con su clave de idioma. */
	private enum Categoria {

		UBICACION("resenas.subnota.ubicacion", Review::getLocationScore),
		SERVICIO("resenas.subnota.servicio", Review::getServiceScore),
		WIFI("resenas.subnota.wifi", Review::getWifiScore),
		COMIDA("resenas.subnota.comida", Review::getFoodScore),
		LIMPIEZA("resenas.subnota.limpieza", Review::getCleaningScore);

		private final String clave;
		private final transient ToDoubleFunction<Review> nota;

		Categoria(String clave, ToDoubleFunction<Review> nota) {
			this.clave = clave;
			this.nota = nota;
		}
	}

	public ResumenDeResenas(double media, List<Review> resenas) {

		super(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.MD + "[]"));
		setOpaque(false);

		add(cuerpo(media, resenas));

		JPanel distintivos = distintivos(resenas);

		if (distintivos != null) {
			add(distintivos);
		}
	}

	/**
	 * Media grande a la izquierda, separador vertical e histograma a la derecha.
	 *
	 * <p>
	 * <b>El histograma tiene ancho máximo y no crece con la ventana</b>, y esto es
	 * la regla de {@code Layout} aplicada a un caso donde no era obvia: la primera
	 * versión declaraba su columna como {@code grow,fill} y en una ventana ancha
	 * las cinco barras se estiraban de lado a lado, convirtiendo un dato de apoyo
	 * en el elemento más grande de la pantalla. Una barra de proporción se lee por
	 * <em>cuánto</em> de su carril ocupa, no por su longitud absoluta, así que
	 * ensancharla no aporta nada — solo le quita protagonismo a la lista de
	 * reseñas, que es a lo que se viene. Lo que crece es el aire de la derecha.
	 */
	private JPanel cuerpo(double media, List<Review> resenas) {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.XL + "[]" + Space.XL + "[180:260:260]push", "[grow,fill]"));
		panel.setOpaque(false);

		panel.add(bloqueDeMedia(media, resenas), "aligny center");
		panel.add(Hairline.vertical(), "growy, w 1!");
		panel.add(histograma(resenas), "aligny center, growx");

		return panel;
	}

	private JPanel bloqueDeMedia(double media, List<Review> resenas) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[]", "[]" + Space.XXS + "[]"));
		panel.setOpaque(false);

		JLabel nota = Labels.price(Formato.nota(media));
		nota.setFont(Typography.serifMedium(40f));

		panel.add(nota);
		panel.add(Labels.muted(resenas.isEmpty() ? Textos.t("catalogo.sinResenas")
				: Formato.plural(resenas.size(), Textos.t("palabra.resena.singular"),
						Textos.t("palabra.resena.plural"))));

		return panel;
	}

	/**
	 * Cinco filas, de 5 estrellas a 1, con la proporción de reseñas de cada nota.
	 *
	 * <p>
	 * El orden es descendente y no es indiferente: se lee de arriba abajo y lo
	 * primero que se quiere saber es cuánta gente puso la nota máxima.
	 */
	private JPanel histograma(List<Review> resenas) {

		JPanel panel = new JPanel(new MigLayout("wrap 3, " + Space.insets(0),
				"[]" + Space.XS + "[grow,fill]" + Space.XS + "[]", ""));
		panel.setOpaque(false);

		int[] cuantas = repartoPorNota(resenas);
		int total = resenas.size();

		for (int estrellas = 5; estrellas >= 1; estrellas--) {

			int enEstaNota = cuantas[estrellas];

			panel.add(Labels.caps(String.valueOf(estrellas)), "aligny center");
			panel.add(new Carril(total == 0 ? 0 : (double) enEstaNota / total), "h 5!, aligny center");
			panel.add(Labels.muted(String.valueOf(enEstaNota)), "aligny center");
		}

		return panel;
	}

	/**
	 * Cuántas reseñas hay de cada nota entera.
	 *
	 * <p>
	 * La nota total es un decimal (media de las cinco sub-notas), así que se
	 * redondea al entero más cercano para asignarle una barra — igual que hace
	 * cualquier sitio de reservas al enseñar «4 estrellas». El acotado a [1,5]
	 * protege del caso límite de una nota de 0, que redondearía a 0 y se saldría
	 * del array.
	 */
	private int[] repartoPorNota(List<Review> resenas) {

		int[] cuantas = new int[6];

		for (Review resena : resenas) {
			int estrellas = Math.max(1, Math.min(5, (int) Math.round(resena.getTotalScore())));
			cuantas[estrellas]++;
		}

		return cuantas;
	}

	/**
	 * Los distintivos de «lo que mejor puntúa» y «lo que peor», o {@code null} si
	 * los datos no dan para afirmarlo. Ver la nota de clase.
	 */
	private JPanel distintivos(List<Review> resenas) {

		if (resenas.size() < RESENAS_MINIMAS_PARA_DISTINTIVOS) {
			return null;
		}

		Map<Categoria, Double> medias = mediasPorCategoria(resenas);

		Categoria mejor = null;
		Categoria peor = null;

		for (Map.Entry<Categoria, Double> entrada : medias.entrySet()) {

			if (mejor == null || entrada.getValue() > medias.get(mejor)) {
				mejor = entrada.getKey();
			}

			if (peor == null || entrada.getValue() < medias.get(peor)) {
				peor = entrada.getKey();
			}
		}

		if (mejor == null || peor == null || medias.get(mejor) - medias.get(peor) < DIFERENCIA_SIGNIFICATIVA) {
			return null;
		}

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XS + "[]", "[]"));
		fila.setOpaque(false);

		// Las flechas van aqui y no dibujadas a mano porque estan comprobadas: "↑" y
		// "↓" existen en Archivo, que es la fuente de los chips. En Fraunces NO
		// estan, asi que este texto no puede pasar nunca a la serif (MedirGlifos).
		fila.add(Chip.informativo(Textos.t(mejor.clave) + " ↑"));
		fila.add(Chip.informativo(Textos.t(peor.clave) + " ↓"));

		return fila;
	}

	private Map<Categoria, Double> mediasPorCategoria(List<Review> resenas) {

		Map<Categoria, Double> medias = new EnumMap<>(Categoria.class);

		for (Categoria categoria : Categoria.values()) {

			double suma = 0;

			for (Review resena : resenas) {
				suma += categoria.nota.applyAsDouble(resena);
			}

			medias.put(categoria, suma / resenas.size());
		}

		return medias;
	}

	/**
	 * Una barra del histograma: carril tenue de fondo y relleno proporcional.
	 *
	 * <p>
	 * Define el mínimo además del preferido. Es la trampa de Swing que el proyecto
	 * ya tiene documentada: un componente que sobrescribe {@code getPreferredSize}
	 * pero no {@code getMinimumSize} declara un mínimo de cero, y el layout lo
	 * aplasta en cuanto falta sitio sin dar ningún error.
	 */
	private static class Carril extends JPanel {

		private static final long serialVersionUID = 1L;

		private final double proporcion;

		Carril(double proporcion) {
			this.proporcion = proporcion;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(Theme.HAIRLINE);
			g2.fillRect(0, 0, getWidth(), getHeight());

			// El color se resuelve al pintar, no se guarda: asi la barra sigue a la
			// estacion activa sin necesidad de suscribirse a nada (convencion del
			// sistema de diseno).
			g2.setColor(Theme.acc());
			g2.fillRect(0, 0, (int) Math.round(getWidth() * proporcion), getHeight());

			g2.dispose();
		}

		@Override
		public java.awt.Dimension getMinimumSize() {
			return new java.awt.Dimension(0, 5);
		}
	}
}
