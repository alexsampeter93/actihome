package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Hueco de imagen de un alojamiento.
 *
 * <p>
 * <b>No es un parche por no tener fotos.</b> Aunque mañana hubiera doscientas,
 * cualquier alojamiento que un administrador publique desde la aplicación no
 * tendría ninguna, porque no existe flujo de subida. Así que esta pieza hace
 * falta igual, y por eso está diseñada en vez de ser un rectángulo gris: color
 * base de la estación, textura sutil, y las etiquetas de tipo y estado
 * superpuestas.
 *
 * <p>
 * Cuando haya foto real, se pinta la foto y encima el <b>velo</b> de la
 * estación ({@code imgTint}). Ese velo es lo que hace que seis fotos de seis
 * fotógrafos distintos, con luces y saturaciones distintas, parezcan parte de
 * un mismo catálogo. Es un truco viejo de dirección de arte editorial y es de
 * las cosas que más diferencia un catálogo cuidado de un collage.
 */
public class ImagePlaceholder extends JComponent {

	private static final long serialVersionUID = 1L;

	private String tipo;
	private String estado;
	private boolean disponible = true;
	private transient BufferedImage foto;

	public ImagePlaceholder() {

		// Un JComponent sin tamaño preferido mide cero, y un layout que no reciba una
		// pista lo dibujaría como una línea. Este valor es solo el punto de partida:
		// lo normal es que el layout de la pantalla lo estire.
		setPreferredSize(new Dimension(240, 150));
	}

	public ImagePlaceholder(String tipo, String estado, boolean disponible) {

		this();
		this.tipo = tipo;
		this.estado = estado;
		this.disponible = disponible;
	}

	/** Etiqueta de tipo, arriba a la izquierda ("Casa", "Villa"...). */
	public void setTipo(String tipo) {
		this.tipo = tipo;
		repaint();
	}

	/** Etiqueta de estado, abajo a la izquierda ("Disponible", "Reservada"). */
	public void setEstado(String estado, boolean disponible) {
		this.estado = estado;
		this.disponible = disponible;
		repaint();
	}

	/** Foto real. Si es {@code null} se pinta el hueco diseñado. */
	public void setFoto(BufferedImage foto) {
		this.foto = foto;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		int ancho = getWidth();
		int alto = getHeight();

		if (foto != null) {
			g2.drawImage(foto, 0, 0, ancho, alto, null);
		} else {
			g2.setColor(Theme.img());
			g2.fillRect(0, 0, ancho, alto);
			pintarTextura(g2, ancho, alto);
		}

		// El velo de la estación va siempre: sobre la foto la integra en el catálogo,
		// y sobre el hueco vacío le da profundidad.
		g2.setColor(Theme.imgTint());
		g2.fillRect(0, 0, ancho, alto);

		if (tipo != null) {
			pintarEtiqueta(g2, tipo, Space.SM, Space.SM, new Color(0, 0, 0, 107), Color.WHITE);
		}

		if (estado != null) {
			int altoEtiqueta = 24;
			pintarEtiqueta(g2, estado, Space.SM, alto - Space.SM - altoEtiqueta,
					disponible ? Theme.acc() : new Color(0, 0, 0, 107), Theme.ON_ACCENT);
		}

		g2.setColor(Theme.HAIRLINE);
		g2.drawRect(0, 0, ancho - 1, alto - 1);

		g2.dispose();
	}

	/**
	 * Trama diagonal muy tenue. Sin ella, un rectángulo de color plano se lee como
	 * "aquí falta algo"; con ella se lee como una superficie intencionada.
	 */
	private void pintarTextura(Graphics2D g2, int ancho, int alto) {

		g2.setColor(new Color(0, 0, 0, 10));

		for (int x = -alto; x < ancho; x += 14) {
			g2.drawLine(x, alto, x + alto, 0);
		}
	}

	private void pintarEtiqueta(Graphics2D g2, String texto, int x, int y, Color fondo, Color tinta) {

		g2.setFont(Typography.label(10f));
		int anchoTexto = g2.getFontMetrics().stringWidth(texto.toUpperCase());
		int anchoCaja = anchoTexto + Space.SM * 2;
		int altoCaja = 24;

		g2.setColor(fondo);
		g2.fillRect(x, y, anchoCaja, altoCaja);

		g2.setColor(tinta);
		g2.drawString(texto.toUpperCase(), x + Space.SM, y + altoCaja - 8);
	}
}
