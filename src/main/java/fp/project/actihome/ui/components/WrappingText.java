package fp.project.actihome.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JTextArea;

import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * Un párrafo de cuerpo que se ajusta a su ancho, en lugar de una sola línea.
 *
 * <p>
 * {@code Labels.body} devuelve un {@code JLabel}, y un {@code JLabel} no
 * envuelve el texto: una descripción de tres frases saldría como una sola línea
 * cortada por el borde de la ventana. Para texto largo hace falta un
 * {@code JTextArea} configurado para comportarse como una etiqueta —sin caja,
 * sin cursor, sin poder editarse— y con el ajuste de palabra activado.
 *
 * <p>
 * Sigue la misma regla que el resto del sistema: el color no se guarda, se
 * resuelve en cada pintado sobrescribiendo {@code getForeground()}, así que el
 * párrafo cambia de color solo al cambiar de estación.
 *
 * <p>
 * A diferencia de las etiquetas de {@code Labels}, aquí no hace falta guardar
 * el color en un campo propio ni protegerse de un {@code null} en el
 * constructor del padre: {@link Theme#txt()} lee un campo <b>estático</b> de
 * {@code Theme}, que existe desde que la clase se carga y no depende de que
 * esta instancia termine de construirse.
 */
public class WrappingText extends JTextArea {

	private static final long serialVersionUID = 1L;

	/** Si este párrafo es texto principal o una aclaración secundaria. */
	private final boolean secundario;

	public WrappingText(String texto) {
		this(texto, false);
	}

	private WrappingText(String texto, boolean secundario) {

		super(texto);

		this.secundario = secundario;

		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setLineWrap(true);
		setWrapStyleWord(true);
		setBorder(null);
		setFont(Typography.sans(secundario ? Typography.BODY_SM : Typography.BODY));
	}

	/**
	 * Párrafo secundario: el equivalente de {@code Labels.muted} para texto que
	 * ocupa más de una línea.
	 *
	 * <p>
	 * <b>Existe porque faltaba, y su ausencia era un fallo latente.</b> Las
	 * descripciones de ayuda bajo un control —"exporta una copia completa de la base
	 * de datos a un fichero…"— se venían escribiendo con {@code Labels.muted}, que
	 * devuelve un {@code JLabel} y <b>no parte el texto en líneas</b>. Un JLabel de
	 * cien caracteres declara un ancho preferido de cien caracteres, y MigLayout no
	 * lo encoge: desborda el contenedor. En una ventana ancha no se nota; en una de
	 * 1024 puntos empuja la tarjeta entera fuera de la pantalla.
	 *
	 * <p>
	 * Es la misma familia de fallo que {@code FilaFluida} resuelve para una fila de
	 * elementos, aplicada a un párrafo: el problema nunca es que el texto se vea
	 * apretado, es que algo acaba dibujado donde no se puede alcanzar.
	 */
	public static WrappingText muted(String texto) {
		return new WrappingText(texto, true);
	}

	/**
	 * El ancho con el que se midió por última vez, para no pedir un segundo pase
	 * de layout más de una vez por cada ancho distinto.
	 */
	private int anchoYaMedido = -1;

	/**
	 * Vuelve a pedir sitio cuando por fin se sabe de qué ancho se dispone.
	 *
	 * <p>
	 * <b>Sin esto un párrafo se dibuja cortado a media línea, y el sitio donde
	 * ocurre no se parece en nada a la causa.</b> Un {@code JTextArea} con ajuste
	 * de línea calcula su alto preferido a partir del ancho que su vista interna
	 * ya recibió. La primera vez que el gestor de layout pregunta, <b>ese ancho es
	 * cero</b>: el párrafo contesta el alto de una sola línea, el layout le reserva
	 * una línea, y acto seguido le entrega el ancho real — donde el texto se
	 * reparte en dos o en tres. La caja ya está decidida, así que lo que sobra se
	 * dibuja fuera y se lee <em>rebanado por la mitad</em>.
	 *
	 * <p>
	 * Se veía en el detalle de una reseña, cuyo cuerpo se reconstruye en cada
	 * visita, y <b>ninguna herramienta lo cazaba</b>: {@code MedirResponsive}
	 * compara cada componente contra el área visible de la ventana, y este texto no
	 * se sale de la ventana — se sale de <em>su propio panel</em>, que es el punto
	 * ciego que el manual ya tenía anotado.
	 *
	 * <p>
	 * El proyecto ya conocía el remedio: el segundo pase con
	 * {@code SwingUtilities.invokeLater(panel::revalidate)} que la Fase 9 tuvo que
	 * poner a mano en la pantalla de intercambio. Pero lo tenía escrito como una
	 * <b>costumbre que hay que acordarse de aplicar</b>, y de las cinco pantallas
	 * que construyen párrafos dinámicamente sólo tres se acordaron. Ponerlo aquí
	 * convierte la costumbre en una propiedad del componente: quien use
	 * {@code WrappingText} ya no tiene que saber que esto existe.
	 *
	 * <p>
	 * <b>No entra en bucle</b> porque sólo reacciona a un ancho <em>distinto</em>
	 * del último medido: el segundo pase vuelve a asignar el mismo ancho, la
	 * condición ya no se cumple y la cadena se para. Es la guarda obligatoria
	 * siempre que un componente pide layout desde dentro del layout.
	 */
	@Override
	public void setBounds(int x, int y, int ancho, int alto) {

		boolean cambioElAncho = ancho != anchoYaMedido;

		super.setBounds(x, y, ancho, alto);

		if (cambioElAncho && ancho > 0) {
			anchoYaMedido = ancho;
			revalidate();
		}
	}

	/**
	 * El mínimo de un párrafo es <b>la palabra más larga</b>, no el párrafo entero.
	 *
	 * <p>
	 * <b>Sin esto, un texto que sí sabe partirse en líneas se comportaba como si no
	 * supiera.</b> Un {@code JTextArea} con ajuste de línea calcula su tamaño a
	 * partir del ancho que ya tiene; cuando todavía no tiene ninguno —que es el caso
	 * cuando el layout le pregunta— responde con el ancho de <em>todo el texto en
	 * una sola línea</em>. MigLayout toma esa cifra como el mínimo por debajo del
	 * cual no puede bajar, y en una ventana estrecha desborda el contenedor en vez
	 * de repartir el párrafo en más líneas.
	 *
	 * <p>
	 * Es exactamente el mismo razonamiento que {@link FilaFluida}: por debajo del
	 * elemento indivisible más ancho no hay reflujo posible, pero por encima
	 * siempre se puede repartir. En una fila, ese elemento es el hijo más ancho; en
	 * un párrafo, la palabra más larga.
	 *
	 * <p>
	 * <b>El alto mínimo es UNA LÍNEA, y llegar a eso costó una segunda corrección.</b>
	 * La primera versión devolvía {@code getPreferredSize().height}, que parecía lo
	 * natural: el alto que el párrafo necesita al ancho que tiene ahora. Y hacía que
	 * este método <b>no fuera una función pura del componente</b>.
	 *
	 * <p>
	 * El motivo es que el alto preferido de un {@code JTextArea} con ajuste de línea
	 * <b>no depende solo del texto y de la fuente</b>: depende de la última anchura
	 * que la vista interna de Swing recibió, que va cambiando mientras el gestor de
	 * layout tantea tamaños. Preguntar por el mínimo en dos momentos distintos del
	 * mismo pase daba dos respuestas distintas — 18 puntos en uno y 36 en otro, según
	 * si en ese instante el texto cabía en una línea o en dos.
	 *
	 * <p>
	 * <b>Y eso volvía intermitente a {@code MedirResponsive}</b>, que compara el alto
	 * real contra este mínimo: la misma pantalla, sin tocar una línea de código, daba
	 * "ok" o "1 ROTO" según la pasada. Medido: <b>tres fallos de seis ejecuciones</b>.
	 * Una comprobación que falla la mitad de las veces por un motivo que no existe es
	 * peor que no tenerla, porque enseña a ignorar sus avisos — y esta corre en el CI.
	 *
	 * <p>
	 * Una línea es un suelo honesto y <b>estable</b>: sale de la métrica de la fuente
	 * y de nada más. Por debajo de una línea no se lee nada; por encima, el párrafo
	 * reflowea solo. Lo que el párrafo <em>quiere</em> lo sigue diciendo
	 * {@code getPreferredSize()}, que es a quien el layout hace caso mientras haya
	 * sitio.
	 *
	 * <p>
	 * <b>Y hubo una tercera corrección, porque «una línea siempre» tampoco era
	 * verdad.</b> Ese suelo es honesto <em>antes</em> de que a nadie le hayan dado un
	 * ancho; después, no. Un párrafo que ya sabe que ocupa dos líneas y sigue
	 * declarando que le basta una está mintiendo, y la mentira se cobra donde no se
	 * mira: {@link Rescate} apreta la pantalla antes de sacar la barra y le quita
	 * alto a quien dijo que podía cederlo, así que el párrafo recibía 28 puntos donde
	 * necesitaba 36 y su segunda línea salía <b>cortada por la mitad</b>. Ni
	 * {@code MedirResponsive} lo veía —el texto no se sale de la ventana, se sale de
	 * su propio panel— ni parecía un problema de tamaño al mirarlo.
	 *
	 * <p>
	 * Un párrafo puede ceder <b>ancho</b>, que para eso reflúye, y no puede ceder
	 * <b>alto</b>: no hay manera de enseñar dos líneas en el hueco de una. Así que el
	 * mínimo pasa a ser el alto real en cuanto hay un ancho asignado — y sigue
	 * cumpliendo la regla general de abajo, porque no se contesta con el ancho que el
	 * componente tiene <em>en este instante</em> sino con el último que alguien le
	 * <b>asignó</b>, que es un valor que no se mueve mientras MigLayout tantea.
	 *
	 * <p>
	 * <b>La regla general, que vale para cualquier componente propio:</b>
	 * {@code getMinimumSize()} tiene que poder contestarse sin saber en qué momento
	 * del pase de layout te lo preguntan. Si su respuesta depende del tamaño que el
	 * componente tiene ahora mismo, no es un mínimo: es una medición.
	 */
	@Override
	public Dimension getMinimumSize() {

		FontMetrics metrica = getFontMetrics(getFont());
		Insets margenes = getInsets();
		int masLarga = 0;

		for (String palabra : getText().split("\\s+")) {
			masLarga = Math.max(masLarga, metrica.stringWidth(palabra));
		}

		// **El alto mínimo es una línea mientras nadie nos haya dado un ancho, y el
		// alto real en cuanto lo tenemos.** La versión anterior contestaba siempre una
		// línea, y eso convertía al párrafo en la pieza que se deja machacar: cuando la
		// pantalla no cabe, Rescate aprieta antes de sacar la barra, MigLayout le quita
		// alto a quien declaró que podía cederlo, y un párrafo que decía "con 18 puntos
		// me apaño" recibía 28 donde necesitaba 36 — dibujando la segunda línea
		// **rebanada por la mitad**. Medido así en el detalle de una reseña.
		//
		// Es exactamente el fallo que el javadoc de Rescate anticipaba: "depende por
		// completo de que cada componente diga la verdad sobre su mínimo". Un párrafo
		// puede ceder **ancho** —para eso reflúye— pero no puede ceder **alto**: no hay
		// forma de enseñar dos líneas en el hueco de una.
		//
		// Y sigue siendo estable, que es lo que costó la Fase 8.13. No se contesta con
		// el ancho que el componente tiene *en este instante* —eso cambiaba mientras
		// MigLayout tantea, y volvió intermitente al CI— sino con el último ancho que
		// alguien nos **asignó** de verdad, que es un valor que no se mueve durante un
		// pase de layout. Antes del primer reparto no hay ancho que valga, y ahí una
		// línea sigue siendo el único suelo honesto.
		int altoMinimo = anchoYaMedido > 0 ? getPreferredSize().height
				: metrica.getHeight() + margenes.top + margenes.bottom;

		return new Dimension(masLarga + margenes.left + margenes.right, altoMinimo);
	}
	@Override
	public Color getForeground() {

		// Guarda contra el null implícito del arranque: getForeground() se llama desde
		// el constructor de JTextArea, antes de que 'secundario' tenga valor. Un
		// boolean no puede ser null, pero sí es false en ese momento, así que el color
		// inicial sería el principal aunque el párrafo fuera secundario. Da igual: el
		// primer pintado real ocurre mucho después, con el campo ya asignado.
		return secundario ? Theme.mut() : Theme.txt();
	}
}
