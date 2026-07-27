package fp.project.actihome.ui.theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Chip;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.ImagePlaceholder;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.ScoreBar;
import fp.project.actihome.ui.components.ScoreDisc;

/**
 * El contenido de la guía de estilo: paleta, tipografía, espaciado,
 * componentes y controles estándar.
 *
 * <p>
 * Está separado de la ventana que lo muestra ({@link ThemePreview}) a
 * propósito, porque tiene dos consumidores: la ventana interactiva y el
 * generador de capturas {@link ThemeSnapshots}, que lo dibuja directamente a un
 * PNG sin abrir ninguna ventana. Un panel que solo sabe pintarse a sí mismo
 * sirve para las dos cosas; un {@code JFrame} que además construye su contenido
 * solo sirve para una.
 */
public class StyleGuidePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Si es {@code true}, incluye el selector de estación (no tiene sentido en una captura). */
	private final boolean interactivo;

	public StyleGuidePanel(boolean interactivo) {

		this.interactivo = interactivo;
		construir();
	}

	/** Rehace el contenido con los colores de la estación activa. */
	public final void construir() {

		removeAll();
		setLayout(new MigLayout("fillx, wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		setBackground(Theme.bg());

		add(cabecera(), "growx");

		if (interactivo) {
			add(selectorDeEstacion(), "growx, gaptop " + Space.XXL + ", gapx " + Space.XXXL);
		} else {
			add(Labels.editorial(Theme.estacion().etiqueta()), "gaptop " + Space.XXL + ", gapx " + Space.XXXL);
		}

		add(seccion("Paleta"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		add(paleta(), "growx, gapx " + Space.XXXL);
		add(seccion("Tipografía"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		add(tipografia(), "growx, gapx " + Space.XXXL);
		add(seccion("Espaciado"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		add(espaciado(), "growx, gapx " + Space.XXXL);
		add(seccion("Componentes"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		add(componentes(), "growx, gapx " + Space.XXXL);
		add(seccion("Controles estándar"), "gapx " + Space.XXXL + ", gaptop " + Space.XXL);
		add(controles(), "growx, gapx " + Space.XXXL + ", gapbottom " + Space.GIANT);

		revalidate();
		repaint();
	}

	// ------------------------------------------------------------------
	// Bloques
	// ------------------------------------------------------------------

	private JPanel cabecera() {

		JPanel panel = new JPanel(
				new MigLayout(Space.insets(Space.XL, Space.XXXL, Space.XL, Space.XXXL), "[]push[]", ""));
		panel.setBackground(Theme.hdr());

		JLabel marca = new JLabel("ActiHome");
		marca.setFont(Typography.serifMedium(26f));
		marca.setForeground(Theme.bg());
		panel.add(marca);

		panel.add(Labels.capsOnHeader("Guía de estilo · por CocoBrain"));

		return panel;
	}

	private JPanel selectorDeEstacion() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.SM + "[]" + Space.SM + "[]" + Space.SM + "[]push[]", ""));
		panel.setOpaque(false);

		for (Season estacion : Season.values()) {

			Chip chip = new Chip(estacion.nombre(), estacion == Theme.estacion());
			chip.addActionListener(e -> Theme.cambiarA(estacion));
			panel.add(chip);
		}

		panel.add(Labels.editorial(Theme.estacion().etiqueta()));

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

		panel.add(Labels.hero("Elige dónde quieres despertar"));
		panel.add(pie("Spectral Medium · 46 · hero"), "gapbottom " + Space.XL);

		panel.add(Labels.title("Casa Rural El Pinar"));
		panel.add(pie("Spectral Medium · 32 · título de pantalla"), "gapbottom " + Space.XL);

		panel.add(Labels.price("75,00 €"));
		panel.add(pie("Spectral Regular · 34 · precio"), "gapbottom " + Space.XL);

		panel.add(Labels.body("Un caserón de piedra a media hora de la sierra, con chimenea y "
				+ "vistas al valle. El desayuno se sirve en el porche."));
		panel.add(pie("Manrope Regular · 15 · cuerpo"), "gapbottom " + Space.XL);

		panel.add(Labels.caps("Disponible · 3 habitaciones · Sierra Nevada"));
		panel.add(pie("Manrope SemiBold · 11 · versalita con tracking 0,18em"));

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
			panel.add(pie(nombres[i] + " · " + valores[i]));
			panel.add(barra(valores[i]), "gapbottom " + Space.XS);
		}

		return panel;
	}

	private JPanel componentes() {

		JPanel panel = new JPanel(
				new MigLayout("wrap 2, " + Space.insets(Space.XXL), "[160!]" + Space.XL + "[grow,fill]", ""));
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(BorderFactory.createLineBorder(Theme.HAIRLINE));

		panel.add(rotulo("Botones"));
		JPanel botones = fila();
		botones.add(Buttons.primary("Reservar", null));
		botones.add(Buttons.secondary("Cancelar", null));
		botones.add(Buttons.link("Ver estancia →", null));
		botones.add(Buttons.linkAccent("Intercambiar ⇄", null));
		panel.add(botones);

		panel.add(Hairline.horizontal(), "span 2, growx, gapy " + Space.LG);

		panel.add(rotulo("Chips"));
		JPanel chips = fila();
		chips.add(new Chip("Todos", true));
		chips.add(new Chip("Casa"));
		chips.add(new Chip("Apartamento"));
		chips.add(Chip.informativo("Wifi"));
		chips.add(Chip.informativo("Piscina"));
		panel.add(chips);

		panel.add(Hairline.horizontal(), "span 2, growx, gapy " + Space.LG);

		panel.add(rotulo("Campos"));
		JPanel campos = new JPanel(new MigLayout(Space.insets(0), "[260!]" + Space.LG + "[260!]", ""));
		campos.setOpaque(false);
		campos.add(Field.text("Nombre de usuario", "alejandro"), "growx");
		campos.add(Field.password("Contraseña"), "growx");
		panel.add(campos);

		panel.add(Hairline.horizontal(), "span 2, growx, gapy " + Space.LG);

		panel.add(rotulo("Puntuaciones"));
		JPanel notas = new JPanel(new MigLayout(Space.insets(0),
				"[]" + Space.MD + "[]" + Space.MD + "[]" + Space.MD + "[]" + Space.XXL + "[grow,fill]", ""));
		notas.setOpaque(false);
		notas.add(new ScoreDisc(4.2, ScoreDisc.Tamano.PEQUENO));
		notas.add(new ScoreDisc(4.8, ScoreDisc.Tamano.MEDIANO));
		notas.add(new ScoreDisc(3.4, ScoreDisc.Tamano.GRANDE));
		notas.add(new ScoreDisc(null, ScoreDisc.Tamano.MEDIANO));

		JPanel barras = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		barras.setOpaque(false);
		barras.add(new ScoreBar("Ubicación", 4.6));
		barras.add(new ScoreBar("Servicio", 3.8), "gaptop " + Space.XS);
		barras.add(new ScoreBar("Wifi", 5.0), "gaptop " + Space.XS);
		barras.add(new ScoreBar("Comida", 2.4), "gaptop " + Space.XS);
		barras.add(new ScoreBar("Limpieza", 4.1), "gaptop " + Space.XS);
		notas.add(barras, "growx");
		panel.add(notas);

		panel.add(Hairline.horizontal(), "span 2, growx, gapy " + Space.LG);

		panel.add(rotulo("Imagen y mascota"));
		JPanel medios = new JPanel(new MigLayout(Space.insets(0),
				"[240!]" + Space.LG + "[150!]" + Space.LG + "[]" + Space.MD + "[]" + Space.MD + "[]", ""));
		medios.setOpaque(false);
		medios.add(new ImagePlaceholder("Casa", "Disponible", true), "growx, height 150!");
		medios.add(new ImagePlaceholder("Villa", "Reservada", false), "growx, height 150!");
		medios.add(new MascotSlot(MascotSlot.Tamano.GRANDE), "bottom");
		medios.add(new MascotSlot(MascotSlot.Tamano.MEDIANO), "bottom");
		medios.add(new MascotSlot(MascotSlot.Tamano.PEQUENO), "bottom");
		panel.add(medios);

		return panel;
	}

	private JPanel controles() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.XXL),
				"[]" + Space.SM + "[220!]" + Space.SM + "[]" + Space.SM + "[]", ""));
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(BorderFactory.createLineBorder(Theme.HAIRLINE));

		panel.add(new javax.swing.JButton("JButton de serie"));
		panel.add(new JTextField("JTextField de serie"), "growx");
		panel.add(new JCheckBox("Desayuno", true));
		panel.add(new JComboBox<>(new String[] { "Casa", "Apartamento", "Villa", "Cabaña" }));

		return panel;
	}

	// ------------------------------------------------------------------
	// Piezas auxiliares
	// ------------------------------------------------------------------

	private JLabel seccion(String titulo) {
		return Labels.capsAccent(titulo);
	}

	private JLabel rotulo(String titulo) {

		JLabel etiqueta = Labels.body(titulo);
		etiqueta.setFont(Typography.sansSemiBold(13f));
		return etiqueta;
	}

	/** Pie explicativo en gris pequeño. */
	private JLabel pie(String texto) {

		JLabel etiqueta = Labels.muted(texto);
		etiqueta.setFont(Typography.sans(12f));
		return etiqueta;
	}

	private JPanel fila() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "", ""));
		fila.setOpaque(false);
		return fila;
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

		JLabel nombre = Labels.body(token);
		nombre.setFont(Typography.sansSemiBold(13f));
		panel.add(nombre, "gaptop " + Space.XS);
		panel.add(pie(papel));
		panel.add(pie(Season.hex(color) + (color.getAlpha() < 255 ? " · " + pct(color.getAlpha()) : "")));

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
}
