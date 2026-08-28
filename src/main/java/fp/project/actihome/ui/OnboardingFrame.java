package fp.project.actihome.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.User;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.services.UserService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Bienvenida de una sola vez, la primera vez que se <b>inicia sesión</b> con una
 * cuenta.
 *
 * <p>
 * <b>No sale al registrarse</b>, aunque lo parezca: {@code SignUpFrame} devuelve
 * al login en cuanto crea la cuenta, y es {@code LoginFrame.entrar()} quien mira
 * {@code User.isOnboardingSeen()} y decide si toca esta pantalla o el buscador.
 * La diferencia importa porque también la ve quien ya tenía cuenta: la bandera
 * nace sin marcar para todo el mundo, no sólo para los recién llegados.
 *
 * <p>
 * <b>Tres pasos y no uno.</b> El párrafo único original cabía en una pantalla y
 * por eso mismo no contaba nada: intentaba resumir en cuatro líneas qué es la
 * aplicación, qué guarda tu cuenta y qué se puede configurar. Partirlo deja que
 * cada paso diga una sola cosa y que la ilustración acompañe —Olaz saluda en el
 * primero y está haciendo algo en los otros dos—, que es exactamente para lo que
 * existen las dos poses.
 *
 * <p>
 * <b>Y sale por donde entró.</b> Hasta ahora "Empezar" llevaba siempre al
 * buscador, lo cual era correcto mientras el único camino hasta aquí fuera el
 * primer login. Desde que Ajustes tiene un botón para volver a verla hay dos, y
 * un destino fijo convierte el segundo en un viaje sin retorno: quien la abría
 * para repasarla acababa en el buscador, con Ajustes a medio revisar. Es el
 * mismo fallo que {@code Navigator.volver} vino a resolver — y no vale usar
 * {@code volver} a secas, porque en el primer login la pantalla anterior es el
 * login, y devolver ahí a alguien que acaba de entrar sería peor todavía.
 *
 * <p>
 * Es de las pocas pantallas que usa {@link BrandAssets#fondo()} en vez del color
 * plano de la estación activa: ese fondo decorativo está reservado para esto y
 * para el splash de arranque (CLAUDE.md §6) — su paleta cálida y fija competiría
 * con las tarjetas si se usara en el catálogo o en cualquier listado.
 *
 * <p>
 * Sin cabecera y sin atajo de Escape, por la misma razón que {@code LoginFrame}:
 * es un destino, no un paso de un formulario con un "atrás" que tenga sentido.
 */
@Component
@Profile("!test")
@Lazy
public class OnboardingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Cuántos pasos tiene la presentación. */
	private static final int PASOS = 3;

	private final transient UserService userService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private JLabel superTitulo;
	private JLabel titulo;
	private WrappingText cuerpo;
	private MascotSlot mascota;
	private Puntos puntos;
	private JButton botonSiguiente;
	private JButton enlaceSaltar;

	/** En qué paso estamos, de 1 a {@link #PASOS}. */
	private int paso = 1;

	/**
	 * Si al terminar hay que volver a la pantalla anterior en vez de ir al
	 * buscador. Lo pone Ajustes; el primer inicio de sesión no lo toca.
	 */
	private boolean volverAlSalir;

	public OnboardingFrame(UserService userService, SessionManager sessionManager, Navigator navigator) {

		this.userService = userService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	/** Ajustes la abre así, para que al terminar se vuelva a Ajustes y no al buscador. */
	public void setVolverAlSalir(boolean volverAlSalir) {
		this.volverAlSalir = volverAlSalir;
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {

			// Siempre por el principio. Es una presentación de tres pasos sobre un frame
			// singleton: sin esto, quien la abriera por segunda vez desde Ajustes se la
			// encontraría abierta por el último paso que vio.
			paso = 1;
			actualizarTextos();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(900, 640);
		setLocationRelativeTo(null);

		// **La columna es "[grow]" y no "[grow,fill]", y la diferencia se ve entera.**
		// Con "fill", cada hijo ocupa el ancho completo y el "alignx center" no pinta
		// nada: un JLabel estirado alinea su texto a la izquierda, así que la versalita
		// y el titular salían pegados al borde mientras el párrafo —que sí centra su
		// propio texto— quedaba en medio. Es la trampa que el manual describe: cuando un
		// tamaño o una alineación "no se aplica", busca un fill por encima.
		Lienzo raiz = new Lienzo(new MigLayout("wrap 1, fill, " + Space.insets(Space.GIANT), "[grow]",
				"push[]" + Space.aire(Space.XL) + "[]" + Space.aire(Space.SM) + "[]" + Space.aire(Space.XL) + "[]"
						+ Space.aire(Space.XL) + "[]" + Space.aire(Space.MD) + "[]push"));

		mascota = new MascotSlot(MascotSlot.Tamano.GRANDE, Pose.BIENVENIDA);
		raiz.add(mascota, "alignx center");

		superTitulo = Labels.capsAccent(" ");
		raiz.add(superTitulo, "alignx center");

		titulo = Labels.hero(" ");
		raiz.add(titulo, "alignx center");

		cuerpo = new WrappingText(" ");
		raiz.add(cuerpo, "alignx center, alignc, " + Layout.ancho(Layout.TEXTO));

		botonSiguiente = Buttons.primary(" ", e -> avanzar());
		raiz.add(botonSiguiente, "alignx center, height " + Typography.altoDeBoton() + "!, w 220!");

		raiz.add(pie(), "alignx center");

		setContentPane(raiz);
	}

	/** Los puntos de progreso y, mientras quede paso siguiente, el enlace para saltar. */
	private JPanel pie() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.XL + "[]", ""));
		panel.setOpaque(false);

		puntos = new Puntos();
		panel.add(puntos, "aligny center");

		enlaceSaltar = Buttons.link(" ", e -> terminar());
		panel.add(enlaceSaltar, "aligny center");

		return panel;
	}

	private void actualizarTextos() {

		User actual = sessionManager.getLoggedInUser();
		String nombre = actual != null ? actual.getName() : "";

		superTitulo.setText(Textos.t("onboarding.paso" + paso + ".superTitulo"));
		titulo.setText(
				paso == 1 ? Textos.t("onboarding.titulo", nombre) : Textos.t("onboarding.paso" + paso + ".titulo"));
		cuerpo.setText(Textos.t("onboarding.paso" + paso + ".cuerpo"));

		// La pose la decide el paso y no el azar: saludo en el primero, "haciendo
		// cosas" en los dos que cuentan qué se puede hacer.
		mascota.setPose(paso == 1 ? Pose.BIENVENIDA : Pose.ACCION);

		boolean ultimo = paso == PASOS;
		botonSiguiente.setText(Textos.t(ultimo ? "onboarding.empezar" : "onboarding.siguiente"));

		// El enlace para saltar desaparece en el último paso: ahí el botón principal
		// hace ya exactamente lo mismo, y dos controles que llevan al mismo sitio se
		// leen como un error.
		enlaceSaltar.setText(Textos.t("onboarding.saltar"));
		enlaceSaltar.setVisible(!ultimo);

		puntos.setActivo(paso);
	}

	/** Pasa al siguiente paso, o termina si ya era el último. */
	public void avanzar() {

		if (paso < PASOS) {
			paso++;
			actualizarTextos();
			return;
		}

		terminar();
	}

	/** Marca la bandera y sale por donde se entró. */
	private void terminar() {

		User actual = sessionManager.getLoggedInUser();

		if (actual == null) {
			navigator.ir(LoginFrame.class);
			return;
		}

		try {
			User actualizado = userService.completeOnboarding(actual.getId());
			sessionManager.setLoggedInUser(actualizado);

		} catch (InstanceNotFoundException ex) {
			// La cuenta ha desaparecido entre el login y este clic — caso extremo, sin
			// nada útil que decir aquí. Se sigue igualmente al buscador: quedarse
			// atascado en la bienvenida sería peor que dejar la bandera sin marcar.
		}

		if (volverAlSalir) {

			// Se apaga aquí y no al llegar: esta pantalla es un singleton, así que la
			// bandera sobreviviría hasta la próxima vez y el primer login de la
			// siguiente cuenta se encontraría con un "volver" que no le corresponde.
			volverAlSalir = false;
			navigator.volver(SearchHousingsFrame.class);
			return;
		}

		navigator.ir(SearchHousingsFrame.class);
	}

	/**
	 * Los puntos de progreso: uno por paso, relleno el que toca.
	 *
	 * <p>
	 * Se dibuja aquí y no en {@code components/}, y es la regla de dónde vive una
	 * pieza aplicada al revés de lo habitual: no sabe nada del negocio —que es el
	 * criterio para que fuera vocabulario compartido— pero tampoco lo usa nadie
	 * más, y el vocabulario se gana usándose dos veces. Si un día hace falta en
	 * otro sitio, se muda.
	 */
	private static class Puntos extends JComponent {

		private static final long serialVersionUID = 1L;

		private static final int DIAMETRO = 8;
		private static final int SEPARACION = 10;

		private int activo = 1;

		Puntos() {

			Dimension tamano = new Dimension(PASOS * DIAMETRO + (PASOS - 1) * SEPARACION, DIAMETRO);
			setPreferredSize(tamano);
			setMinimumSize(tamano);
		}

		void setActivo(int activo) {

			this.activo = activo;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int y = (getHeight() - DIAMETRO) / 2;

			for (int i = 0; i < PASOS; i++) {

				// El inactivo es el gris de texto secundario rebajado, no la hairline: sobre
				// el fondo decorativo claro de esta pantalla una hairline es invisible, y
				// unos puntos de progreso que no se ven no informan de ningún progreso.
				Color mut = Theme.mut();
				g2.setColor(i + 1 == activo ? Theme.acc() : new Color(mut.getRed(), mut.getGreen(), mut.getBlue(), 90));
				g2.fillOval(i * (DIAMETRO + SEPARACION), y, DIAMETRO, DIAMETRO);
			}

			g2.dispose();
		}
	}

	/**
	 * El lienzo de esta pantalla en concreto: el fondo decorativo cálido en vez
	 * del color plano de la estación que pinta {@link
	 * fp.project.actihome.ui.components.Page}. Mismo modo "cubrir" que ya usa
	 * {@link fp.project.actihome.ui.brand.SplashScreen}: se escala hasta llenar
	 * el panel conservando la proporción y se recorta lo que sobra, para no
	 * deformar la ilustración.
	 */
	private static class Lienzo extends JPanel {

		private static final long serialVersionUID = 1L;

		Lienzo(MigLayout layout) {
			super(layout);
			setOpaque(true);
		}

		@Override
		protected void paintComponent(Graphics g) {

			BufferedImage fondo = BrandAssets.fondo();

			if (fondo == null) {
				g.setColor(Theme.bg());
				g.fillRect(0, 0, getWidth(), getHeight());
				return;
			}

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			int ancho = getWidth();
			int alto = getHeight();
			double escala = Math.max((double) ancho / fondo.getWidth(), (double) alto / fondo.getHeight());
			int nuevoAncho = (int) Math.ceil(fondo.getWidth() * escala);
			int nuevoAlto = (int) Math.ceil(fondo.getHeight() * escala);

			g2.drawImage(fondo, (ancho - nuevoAncho) / 2, (alto - nuevoAlto) / 2, nuevoAncho, nuevoAlto, null);
			g2.dispose();
		}
	}
}
