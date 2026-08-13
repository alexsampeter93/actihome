package fp.project.actihome.ui;

import java.io.File;
import java.time.LocalDate;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.exceptions.BackupFailedException;
import fp.project.actihome.model.exceptions.BackupNotAvailableException;
import fp.project.actihome.model.exceptions.InstanceNotFoundException;
import fp.project.actihome.model.exceptions.NotAuthorizedUserException;
import fp.project.actihome.model.services.BackupService;
import fp.project.actihome.model.services.PasswordResetService;
import fp.project.actihome.ui.components.Buttons;
import fp.project.actihome.ui.components.Card;
import fp.project.actihome.ui.components.CodigoCopiable;
import fp.project.actihome.ui.components.Columnas;
import fp.project.actihome.ui.components.Field;
import fp.project.actihome.ui.components.Foco;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.MascotSlot;
import fp.project.actihome.ui.components.Page;
import fp.project.actihome.ui.components.Rescate;
import fp.project.actihome.ui.components.Toast;
import fp.project.actihome.ui.components.WrappingText;
import fp.project.actihome.ui.nav.Navigator;
import fp.project.actihome.ui.sessionManagement.SessionManager;
import fp.project.actihome.ui.theme.BrandAssets.Pose;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;

/**
 * Mantenimiento de la instalación: copia de seguridad de la base de datos y
 * generación de códigos de recuperación. Solo ADMIN.
 *
 * <h2>Por qué existe esta pantalla</h2>
 *
 * <p>
 * <b>Los dos bloques vivían en {@code SettingsFrame} y ahí no eran ajustes de
 * nadie.</b> Exportar la base de datos entera y generar un código para que
 * <em>otra</em> persona recupere su cuenta no son preferencias de quien las
 * ejecuta: son operaciones sobre la instalación. El propio código lo decía —el
 * comentario que justificaba ocultarlas a CUSTOMER hablaba de «tareas de
 * administración de la instalación, no de la cuenta de quien las usa»— y aun así
 * seguían dentro de la pantalla de la cuenta.
 *
 * <p>
 * <b>Y lo que destapó la contradicción fue una medida, no una revisión de
 * diseño.</b> Ajustes era la única pantalla que seguía sin caber en un portátil:
 * pedía 1085 puntos donde hay 672, y de esos, la tarjeta de administración ponía
 * <b>566 ella sola</b> — más que las otras dos juntas. En columnas manda la más
 * alta, así que no había reparto que lo salvara. Cuando un bloque no cabe en
 * ninguna disposición razonable, muchas veces el problema no es el tamaño sino
 * que está en el sitio equivocado.
 *
 * <p>
 * <b>Aparte de {@link PlatformPanelFrame}, y no dentro.</b> Aquel contesta
 * «cómo va la plataforma» —ocupación, ingresos, propietarios— y su cuerpo es una
 * lista que crece con el número de alojamientos, así que se recorre con la
 * rueda. Meter «Exportar copia de seguridad» dentro de una lista que se desplaza
 * convierte una acción en algo que hay que ir a buscar. Aquí no hay lista: son
 * dos acciones y caben las dos a la vista.
 */
