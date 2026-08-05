package fp.project.actihome.ui;

import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.exceptions.InvalidResetCodeException;
import fp.project.actihome.model.services.PasswordResetService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Typography;

/**
 * Recuperar una contraseña olvidada (Fase 8.6).
 *
 * <p>
 * <b>Una sola pantalla con los dos pasos a la vista</b>, no un asistente de dos
 * ventanas. Quien llega aquí con un código que le ha dado un administrador no
 * tiene que pasar por el paso de pedirlo, y quien lo pide por correo ve desde el
 * principio qué va a tener que hacer después. Partirlo en dos pantallas obligaría
 * al primero a fingir que pide algo que ya tiene.
 *
 * <p>
 * <b>El texto de ayuda cambia según si esta instalación puede enviar correo.</b>
 * Se pregunta al abrir, antes de que el usuario escriba nada: prometer un correo
 * que nunca va a llegar deja a alguien esperando delante de la pantalla, y
 * decirle de entrada que pida el código a un administrador, no. Es toda la razón
 * de ser de {@code PasswordResetService.puedeEnviarCorreo()}.
 *
 * <p>
 * El envío va en un {@link SwingWorker} por lo mismo que la traducción de la
 * Fase 8.5: detrás hay una conexión a un servidor de correo, y una espera de red
 * en el hilo de la interfaz la congela entera.
 */
