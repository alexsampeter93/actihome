package fp.project.actihome.ui.components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

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
 * </ol>
 *
 * <p>
 * <b>Ausencia elegante.</b> Las ilustraciones actuales tienen el fondo blanco
 * horneado y no sirven para componer sobre la interfaz; faltan las versiones
 * con transparencia. Mientras no lleguen, la ranura dibuja un contorno
 * discontinuo con el nombre de la variante que irá ahí. Así el trabajo de
 * maquetación puede avanzar con el espacio ya reservado, y el día que se
 * copien los archivos a {@code resources/images/olaz/} aparecen sin tocar una
 * línea de código. Un componente que se rompe porque falta un recurso obliga a
 * parar; uno que degrada bien, no.
 */
public class MascotSlot extends JComponent {

	private static final long serialVersionUID = 1L;

	/** Ruta esperada dentro del jar: /images/olaz/olaz-verano.png, etc. */
	private static final String RUTA = "/images/olaz/olaz-%s.png";

	/**
	 * Caché de imágenes ya cargadas. Sin ella se leería el archivo del disco en
	 * cada repintado, es decir, decenas de veces por segundo al redimensionar.
	 */
	private static final Map<Season, BufferedImage> CACHE = new EnumMap<>(Season.class);

	/** Estaciones cuya imagen ya se ha buscado y no existe: no se reintenta. */
	private static final Map<Season, Boolean> NO_ENCONTRADA = new EnumMap<>(Season.class);

	public enum Tamano {

		PEQUENO(72), MEDIANO(140), GRANDE(220);

		private final int lado;

		Tamano(int lado) {
			this.lado = lado;
		}
	}

	private final Tamano tamano;

	public MascotSlot(Tamano tamano) {

		this.tamano = tamano;
		setPreferredSize(new Dimension(tamano.lado, tamano.lado));
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		Season estacion = Theme.estacion();
		BufferedImage imagen = cargar(estacion);

		if (imagen != null) {
			pintarAjustada(g2, imagen);
		} else {
			pintarHueco(g2, estacion);
		}

		g2.dispose();
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

	private static BufferedImage cargar(Season estacion) {

		if (CACHE.containsKey(estacion)) {
			return CACHE.get(estacion);
		}

		if (Boolean.TRUE.equals(NO_ENCONTRADA.get(estacion))) {
			return null;
		}

		String ruta = String.format(RUTA, estacion.name().toLowerCase());

		try (InputStream in = MascotSlot.class.getResourceAsStream(ruta)) {

			if (in == null) {
				NO_ENCONTRADA.put(estacion, Boolean.TRUE);
				return null;
			}

			BufferedImage imagen = ImageIO.read(in);
			CACHE.put(estacion, imagen);
			return imagen;

		} catch (IOException e) {
			System.err.println("[MascotSlot] No se pudo leer " + ruta + ": " + e.getMessage());
			NO_ENCONTRADA.put(estacion, Boolean.TRUE);
			return null;
		}
	}
}
