package fp.project.actihome.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
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
 * Bienvenida de una sola vez, la primera vez que se inicia sesión con una
 * cuenta.
 *
 * <p>
 * Fase 7.8: hasta ahora quien entraba por primera vez caía directo en el
 * catálogo, sin ninguna explicación de qué es ActiHome. {@code
 * LoginFrame.entrar()} decide si toca esta pantalla o el catálogo mirando
 * {@code User.isOnboardingSeen()}; el único botón, "Empezar", marca esa
 * bandera y esta pantalla no vuelve a aparecer para esa cuenta.
 *
 * <p>
 * Es de las pocas pantallas que usa {@link BrandAssets#fondo()} en vez del
 * color plano de la estación activa: ese fondo decorativo está reservado
 * para esto y para el splash de arranque (CLAUDE.md §6) — su paleta cálida y
 * fija competiría con las tarjetas si se usara en el catálogo o en cualquier
 * listado.
 *
 * <p>
 * Sin cabecera y sin atajo de Escape, por la misma razón que {@code
 * LoginFrame}: es un destino, no un paso de un formulario con un "atrás" que
 * tenga sentido.
 */
@Component
@Profile("!test")
@Lazy
public class OnboardingFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient UserService userService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;

	private JLabel superTitulo;
	private JLabel titulo;
	private WrappingText cuerpo;
	private JButton botonEmpezar;

	public OnboardingFrame(UserService userService, SessionManager sessionManager, Navigator navigator) {

		this.userService = userService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			actualizarTextos();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(900, 640);
		setLocationRelativeTo(null);

		Lienzo raiz = new Lienzo(new MigLayout("wrap 1, fill, " + Space.insets(Space.GIANT), "[grow,fill]",
				"push[]" + Space.XL + "[]" + Space.SM + "[]" + Space.XL + "[]" + Space.XXL + "[]push"));

		raiz.add(new MascotSlot(MascotSlot.Tamano.GRANDE, Pose.BIENVENIDA), "alignx center");

		superTitulo = Labels.capsAccent(" ");
		raiz.add(superTitulo, "alignx center");

		titulo = Labels.hero(" ");
		raiz.add(titulo, "alignx center");

		cuerpo = new WrappingText(" ");
		raiz.add(cuerpo, "alignx center, alignc, " + Layout.ancho(Layout.TEXTO));

		botonEmpezar = Buttons.primary(" ", e -> empezar());
		raiz.add(botonEmpezar, "alignx center, height " + Typography.altoDeBoton() + "!, w 220!");

		setContentPane(raiz);
	}

	private void actualizarTextos() {

		User actual = sessionManager.getLoggedInUser();
		String nombre = actual != null ? actual.getName() : "";

		superTitulo.setText(Textos.t("onboarding.superTitulo"));
		titulo.setText(Textos.t("onboarding.titulo", nombre));
		cuerpo.setText(Textos.t("onboarding.cuerpo"));
		botonEmpezar.setText(Textos.t("onboarding.empezar"));
	}

	private void empezar() {

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

		navigator.ir(SearchHousingsFrame.class);
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
