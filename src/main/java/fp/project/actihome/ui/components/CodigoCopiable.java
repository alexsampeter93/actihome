package fp.project.actihome.ui.components;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Enseña un código de un solo uso de forma que se pueda <b>leer, dictar y
 * copiar</b>.
 *
 * <p>
 * <b>El problema que resuelve, y era un fallo de verdad.</b> El código de
 * recuperación se pintaba con {@code Labels.body(...)} dentro de una frase
 * ("Código para Ana: A3F7K9M2"). Eso tiene tres cosas mal, y la tercera es la
 * grave:
 *
 * <ul>
 * <li>Sale al <b>tamaño del texto corrido</b>, cuando es el único dato de la
 * pantalla que alguien va a transcribir carácter a carácter.</li>
 * <li>Sale <b>en bloque</b>, y ocho caracteres seguidos sin agrupar se pierden
 * al dictarlos por teléfono.</li>
 * <li>Sale en un {@code JLabel}, y <b>el texto de un JLabel no se puede
 * seleccionar con el ratón</b>. No es que costara encontrar cómo copiarlo: es
 * que no se podía. Para un dato que solo se ve una vez —después se guarda
 * cifrado y ni la aplicación puede volver a leerlo— eso es perderlo.</li>
 * </ul>
 *
 * <p>
 * <b>Las tres decisiones de esta pieza.</b>
 *
 * <p>
 * <b>1. Es un {@link JTextField} de solo lectura, no una etiqueta.</b> Sin
 * borde, sin fondo y sin poder editarse, se ve exactamente igual que un texto
 * fijo — pero se puede seleccionar y copiar con el teclado, que es lo que
 * cualquiera intenta primero. El botón de copiar es el atajo; la selección es la
 * red por debajo. Dar solo el botón dejaría fuera a quien no lo vea.
 *
 * <p>
 * <b>2. Se enseña agrupado y se copia en crudo.</b> En pantalla pone
 * {@code A3F7 · K9M2} porque un bloque partido en dos se lee y se dicta mucho
 * mejor; al portapapeles va {@code A3F7K9M2}, que es lo que el otro formulario
 * espera. Es deliberado que las dos formas no coincidan: el separador es una
 * ayuda de lectura, no parte del código.
 *
 * <p>
 * <b>3. No hace falta fuente monoespaciada</b>, y merece explicarse porque es lo
 * primero que se propondría. La monoespaciada sirve para distinguir caracteres
 * que se parecen, y aquí ese problema <b>ya está resuelto en el origen</b>: el
 * alfabeto del generador excluye {@code I}, {@code L}, {@code O}, {@code 0} y
 * {@code 1}. Añadir una cuarta familia tipográfica al proyecto para un problema
 * que no existe sería coste sin beneficio.
 */
public class CodigoCopiable extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Cuánto se separan las letras. Un código se lee mejor holgado. */
	private static final float TRACKING = 0.22f;

	/** Cada cuántos caracteres se parte para leerlo. */
	private static final int TAMANO_DE_GRUPO = 4;

	/** El separador visual. Está en las dos fuentes: comprobado con MedirGlifos. */
	private static final String SEPARADOR = " · ";

	/** El código tal cual, sin separadores. Es lo que va al portapapeles. */
	private transient String codigo;

	private final JTextField visible;
	private final JButton copiar;

	public CodigoCopiable() {

		super(new MigLayout(Space.insets(0), "[]" + Space.MD + "[]", "[]"));
		setOpaque(false);

		visible = crearCampo();
		copiar = Buttons.secondary(" ", e -> alPortapapeles());

		add(visible);
		add(copiar);

		limpiar();
	}

	/**
	 * El campo que enseña el código: un {@code JTextField} disfrazado de texto
	 * fijo.
	 *
	 * <p>
	 * {@code setEditable(false)} más quitar borde y fondo lo dejan visualmente
	 * indistinguible de una etiqueta, conservando lo único que a una etiqueta le
	 * falta: que se pueda seleccionar. El color se resuelve al pintar
	 * ({@code getForeground}) igual que en el resto del sistema, para que siga a la
	 * estación sin suscribirse a nada.
	 */
	private JTextField crearCampo() {

		JTextField campo = new JTextField() {

			private static final long serialVersionUID = 1L;

			@Override
			public Color getForeground() {
				return Theme.accText();
			}
		};

		campo.setEditable(false);
		campo.setBorder(null);
		campo.setOpaque(false);
		campo.setFocusable(true);

		Font base = Typography.sansSemiBold(Typography.PRICE);
		campo.setFont(base.deriveFont(Collections.singletonMap(TextAttribute.TRACKING, TRACKING)));

		// El ancho se declara en columnas de texto y no en píxeles: el código mide
		// siempre lo mismo en caracteres, pero cuántos píxeles ocupa eso depende del
		// escalado del sistema. Es la regla de siempre — ningún tamaño que dependa de
		// texto puede ser una constante.
		campo.setColumns(14);

		return campo;
	}

	/** Enseña un código nuevo y deja el bloque visible. */
	public void mostrar(String codigo) {

		this.codigo = codigo;

		visible.setText(agrupar(codigo));
		visible.setCaretPosition(0);
		visible.setVisible(true);

		copiar.setText(Textos.t("admin.codigo.copiar"));
		copiar.setEnabled(true);
		copiar.setVisible(true);
	}

	/** Esconde el bloque. Se llama al abrir la pantalla y tras cada error. */
	public final void limpiar() {

		codigo = null;

		visible.setText("");
		visible.setVisible(false);

		copiar.setVisible(false);
	}

	private void alPortapapeles() {

		if (codigo == null) {
			return;
		}

		// Al portapapeles va el código CRUDO, sin los separadores de lectura.
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(codigo), null);

		// La confirmación se da en el propio botón y no con un Toast: el usuario está
		// mirando el botón que acaba de pulsar, y un aviso flotante en otra esquina se
		// lee como un mensaje distinto. No se vuelve a "Copiar" — que siga diciendo
		// "Copiado" es información correcta y evita un temporizador que habría que
		// parar al cerrar la ventana.
		copiar.setText(Textos.t("admin.codigo.copiado"));
	}

	/** Parte el código en grupos para que se pueda leer y dictar. */
	private static String agrupar(String codigo) {

		StringBuilder agrupado = new StringBuilder();

		for (int i = 0; i < codigo.length(); i++) {

			if (i > 0 && i % TAMANO_DE_GRUPO == 0) {
				agrupado.append(SEPARADOR);
			}

			agrupado.append(codigo.charAt(i));
		}

		return agrupado.toString();
	}
}