@Component
@Profile("!test")
@Lazy
public class AdministrationFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Dos tarjetas de ancho cómodo más su separación, con margen para el marco. */
	private static final int ANCHO_DEL_CUERPO = 800;

	private final transient BackupService backupService;
	private final transient PasswordResetService passwordResetService;
	private final transient SessionManager sessionManager;
	private final transient Navigator navigator;
	private final HeaderPanel headerPanel;

	private JButton enlaceVolver;
	private JLabel superTitulo;
	private JLabel titulo;

	// Copia de seguridad (F12)
	private JLabel superTituloCopia;
	private WrappingText descripcionCopia;
	private JButton exportarCopia;
	private JLabel errorCopia;

	// Código de recuperación para otro usuario (Fase 8.6)
	private JLabel superTituloCodigo;
	private WrappingText descripcionCodigo;
	private Field usuarioDelCodigo;
	private JButton generarCodigo;
	private CodigoCopiable codigoGenerado;
	private JLabel entregaCodigo;
	private JLabel resultadoCodigo;

	public AdministrationFrame(BackupService backupService, PasswordResetService passwordResetService,
			SessionManager sessionManager, Navigator navigator, HeaderPanel headerPanel) {

		this.backupService = backupService;
		this.passwordResetService = passwordResetService;
		this.sessionManager = sessionManager;
		this.navigator = navigator;
		this.headerPanel = headerPanel;

		initUI();
	}

	@Override
	public void setVisible(boolean visible) {

		if (visible) {
			headerPanel.refresh();
			actualizarTextos();
			limpiar();
		}

		super.setVisible(visible);
	}

	private void initUI() {

		setTitle("ActiHome");
		setSize(920, 700);
		setLocationRelativeTo(null);

		JPanel raiz = new Page(new MigLayout("wrap 1, fill, " + Space.insets(0), "[grow,fill]", "[]0[grow,fill]"));

		JPanel exterior = new JPanel(new MigLayout("wrap 1, " + Space.insetsLaterales(Space.GIANT, Space.GIANT),
				"[grow]", Space.margen(Space.GIANT) + "[grow]" + Space.margen(Space.GIANT)));
		exterior.setOpaque(false);
		exterior.add(cuerpo(), Layout.ancho(ANCHO_DEL_CUERPO) + ", aligny center, alignx center");

		raiz.add(headerPanel, "growx");
		raiz.add(Rescate.envolver(exterior), "grow");

		setContentPane(raiz);

		Foco.alPulsarEscape(this, () -> navigator.volver(ShowHousingsFrame.class));
	}

	private JPanel cuerpo() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]",
				"[]" + Space.aire(Space.XL) + "[]"));
		panel.setOpaque(false);

		panel.add(cabecera());

		// Dos tarjetas donde quepan dos, una debajo de otra donde no: el reparto lo
		// decide el ancho, igual que en el resto de la aplicación desde que existe
		// Columnas.
		Columnas columnas = new Columnas(360, Space.LG);
		columnas.add(tarjetaCopiaDeSeguridad());
		columnas.add(tarjetaCodigoDeRecuperacion());

		panel.add(columnas);

		return panel;
	}

	/**
	 * Era la única pantalla con {@code Foco.alPulsarEscape} sin ningún enlace
	 * visible que hiciera lo mismo (Fase 9): se llega aquí desde el menú de
	 * usuario, no desde la cabecera de navegación, así que sin este enlace la
	 * única forma de salir era conocer de antemano que Escape volvía al catálogo.
	 */
	private JPanel cabecera() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(0), "[grow,fill]push[]", ""));
		panel.setOpaque(false);

		JPanel titulos = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", ""));
		titulos.setOpaque(false);

		enlaceVolver = Buttons.link(" ", e -> navigator.volver(ShowHousingsFrame.class));
		titulos.add(enlaceVolver, "gapbottom " + Space.XXS);

		superTitulo = Labels.capsAccent(" ");
		titulos.add(superTitulo);

		titulo = Labels.title(" ");
		titulos.add(titulo, "gaptop " + Space.XXS);

		panel.add(titulos, "aligny center");
		panel.add(new MascotSlot(MascotSlot.Tamano.PEQUENO, Pose.BIENVENIDA), "top, w 56!, h 56!");

		return panel;
	}

	/** Copia de seguridad de la base de datos (F12). */
	private Card tarjetaCopiaDeSeguridad() {

		Card tarjeta = new Card(new MigLayout("wrap 1, " + Space.insets(Space.XL), "[grow,fill]", ""));

		superTituloCopia = Labels.capsAccent(" ");
		descripcionCopia = WrappingText.muted(" ");

		exportarCopia = Buttons.secondary(" ", e -> exportarCopiaDeSeguridad());
		errorCopia = Labels.error(" ");
		errorCopia.setVisible(false);

		tarjeta.add(superTituloCopia, "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(descripcionCopia, "gapbottom " + Space.aire(Space.SM));
		tarjeta.add(exportarCopia);
		tarjeta.add(errorCopia, "gaptop " + Space.XXS);

		return tarjeta;
	}

	/**
	 * Generar un código de recuperación para otro usuario (Fase 8.6).
	 *
	 * <p>
	 * Es el camino de salida cuando esta instalación no tiene servidor de correo
	 * configurado: sin él, quien olvida su contraseña no puede pedir nada por sí
	 * mismo, y {@code RecoverPasswordFrame} le dice explícitamente que se lo pida a
	 * un administrador. Aquí es donde ese administrador lo genera.
	 */
	private Card tarjetaCodigoDeRecuperacion() {

		Card tarjeta = new Card(new MigLayout("wrap 1, " + Space.insets(Space.XL), "[grow,fill]", ""));

		superTituloCodigo = Labels.capsAccent(" ");
		descripcionCodigo = WrappingText.muted(" ");
		usuarioDelCodigo = Field.text(" ");
		generarCodigo = Buttons.secondary(" ", e -> generarCodigoDeRecuperacion());
		codigoGenerado = new CodigoCopiable();
		entregaCodigo = Labels.muted(" ");
		resultadoCodigo = Labels.body(" ");

		tarjeta.add(superTituloCodigo, "gapbottom " + Space.aire(Space.MD));
		tarjeta.add(descripcionCodigo, "gapbottom " + Space.aire(Space.SM));
		tarjeta.add(usuarioDelCodigo, "gapbottom " + Space.aire(Space.SM));
		tarjeta.add(generarCodigo, "gapbottom " + Space.aire(Space.SM));

		// El código y la instrucción de qué hacer con él van juntos y por encima de
		// resultadoCodigo, que solo lleva errores. Con un único hueco para las dos
		// cosas, el dato más importante de la pantalla compartía sitio y tamaño con
		// "no hay ningún usuario con ese nombre".
		tarjeta.add(codigoGenerado, "gapbottom " + Space.XXS);
		tarjeta.add(entregaCodigo, "gapbottom " + Space.XS);
		tarjeta.add(resultadoCodigo);

		return tarjeta;
	}

	private void actualizarTextos() {

		enlaceVolver.setText(Textos.t("detalle.volver"));
		superTitulo.setText(Textos.t("admin.superTitulo"));
		titulo.setText(Textos.t("ajustes.administracion"));

		superTituloCopia.setText(Textos.t("ajustes.backup.titulo"));
		descripcionCopia.setText(Textos.t("ajustes.backup.descripcion"));
		exportarCopia.setText(Textos.t("ajustes.backup.boton"));

		superTituloCodigo.setText(Textos.t("admin.codigo.titulo"));
		descripcionCodigo.setText(Textos.t("admin.codigo.descripcion"));
		usuarioDelCodigo.setEtiqueta(Textos.t("admin.codigo.usuario"));
		generarCodigo.setText(Textos.t("admin.codigo.generar"));
	}

	private void limpiar() {

		usuarioDelCodigo.setText("");
		codigoGenerado.limpiar();
		entregaCodigo.setVisible(false);
		resultadoCodigo.setText(" ");
		errorCopia.setVisible(false);
	}

	private void exportarCopiaDeSeguridad() {

		errorCopia.setVisible(false);

		JFileChooser selector = new JFileChooser();
		selector.setDialogTitle(Textos.t("ajustes.backup.dialogoTitulo"));
		selector.setFileFilter(new FileNameExtensionFilter("ZIP", "zip"));
		selector.setSelectedFile(new File(Textos.t("ajustes.backup.nombreSugerido") + "-" + LocalDate.now() + ".zip"));

		if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File destino = selector.getSelectedFile();

		if (!destino.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
			destino = new File(destino.getParentFile(), destino.getName() + ".zip");
		}

		try {
			backupService.exportarCopiaDeSeguridad(sessionManager.getLoggedInUser().getId(), destino.getAbsolutePath());
			Toast.mostrar(this, Textos.t("ajustes.backup.confirmacion"));

		} catch (NotAuthorizedUserException ex) {
			errorCopia.setText(Textos.t("ajustes.error.usuarioNoExiste"));
			errorCopia.setVisible(true);

		} catch (InstanceNotFoundException ex) {
			navigator.ir(LoginFrame.class);

		} catch (BackupNotAvailableException ex) {
			errorCopia.setText(Textos.t("ajustes.backup.error.noDisponible"));
			errorCopia.setVisible(true);

		} catch (BackupFailedException ex) {
			errorCopia.setText(Textos.t("ajustes.backup.error.fallo"));
			errorCopia.setVisible(true);
		}
	}

	/**
	 * Rellena el usuario y genera, como si se hubiera tecleado y pulsado el botón.
	 *
	 * <p>
	 * Existe para {@code ScreenSnapshots} por la misma razón que
	 * {@code TradeHousingsFrame.buscarPorCodigo}: el estado que hay que revisar —el
	 * código ya en pantalla— no se alcanza abriendo la ventana, y una captura del
	 * estado inicial no enseñaría precisamente la parte nueva.
	 */
	public void generarCodigoPara(String username) {

		usuarioDelCodigo.setText(username);
		generarCodigoDeRecuperacion();
	}

	private void generarCodigoDeRecuperacion() {

		String nombre = usuarioDelCodigo.getText().trim();

		// Cada intento parte de cero: dejar en pantalla el código de la consulta
		// anterior mientras se enseña un error de la nueva es la forma más directa de
		// que alguien dicte un código que ya no corresponde al usuario que pidió.
		codigoGenerado.limpiar();
		entregaCodigo.setVisible(false);

		if (nombre.isEmpty()) {
			resultadoCodigo.setText(Textos.t("recuperar.error.usuarioVacio"));
			return;
		}

		try {
			String codigo = passwordResetService.generarCodigoParaEntregar(nombre,
					sessionManager.getLoggedInUser().getId());

			resultadoCodigo.setText(" ");
			codigoGenerado.mostrar(codigo);

			entregaCodigo.setText(Textos.t("admin.codigo.entrega", nombre));
			entregaCodigo.setVisible(true);

		} catch (InstanceNotFoundException ex) {
			// Aquí SÍ se dice que el usuario no existe, al revés que en la pantalla de
			// recuperación. La diferencia es quién pregunta: allí es cualquiera y
			// contestarlo convertiría la pantalla en un comprobador de cuentas; aquí es
			// un administrador identificado que necesita saber si se ha equivocado al
			// teclear el nombre.
			resultadoCodigo.setText(Textos.t("admin.codigo.error.noExiste"));

		} catch (NotAuthorizedUserException ex) {
			resultadoCodigo.setText(Textos.t("admin.codigo.error.soloAdmin"));
		}
	}
}
