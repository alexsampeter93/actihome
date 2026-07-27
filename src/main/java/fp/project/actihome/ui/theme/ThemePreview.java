package fp.project.actihome.ui.theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

/**
 * Guía de estilo viva del sistema de diseño de ActiHome.
 *
 * <p>
 * Muestra en una sola pantalla la paleta de la estación activa, la escala
 * tipográfica, la escala de espaciado y cómo quedan los controles estándar de
 * Swing con el tema aplicado. El selector de arriba cambia la estación y
 * repinta todo al instante.
 *
 * <p>
 * No forma parte de la aplicación: es una herramienta de desarrollo. Sirve para
 * tres cosas, y las tres importan:
 * <ul>
 * <li><b>Verificar</b> que un cambio en el sistema de diseño se propaga como se
 * espera, sin tener que arrancar la aplicación entera ni la base de datos.</li>
 * <li><b>Decidir</b> con los ojos en vez de con la imaginación: los colores
 * sobre una tabla de Markdown no se parecen a los colores sobre una pantalla
 * real.</li>
 * <li><b>Documentar</b>: cuando dentro de tres meses haya que añadir una
 * pantalla, esto dice qué piezas hay disponibles.</li>
 * </ul>
 *
 * <p>
 * Se arranca sin Spring ni MySQL:
 *
 * <pre>
 * .\mvnw.cmd compile exec:java
 * </pre>
 */
public class ThemePreview extends JFrame {

	private static final long serialVersionUID = 1L;

	private final JPanel contenido = new JPanel();

	public ThemePreview() {

		setTitle("ActiHome — Guía de estilo");
		setSize(1180, 900);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		add(new JScrollPane(contenido));
		construir();

		// Al cambiar de estación se reconstruye todo. Reconstruir es más caro que
		// repintar, pero en una herramienta de desarrollo la simplicidad gana: así no
		// hay que mantener una lista de qué componente depende de qué token.
		Theme.alCambiar(estacion -> construir());
	}