@Component
@Profile("!test")
@Lazy
public class RecoverPasswordFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final transient PasswordResetService passwordResetService;
	private final transient Navigator navigator;

	private JLabel superTitulo;
	private JLabel titulo;
	private JLabel ayuda;
	private JLabel dondeConseguirlo;

	private Field usuario;
	private JButton pedirCodigo;

	private Field codigo;
	private Field nuevaContrasena;
	private JButton cambiar;
	private JButton cancelar;
	private JLabel error;

	public RecoverPasswordFrame(PasswordResetService passwordResetService, Navigator navigator) {

		this.passwordResetService = passwordResetService;
		this.navigator = navigator;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			usuario.setText("");
			codigo.setText("");
			nuevaContrasena.setText("");
			error.setText(" ");
			actualizarTextos();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(760, 640);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("fill, " + Space.insets(Space.GIANT), "[grow]", "[grow]"));

		JPanel formulario = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		formulario.setOpaque(false);

		superTitulo = Labels.capsAccent(" ");
		titulo = Labels.title(" ");
		ayuda = Labels.muted(" ");
		dondeConseguirlo = Labels.muted(" ");

		formulario.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA),
				"w 56!, h 56!, gapbottom " + Space.MD);
		formulario.add(superTitulo);
		formulario.add(titulo, "gaptop " + Space.XXS + ", gapbottom " + Space.XS);
		formulario.add(ayuda, "gapbottom " + Space.XS);
		formulario.add(dondeConseguirlo, "gapbottom " + Space.XL);

		usuario = Field.text(" ");
		formulario.add(usuario, "gapbottom " + Space.SM);

		pedirCodigo = Buttons.secondary(" ", e -> pedirCodigo());
		formulario.add(pedirCodigo, "gapbottom " + Space.XXL);

		codigo = Field.text(" ");
		formulario.add(codigo, "gapbottom " + Space.MD);

		nuevaContrasena = Field.password(" ");
		formulario.add(nuevaContrasena, "gapbottom " + Space.MD);

		error = Labels.error(" ");
		formulario.add(error, "gapbottom " + Space.SM);

		formulario.add(acciones());

		JPanel centrado = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow]", "[grow]"));
		centrado.setOpaque(false);
		centrado.add(formulario, Layout.ancho(Layout.FORMULARIO) + ", alignx center, aligny center");

		raiz.add(Rescate.envolver(centrado), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.ir(LoginFrame.class));
	}

	private JPanel acciones() {

		JPanel fila = new JPanel(new MigLayout(Space.insets(0), "[]" + Space.LG + "[]", ""));
		fila.setOpaque(false);

		cambiar = Buttons.primary(" ", e -> cambiar());
		cancelar = Buttons.link(" ", e -> navigator.ir(LoginFrame.class));

		fila.add(cambiar, "height " + Typography.altoDeBoton() + "!");
		fila.add(cancelar);

		return fila;
	}

	private void actualizarTextos() {

		superTitulo.setText(Textos.t("recuperar.superTitulo"));
		titulo.setText(Textos.t("recuperar.titulo"));

		// El texto de ayuda y el botón de pedir código dependen de si esta instalación
		// puede enviar correo. Sin servidor configurado, pedir un código por correo no
		// llevaría a ninguna parte, así que el botón desaparece en lugar de quedarse
		// ahí sin hacer nada.
		boolean conCorreo = passwordResetService.puedeEnviarCorreo();

		ayuda.setText(Textos.t(conCorreo ? "recuperar.ayuda.conCorreo" : "recuperar.ayuda.sinCorreo"));

		// El "dónde" del camino sin correo. Sin esto, la pantalla decía "pídele un
		// código a un administrador" sin explicar dónde se genera, y quedaba un
		// callejón sin salida: para generarlo hay que entrar como administrador, y
		// quien no puede entrar es precisamente el que lo necesita. La nota dice las
		// dos cosas — dónde está la opción y qué hacer si el bloqueado eres tú.
		dondeConseguirlo.setText(Textos.t("recuperar.donde"));
		dondeConseguirlo.setVisible(!conCorreo);
		pedirCodigo.setVisible(conCorreo);
		pedirCodigo.setText(Textos.t("recuperar.pedirCodigo"));

		usuario.setEtiqueta(Textos.t("login.usuario"));
		codigo.setEtiqueta(Textos.t("recuperar.codigo"));
		nuevaContrasena.setEtiqueta(Textos.t("recuperar.nuevaContrasena"));
		cambiar.setText(Textos.t("recuperar.cambiar"));
		cancelar.setText(Textos.t("ajustes.cancelar"));
	}

	private void pedirCodigo() {

		String nombre = usuario.getText().trim();

		if (nombre.isEmpty()) {
			error.setText(Textos.t("recuperar.error.usuarioVacio"));
			return;
		}

		pedirCodigo.setEnabled(false);
		error.setText(Textos.t("recuperar.enviando"));

		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() {
				passwordResetService.solicitarPorCorreo(nombre);
				return null;
			}

			@Override
			protected void done() {

				pedirCodigo.setEnabled(true);

				try {
					get();

				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();

				} catch (ExecutionException ex) {
					// El servicio no propaga los fallos de envío a propósito (ver su javadoc):
					// contarlos revelaría qué cuentas existen. Aquí solo se llega si algo
					// inesperado revienta, y el mensaje sigue siendo el mismo por la misma razón.
					Thread.currentThread().interrupt();
				}

				// SIEMPRE el mismo mensaje, exista o no el usuario y se haya podido enviar o
				// no. Es lo que hace que esta pantalla no sirva para averiguar qué nombres
				// están registrados, y es la misma decisión que ya toma el login al no
				// distinguir entre usuario inexistente y contraseña incorrecta.
				error.setText(" ");
				Toast.mostrar(RecoverPasswordFrame.this, Textos.t("recuperar.enviadoSiExiste"));
			}
		}.execute();
	}

	private void cambiar() {

		String nombre = usuario.getText().trim();
		String clave = nuevaContrasena.getText();

		if (nombre.isEmpty() || codigo.getText().trim().isEmpty() || clave.isEmpty()) {
			error.setText(Textos.t("recuperar.error.camposVacios"));
			return;
		}

		try {
			passwordResetService.restablecer(nombre, codigo.getText().trim().toUpperCase(), clave);

			navigator.ir(LoginFrame.class);
			Toast.mostrar(navigator.ventanaVisible(), Textos.t("recuperar.confirmacion"));

		} catch (InvalidResetCodeException ex) {
			error.setText(Textos.t("recuperar.error.codigoInvalido"));
		}
	}
}
