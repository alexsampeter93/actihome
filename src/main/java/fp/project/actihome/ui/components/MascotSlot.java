package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Season;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Ranura para Olaz, la mascota de CocoBrain, con la variante de la estación
 * activa.
 *
 * <p>
 * Olaz aparece en todas las pantallas menos una por decisión de producto. La
 * excepción es el detalle de un alojamiento: entre galería, tiempo, mapa, precio
 * y reseñas no queda un hueco que no le quite sitio a algo, y la propia regla de
 * abajo dice que la mascota nunca compite con la información. Para que sume
 * en lugar de cansar, tres reglas de oficio que este componente hace fáciles de
 * respetar:
 *
 * <ol>
 * <li><b>Ranura fija.</b> Siempre en el mismo sitio de la pantalla. Lo que
 * aparece en un lugar distinto cada vez se percibe como desorden; lo que
 * aparece siempre en el mismo se percibe como identidad.</li>
 * <li><b>Tamaño según el vacío.</b> Grande donde la pantalla respira (login,
 * estados vacíos, confirmaciones); pequeño donde hay contenido denso
 * (catálogo, listados). La mascota nunca compite con la información.</li>
 * <li><b>Variante estacional siempre.</b> Cambiar de estación cambia a Olaz, y
 * eso convierte el selector en algo que apetece tocar.</li>
 * <li><b>Pose según lo que hace la pantalla.</b> {@link Pose#BIENVENIDA} donde
 * se pide algo al usuario (formularios, confirmaciones) y {@link Pose#ACCION}
 * donde se explora (catálogo, listados, detalles). La regla es fija por
 * pantalla, no aleatoria: una mascota que sale distinta cada vez que abres la
 * misma ventana se lee como ruido, no como identidad.</li>
 * </ol>
 *
 * <p>
 * <b>Ausencia elegante.</b> Si faltara el archivo, la ranura dibuja un contorno
 * discontinuo con el nombre de la variante en lugar de reventar. Un componente
 * que se rompe por un recurso decorativo obliga a parar; uno que degrada bien,
 * no.
 */
public class MascotSlot extends JComponent {

	private static final long serialVersionUID = 1L;

	/**
	 * Los tres tamaños de ranura, en puntos de lado.
	 *
	 * <p>
	 * <b>{@code PEQUENO} decía 72 y durante meses se dibujó a 56.</b> Las once
	 * pantallas que lo usan lo añadían con {@code "top, w 56!, h 56!"} —copiado de
	 * una a otra, como se copian estas cosas— y un {@code !} en MigLayout no negocia
	 * con nadie: el tamaño declarado aquí no llegaba a aplicarse nunca. La constante
	 * era, literalmente, documentación falsa.
	 *
	 * <p>
	 * Retirados esos forzados, Olaz recupera sus 72 puntos <b>sin costar un solo
	 * punto de alto</b>: en todas esas pantallas la ranura convive en una fila con
	 * un bloque de títulos —versalita, titular y a veces un subtítulo— que ya mide
	 * entre 70 y 80. La altura de esa fila la ponía el texto, no la mascota, así que
	 * los 16 puntos que se le habían quitado no se los estaba dando a nadie.
	 */
	public enum Tamano {

		PEQUENO(72), MEDIANO(140), GRANDE(220);

		private final int lado;

		Tamano(int lado) {
			this.lado = lado;
		}
	}

	private final Tamano tamano;
	private Pose pose;

	public MascotSlot(Tamano tamano, Pose pose) {

		this.tamano = tamano;
		this.pose = pose;
		setPreferredSize(new Dimension(tamano.lado, tamano.lado));
		setMinimumSize(new Dimension(tamano.lado / 2, tamano.lado / 2));
	}

	/**
	 * Cambia la pose sin cambiar de ranura.
	 *
	 * <p>
	 * Existe por la bienvenida, que es una sola pantalla con tres pasos: el primero
	 * saluda y los dos siguientes cuentan qué se puede hacer, así que la
	 * ilustración acompaña. <b>No contradice la regla de que la pose la decide la
	 * pantalla y nunca el azar</b>: sigue decidiéndola la pantalla, sólo que ahora
	 * en función de en qué paso está, y el mismo paso da siempre la misma imagen.
	 */
	public void setPose(Pose pose) {

		this.pose = pose;
		repaint();
	}

	/**
	 * Una ranura que <b>crece con el hueco que le den</b>, en vez de tener un
	 * tamaño fijo.
	 *
	 * <p>
	 * Es la respuesta a un problema que las tres tallas fijas no sabían resolver:
	 * hay pantallas —el detalle de una reseña, la comparación, la bandeja de
	 * mensajes— que en un portátil van justas y en un monitor de 1920 dejan
	 * cuatrocientos puntos de vacío. Una talla fija obliga a elegir entre romper la
	 * primera o desaprovechar la segunda; con esta el mismo componente se encoge
	 * hasta desaparecer donde no cabe y llena el hueco donde sobra.
	 *
	 * <p>
	 * Técnicamente son dos declaraciones honestas: el <b>preferido</b> es el de la
	 * ranura grande —lo que le gustaría ocupar— y el <b>mínimo es cero</b>, que en
	 * una mascota decorativa es la verdad y no una mentira como lo era en un
	 * párrafo: aquí no hay contenido indivisible que defender, y no verla no rompe
	 * nada. Colocada en una fila con {@code grow}, el layout hace el resto, y como
	 * {@code pintarAjustada} ya escala conservando la proporción, la ilustración
	 * sale bien a cualquier tamaño intermedio.
	 */
	public static MascotSlot queLlenaElHueco(Pose pose) {

		MascotSlot ranura = new MascotSlot(Tamano.GRANDE, pose);
		ranura.setMinimumSize(new Dimension(0, 0));

		return ranura;
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		Season estacion = Theme.estacion();
		BufferedImage imagen = BrandAssets.olaz(estacion, poseEfectiva(estacion));

		if (imagen != null) {
			pintarAjustada(g2, imagen);
		} else {
			pintarHueco(g2, estacion);
		}

		g2.dispose();
	}

	/**
	 * La pose que se pinta de verdad, que en verano no siempre es la que pidió la
	 * pantalla.
	 *
	 * <p>
	 * <b>Verano tiene un trato aparte por decisión de producto:</b> su ilustración
	 * de la toalla es la única de las ocho en la que Olaz enseña la propia
	 * aplicación en el móvil, así que es la que más trabaja como imagen de marca y
	 * se quiere ver más. Pero no se puede poner en cualquier sitio, y la razón es
	 * de composición, no de gusto: esa escena es <b>apaisada</b> —personaje, toalla
	 * y sombrilla— mientras que las siete restantes son figuras <b>verticales</b>.
	 * Una escena apaisada metida en una ranura cuadrada de 56px se reduce hasta que
	 * no se distingue qué es, así que ahí «más protagonismo» acabaría dando lo
	 * contrario.
	 *
	 * <p>
	 * De modo que se usa en las ranuras <b>mediana y grande</b>, donde hay sitio
	 * para leerla, y en las pequeñas se mantiene la figura vertical que sí llena un
	 * cuadrado. Es la misma idea que la regla de «tamaño según el vacío» del
	 * sistema, aplicada ahora también a <em>qué</em> se dibuja y no solo a cómo de
	 * grande.
	 */
	private Pose poseEfectiva(Season estacion) {

		if (estacion == Season.VERANO) {

			// La regla es la misma en las dos direcciones, y hasta ahora sólo se
			// aplicaba en una. La escena de la toalla es apaisada —personaje, toalla y
			// sombrilla— y en una ranura pequeña se reduce hasta que no se distingue
			// qué es; por eso se prefiere en mediano y grande, **y por eso mismo hay que
			// evitarla en pequeño aunque sea la pantalla quien la pida**. Faltaba este
			// segundo caso: al añadir Olaz al detalle de una reseña y a la comparación,
			// que son pantallas de explorar y piden ACCION, en verano salía la escena
			// apaisada a 72 puntos y se leía como una mancha.
			return tamano == Tamano.PEQUENO ? Pose.BIENVENIDA : Pose.ACCION;
		}
		return pose;
	}

	/** Dibuja la imagen conservando su proporción y centrada en la ranura. */
	private void pintarAjustada(Graphics2D g2, BufferedImage imagen) {

		int ancho = getWidth();
		int alto = getHeight();

		double escala = Math.min((double) ancho / imagen.getWidth(), (double) alto / imagen.getHeight());
		int nuevoAncho = (int) Math.round(imagen.getWidth() * escala);
		int nuevoAlto = (int) Math.round(imagen.getHeight() * escala);

		g2.drawImage(imagen, (ancho - nuevoAncho) / 2, (alto - nuevoAlto) / 2, nuevoAncho, nuevoAlto, null);
	}

	/** Marca de posición mientras no haya ilustración con transparencia. */
	private void pintarHueco(Graphics2D g2, Season estacion) {

		int ancho = getWidth();
		int alto = getHeight();

		g2.setColor(Theme.FIELD_BORDER);
		g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 4f, 4f }, 0f));
		g2.drawRoundRect(0, 0, ancho - 1, alto - 1, 8, 8);

		g2.setFont(Typography.label(10f));
		g2.setColor(Theme.mut());

		String texto = "OLAZ";
		String subtexto = estacion.nombre().toUpperCase();

		int anchoTexto = g2.getFontMetrics().stringWidth(texto);
		int anchoSub = g2.getFontMetrics().stringWidth(subtexto);

		g2.drawString(texto, (ancho - anchoTexto) / 2, alto / 2 - 2);
		g2.drawString(subtexto, (ancho - anchoSub) / 2, alto / 2 + 14);
	}
}