	private void construir() {

		contenido.removeAll();
		contenido.setLayout(new MigLayout("fillx, wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		contenido.setBackground(Theme.bg());

		contenido.add(cabecera(), "growx");
		contenido.add(selectorDeEstacion(), "growx, gaptop " + Space.XXL + ", gapx " + Space.XXXL);
		contenido.add(seccion("Paleta"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		contenido.add(paleta(), "growx, gapx " + Space.XXXL);
		contenido.add(seccion("Tipografía"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		contenido.add(tipografia(), "growx, gapx " + Space.XXXL);
		contenido.add(seccion("Espaciado"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		contenido.add(espaciado(), "growx, gapx " + Space.XXXL);
		contenido.add(seccion("Controles estándar"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		contenido.add(controles(), "growx, gapx " + Space.XXXL + ", gapbottom " + Space.GIANT);

		contenido.revalidate();
		contenido.repaint();
	}

	// ------------------------------------------------------------------
	// Bloques
	// ------------------------------------------------------------------

	/** Barra oscura con el wordmark, como la tendrá la aplicación. */
	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.XL, Space.XXXL, Space.XL, Space.XXXL), "[]push[]", ""));
		panel.setBackground(Theme.hdr());

		JLabel marca = new JLabel("ActiHome");
		marca.setFont(Typography.serifMedium(26f));
		marca.setForeground(Theme.bg());
		panel.add(marca);

		JLabel firma = new JLabel("GUÍA DE ESTILO · POR COCOBRAIN");
		firma.setFont(Typography.label());
		firma.setForeground(Theme.mutSobreOscuro());
		panel.add(firma);

		return panel;
	}

	private JPanel selectorDeEstacion() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]push[]", ""));
		panel.setOpaque(false);

		for (Season estacion : Season.values()) {

			JButton boton = new JButton(estacion.nombre());
			boton.setFont(Typography.sansSemiBold(13f));
			boton.addActionListener(e -> Theme.cambiarA(estacion));

			if (estacion == Theme.estacion()) {
				boton.setBackground(Theme.acc());
				boton.setForeground(Theme.ON_ACCENT);
			}

			panel.add(boton);
		}

		JLabel etiqueta = new JLabel(Theme.estacion().etiqueta());
		etiqueta.setFont(Typography.serifItalic(16f));
		etiqueta.setForeground(Theme.acc());
		panel.add(etiqueta);

		return panel;
	}

	private JPanel paleta() {

		JPanel panel = new JPanel(new MigLayout("wrap 7, " + Space.insets(0), "[]" + Space.SM + "[]" + Space.SM + "[]"
				+ Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]", ""));
		panel.setOpaque(false);

		Season s = Theme.estacion();
		panel.add(muestra(s.acc(), "acc", "Acento"));
		panel.add(muestra(s.bg(), "bg", "Fondo"));
		panel.add(muestra(s.hdr(), "hdr", "Cabecera"));
		panel.add(muestra(s.txt(), "txt", "Texto"));
		panel.add(muestra(s.mut(), "mut", "Secundario"));
		panel.add(muestra(s.img(), "img", "Hueco de foto"));
		panel.add(muestra(s.imgTint(), "imgTint", "Velo de foto"));

		return panel;
	}

	private JPanel tipografia() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(Space.XXL), "[grow,fill]", ""));
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(BorderFactory.createLineBorder(Theme.HAIRLINE));

		panel.add(texto("Elige dónde quieres despertar", Typography.serifMedium(Typography.HERO), Theme.txt()));
		panel.add(texto("Spectral Medium · 46 · hero", Typography.sans(12f), Theme.mut()), "gapbottom " + Space.XL);

		panel.add(texto("Casa Rural El Pinar", Typography.serifMedium(Typography.SCREEN_TITLE), Theme.txt()));
		panel.add(texto("Spectral Medium · 32 · título de pantalla", Typography.sans(12f), Theme.mut()),
				"gapbottom " + Space.XL);

		panel.add(texto("75,00 €", Typography.serif(Typography.PRICE_LG), Theme.acc()));
		panel.add(texto("Spectral Regular · 34 · precio", Typography.sans(12f), Theme.mut()), "gapbottom " + Space.XL);

		panel.add(texto("Un caserón de piedra a media hora de la sierra, con chimenea y "
				+ "vistas al valle. El desayuno se sirve en el porche.", Typography.sans(Typography.BODY), Theme.txt()));
		panel.add(texto("Manrope Regular · 15 · cuerpo", Typography.sans(12f), Theme.mut()), "gapbottom " + Space.XL);

		panel.add(texto("DISPONIBLE · 3 HABITACIONES · SIERRA NEVADA", Typography.label(), Theme.mut()));
		panel.add(texto("Manrope SemiBold · 11 · versalita con tracking 0,18em", Typography.sans(12f), Theme.mut()));

		return panel;
	}

	private JPanel espaciado() {

		JPanel panel = new JPanel(new MigLayout("wrap 2, " + Space.insets(Space.XXL), "[120!][grow,fill]", ""));
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(BorderFactory.createLineBorder(Theme.HAIRLINE));

		int[] valores = { Space.XXS, Space.XS, Space.SM, Space.MD, Space.LG, Space.XL, Space.XXL, Space.XXXL,
				Space.HUGE, Space.GIANT, Space.MAX };
		String[] nombres = { "XXS", "XS", "SM", "MD", "LG", "XL", "XXL", "XXXL", "HUGE", "GIANT", "MAX" };

		for (int i = 0; i < valores.length; i++) {
			panel.add(texto(nombres[i] + " · " + valores[i], Typography.sans(12f), Theme.mut()));
			panel.add(barra(valores[i]), "gapbottom " + Space.XS);
		}

		return panel;
	}

	private JPanel controles() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.XXL), "[]" + Space.SM + "[]" + Space.SM + "[220!]"
				+ Space.SM + "[]" + Space.SM + "[]", ""));
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(BorderFactory.createLineBorder(Theme.HAIRLINE));

		JButton principal = new JButton("Reservar");
		principal.putClientProperty("JButton.buttonType", "default");
		panel.add(principal);

		panel.add(new JButton("Cancelar"));

		JTextField campo = new JTextField("Buscar por nombre o ubicación");
		panel.add(campo, "growx");

		panel.add(new JCheckBox("Desayuno", true));
		panel.add(new JComboBox<>(new String[] { "Casa", "Apartamento", "Villa", "Cabaña" }));

		return panel;
	}

	// ------------------------------------------------------------------
	// Piezas auxiliares
	// ------------------------------------------------------------------

	private JLabel seccion(String titulo) {

		JLabel etiqueta = new JLabel(titulo.toUpperCase());
		etiqueta.setFont(Typography.label(12f));
		etiqueta.setForeground(Theme.acc());
		return etiqueta;
	}

	private JLabel texto(String contenidoTexto, java.awt.Font fuente, Color color) {

		JLabel etiqueta = new JLabel(contenidoTexto);
		etiqueta.setFont(fuente);
		etiqueta.setForeground(color);
		return etiqueta;
	}

	/** Muestra de color con su nombre de token, su papel y su valor hexadecimal. */
	private JPanel muestra(Color color, String token, String papel) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[140!]", ""));
		panel.setOpaque(false);

		JPanel tarjeta = new JPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {

				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// Los tokens translúcidos (imgTint) se pintan sobre un tablero de ajedrez
				// claro, que es la convención para representar transparencia.
				if (color.getAlpha() < 255) {
					pintarTablero(g2);
				}

				g2.setColor(color);
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(Theme.HAIRLINE);
				g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
				g2.dispose();
			}

			private void pintarTablero(Graphics2D g2) {

				int lado = 8;
				for (int y = 0; y < getHeight(); y += lado) {
					for (int x = 0; x < getWidth(); x += lado) {
						boolean par = ((x / lado) + (y / lado)) % 2 == 0;
						g2.setColor(par ? Color.WHITE : new Color(0xE8E8E8));
						g2.fillRect(x, y, lado, lado);
					}
				}
			}
		};
		tarjeta.setPreferredSize(new Dimension(140, 76));
		panel.add(tarjeta, "growx");

		panel.add(texto(token, Typography.sansSemiBold(13f), Theme.txt()), "gaptop " + Space.XS);
		panel.add(texto(papel, Typography.sans(11f), Theme.mut()));
		panel.add(texto(Season.hex(color) + (color.getAlpha() < 255 ? " · " + pct(color.getAlpha()) : ""),
				Typography.sans(11f), Theme.mut()));

		return panel;
	}

	private static String pct(int alfa) {
		return Math.round(alfa * 100f / 255f) + "%";
	}

	/** Barra horizontal de ancho igual al peldaño de espaciado, para verlo a escala. */
	private JPanel barra(int ancho) {

		JPanel panel = new JPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(Theme.acc());
				g.fillRect(0, getHeight() / 2 - 5, ancho, 10);
			}
		};
		panel.setOpaque(false);
		panel.setPreferredSize(new Dimension(ancho, 18));
		return panel;
	}

	public static void main(String[] args) {

		ActiHomeTheme.install();
		EventQueue.invokeLater(() -> new ThemePreview().setVisible(true));
	}

	static {
		// Silencia la alineación por defecto de los JLabel dentro de MigLayout.
		javax.swing.UIManager.put("Label.horizontalAlignment", SwingConstants.LEADING);
	}
}
