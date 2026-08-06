package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import fp.project.actihome.ui.theme.BrandAssets;
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
	private String destacado;

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

	/**
	 * Con la foto del alojamiento, si la tiene.
	 *
	 * <p>
	 * Recibe el <b>nombre del archivo</b> y no la entidad {@code Housing}. Es
	 * deliberado: este componente vive en el vocabulario visual del sistema y no
	 * debe conocer el modelo de negocio, o dejaría de poder usarse —y probarse—
	 * fuera de esta aplicación. Quien lo construye ya tiene el alojamiento delante
	 * y le cuesta lo mismo pasarle {@code housing.getImage()}.
	 */
	public ImagePlaceholder(String tipo, String estado, boolean disponible, String imagen) {

		this(tipo, estado, disponible);
		this.foto = BrandAssets.fotoDeAlojamiento(imagen);
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

	/**
	 * Distintivo de la esquina superior derecha, en el acento de la estación
	 * ("Ideal en primavera"). {@code null} para no pintar ninguno (Fase 8.4).
	 *
	 * <p>
	 * Es un texto y no un enumerado de estación a propósito: este componente vive
	 * en el vocabulario visual del sistema y no debe saber qué es una estación
	 * ideal ni cómo se decide. Quien lo construye ya tiene esa lógica delante.
	 */
	public void setDestacado(String destacado) {
		this.destacado = destacado;
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
			pintarCubriendo(g2, ancho, alto);
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

		int altoEtiqueta = altoDeEtiqueta(g2);

		if (estado != null) {
			pintarEtiqueta(g2, estado, Space.SM, alto - Space.SM - altoEtiqueta,
					disponible ? Theme.acc() : new Color(0, 0, 0, 107), Theme.onAccent());
		}

		// **Abajo a la derecha, y antes estaba arriba a la derecha por un error de
		// premisa.** El comentario que había aquí decía "la esquina que quedaba libre",
		// y esa esquina no está libre: en la vista de cuadrícula, `HousingCard` ancla
		// ahí el disco de puntuación. El resultado era que el disco tapaba el final del
		// texto y en pantalla se leía "IDEAL EN VER" — un distintivo cortado, que se
		// interpreta como un fallo de la aplicación y no como lo que es.
		//
		// No lo detectó ninguna herramienta, y merece anotarse por qué: `MedirResponsive`
		// busca componentes fuera del área visible, y aquí no hay ningún componente
		// fuera de nada. Son dos cosas **dibujadas** en las mismas coordenadas, una
		// dentro de un `paintComponent` y la otra colocada por MigLayout. Nadie las ve
		// a la vez salvo el ojo.
		//
		// El ancho se sigue midiendo en vez de fijarse, porque el texto cambia con la
		// estación y con el idioma ("Ideal en primavera" / "Perfect in spring").
		if (destacado != null) {

			int anchoDestacado = anchoDeEtiqueta(g2, destacado);
			int x = ancho - Space.SM - anchoDestacado;
			int y = alto - Space.SM - altoEtiqueta;

			// Y si tampoco cabe al lado del estado —fotos estrechas, textos largos en
			// inglés, escalado del sistema al 150 %— sube una fila en lugar de solaparse.
			// Es el mismo criterio que en el resto del proyecto: cuando falta sitio, lo
			// que cede es la composición, nunca la legibilidad de un dato.
			int finDelEstado = estado != null ? Space.SM + anchoDeEtiqueta(g2, estado) : 0;

			if (x < finDelEstado + Space.XS) {
				y -= altoEtiqueta + Space.XS;
			}

			pintarEtiqueta(g2, destacado, x, y, Theme.acc(), Theme.onAccent());
		}

		g2.setColor(Theme.HAIRLINE);
		g2.drawRect(0, 0, ancho - 1, alto - 1);

		g2.dispose();
	}

	/**
	 * Dibuja la foto <b>cubriendo</b> el hueco: se escala hasta llenarlo
	 * conservando su proporción y se recorta lo que sobra.
	 *
	 * <p>
	 * La alternativa —estirarla hasta que encaje, que es lo que hace
	 * {@code drawImage} con un ancho y un alto cualesquiera— deforma la fotografía.
	 * Y se nota especialmente aquí, porque la misma foto aparece en una ficha
	 * apaisada en la vista de lista y en un cuadrado en la de cuadrícula: estirada,
	 * las dos versiones parecerían edificios distintos.
	 *
	 * <p>
	 * El recorte se centra en el eje que sobra, salvo en vertical, donde se sube un
	 * poco el encuadre: en una foto de una casa, lo interesante suele estar en el
	 * tercio superior y el inferior suele ser suelo.
	 */
	private void pintarCubriendo(Graphics2D g2, int ancho, int alto) {

		double escala = Math.max((double) ancho / foto.getWidth(), (double) alto / foto.getHeight());

		int nuevoAncho = (int) Math.ceil(foto.getWidth() * escala);
		int nuevoAlto = (int) Math.ceil(foto.getHeight() * escala);

		int x = (ancho - nuevoAncho) / 2;
		int y = (int) ((alto - nuevoAlto) * 0.38);

		Shape recorte = g2.getClip();
		g2.clipRect(0, 0, ancho, alto);
		g2.drawImage(foto, x, y, nuevoAncho, nuevoAlto, null);
		g2.setClip(recorte);
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

	/** Lo que va a ocupar una etiqueta, para poder anclarla a un borde derecho. */
	/** El cuerpo de las etiquetas que van encima de la foto. */
	private static final float CUERPO_DE_ETIQUETA = 10f;

	private int anchoDeEtiqueta(Graphics2D g2, String texto) {

		g2.setFont(Typography.label(CUERPO_DE_ETIQUETA));

		return g2.getFontMetrics().stringWidth(texto.toUpperCase()) + Space.SM * 2;
	}

	/**
	 * El alto de una etiqueta, <b>medido</b> y no fijado en 24 píxeles como estaba.
	 *
	 * <p>
	 * Era uno de los altos escritos a mano que quedaban vivos: el 24 salía de mirar
	 * una captura en un equipo sin escalado, y en un Windows al 150 % la letra crece
	 * pero la caja no, así que el texto quedaba pegado a los bordes y luego cortado.
	 * Es la regla más repetida del proyecto — ningún tamaño que dependa de texto
	 * puede ser una constante— y esta pieza se la había saltado desde la Fase 3.
	 */
	private int altoDeEtiqueta(Graphics2D g2) {

		g2.setFont(Typography.label(CUERPO_DE_ETIQUETA));

		return g2.getFontMetrics().getHeight() + Space.SM;
	}

	private void pintarEtiqueta(Graphics2D g2, String texto, int x, int y, Color fondo, Color tinta) {

		g2.setFont(Typography.label(CUERPO_DE_ETIQUETA));

		int anchoCaja = anchoDeEtiqueta(g2, texto);
		int altoCaja = altoDeEtiqueta(g2);

		g2.setColor(fondo);
		g2.fillRect(x, y, anchoCaja, altoCaja);

		// La línea base se calcula desde la métrica real: el descendente es lo que hay
		// que dejar por debajo para que las letras con cola (g, p, j) no se corten, y
		// centrar de verdad exige repartir el sobrante entre arriba y abajo.
		int descendente = g2.getFontMetrics().getDescent();
		int sobrante = altoCaja - g2.getFontMetrics().getHeight();

		g2.setColor(tinta);
		g2.drawString(texto.toUpperCase(), x + Space.SM, y + altoCaja - descendente - sobrante / 2);
	}
}
