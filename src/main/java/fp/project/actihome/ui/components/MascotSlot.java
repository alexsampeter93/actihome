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
 * Olaz aparece en todas las pantallas por decisión de producto. Para que sume
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

	public enum Tamano {

		PEQUENO(72), MEDIANO(140), GRANDE(220);

		private final int lado;

		Tamano(int lado) {
			this.lado = lado;
		}
	}

	private final Tamano tamano;
	private final Pose pose;

	public MascotSlot(Tamano tamano, Pose pose) {

		this.tamano = tamano;
		this.pose = pose;
		setPreferredSize(new Dimension(tamano.lado, tamano.lado));
		setMinimumSize(new Dimension(tamano.lado / 2, tamano.lado / 2));
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

		if (estacion == Season.VERANO && tamano != Tamano.PEQUENO) {
			return Pose.ACCION;
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
