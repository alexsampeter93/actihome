package fp.project.actihome.ui.catalog;

import java.awt.FontMetrics;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import fp.project.actihome.model.entities.Housing;
import fp.project.actihome.ui.components.Hairline;
import fp.project.actihome.ui.components.Labels;
import fp.project.actihome.ui.components.Stat;
import fp.project.actihome.ui.theme.Formato;
import fp.project.actihome.ui.theme.Layout;
import fp.project.actihome.ui.theme.Space;
import fp.project.actihome.ui.theme.Textos;
import fp.project.actihome.ui.theme.Theme;
import fp.project.actihome.ui.theme.Typography;

/**
 * La cabecera editorial del catálogo, en sus dos versiones.
 *
 * <p>
 * <b>Qué se lleva de {@code ShowHousingsFrame} y por qué era un bloque.</b> El
 * hero no era una banda: eran dos bandas que se turnan, tres cifras que se
 * recalculan, un titular que se remide con cada cambio de ancho y una regla
 * sobre cuánta ventana puede ocupar. Trece métodos y catorce campos entrelazados
 * entre sí y con nada más de la pantalla. Sacarlos no es mover líneas de sitio:
 * es hacer visible que ese grupo tenía una frontera propia.
 *
 * <p>
 * <b>Las dos bandas viven dentro de este componente</b> y no sueltas en la raíz
 * de la pantalla, que es como estaban. Alternarlas era antes cosa del frame —dos
 * {@code setVisible} y un {@code revalidate}— y ahora es asunto interno: quien
 * usa esta clase no sabe que hay dos, solo le dice cuánto se ha desplazado la
 * lista.
 *
 * <hr>
 *
 * <p>
 * <b>La idea de las dos versiones es de Airbnb y resuelve una tensión real.</b>
 * El titular editorial es lo que le da carácter a la aplicación, pero mientras
 * recorres fichas no aporta nada y se lleva 140px de los 866 que hay. Con dos
 * versiones no hay que elegir: al llegar se ve el titular completo —el impacto— y
 * al bajar por la lista se contrae a una línea que sigue diciendo dónde estás,
 * devolviendo el espacio al contenido.
 *
 * <p>
 * Son dos paneles distintos y no el mismo encogido, a propósito: cambiar tamaños
 * de fuente y márgenes a mitad de transición produce saltos de reflujo, mientras
 * que alternar dos paneles ya construidos es instantáneo y no recalcula nada.
 */
public class HeroCatalogo extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Cuerpo de partida del titular. Lo escala {@link Layout#display}. */
	private static final float TITULAR = 40f;

	/**
	 * Por debajo de esto no se sigue encogiendo el titular.
	 *
	 * <p>
	 * Es el suelo de la búsqueda de {@link #ajustarEscalaDeDisplay}: un texto
	 * pequeño es aceptable, uno ilegible no. Si ni a este cuerpo cabe, se prefiere
	 * que se note que falta ancho a seguir bajando.
	 */
	private static final float TITULAR_MINIMO = 26f;

	/**
	 * El titular no puede llevarse más de un quinto de la ventana.
	 *
	 * <p>
	 * <b>El problema que resuelve es de primera impresión, y solo existía en
	 * ventanas bajas.</b> El hero ya se contraía al bajar por la lista, así que en
	 * cuanto el usuario mueve la rueda la lista se queda con el 74 % de la
	 * pantalla. Pero <b>antes de mover nada</b> —que es cuando alguien se hace una
	 * idea de qué es esta aplicación— el reparto es el de partida, y en un portátil
	 * de 1280×660 eso dejaba la lista en 313px: <b>un alojamiento, y cortado</b>.
	 * En un catálogo, la primera pantalla debería enseñar catálogo.
	 *
	 * <p>
	 * Con el hero completo midiendo unos 165 puntos, la regla lo despliega a partir
	 * de unos 825 de alto y lo arranca contraído por debajo. En un monitor de
	 * escritorio no cambia nada.
	 *
	 * <p>
	 * <b>No es una excepción a la identidad editorial, es la regla del proyecto
	 * aplicada a lo que toca:</b> cuando falta sitio cede el aire, nunca un
	 * elemento con el que se interactúa. El hero <em>es</em> aire; la lista es el
	 * contenido. Lo que no se hace es quitarlo donde sí cabe.
	 */
	private static final int PARTE_DE_VENTANA_PARA_EL_HERO = 5;

	/** Umbrales de contracción, con histéresis. Ver {@link #ajustarAlScroll}. */
	private static final int SCROLL_PARA_CONTRAER = 60;
	private static final int SCROLL_PARA_DESPLEGAR = 20;

	private final JPanel completo;
	private final JPanel compacto;

	private JLabel tituloPrimera;
	private JLabel tituloSegunda;
	/**
	 * La firma de la marca, en el sitio donde antes iba la frase de la estación.
	 *
	 * <p>
	 * <b>Se escribe aquí y no se traduce.</b> "By CocoBrain" es un nombre
	 * comercial, no una frase: se firma igual en español y en inglés, igual que no
	 * se traduce "ActiHome". Por eso es una constante y no una clave de
	 * {@code Textos}.
	 *
	 * <p>
	 * Hasta la Fase 9 este hueco lo ocupaba la frase editorial de la estación
	 * ("Estancias de verano — Sol alto, luz dorada y sombra fresca"). Se retiró por
	 * decisión del usuario: la estación ya se ve en la paleta, en las partículas y
	 * en la mascota, y una frase distinta en cada una era una cuarta señal de lo
	 * mismo compitiendo con el titular.
	 */
	private static final String FIRMA = "By CocoBrain";

	private JLabel firmaDeMarca;
	private JPanel controles;

	private JLabel resumenCompacto;
	private JLabel tituloCompacto;

	private Stat enCatalogo;
	private Stat disponibles;
	private Stat media;

	private boolean contraido;

	/** Cuántas estancias hay ahora mismo, para el resumen de la banda compacta. */
	private int estancias;

	/**
	 * @param buscador el campo de búsqueda, que vive en {@code CatalogFilters} y se
	 *                 aloja aquí. Ver {@link #controles()}
	 */
	public HeroCatalogo(JComponent buscador) {

		// "hidemode 3" para que la banda oculta no reserve su hueco: es lo que permite
		// que al contraerse la lista gane el espacio de verdad y no quede un vacío.
		super(new MigLayout("wrap 1, hidemode 3, " + Space.insets(0), "[grow,fill]", "[]0[]"));

		setOpaque(false);

		completo = bandaCompleta(buscador);
		compacto = bandaCompacta();

		add(completo, "growx");
		add(compacto, "growx");
	}

	// ------------------------------------------------------------------
	// Construcción
	// ------------------------------------------------------------------

	private JPanel bandaCompleta(JComponent buscador) {

		// Los márgenes son menores que los del handoff (48/44/30). En una ventana de
		// escritorio el alto es el recurso escaso: cada píxel que se lleva el hero se
		// lo quita a la lista, que es lo único que el usuario ha venido a mirar. El
		// aire lateral se mantiene, que es el que se nota.
		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.XL, 0, Space.SM, 0),
				Space.LG + ":" + Space.HUGE + ":" + Space.HUGE + "[grow]" + Space.MD + ":" + Space.XXXL + ":"
						+ Space.XXXL + "[]" + Space.LG + ":" + Space.HUGE + ":" + Space.HUGE,
				"[]"));
		panel.setOpaque(false);

		// El titular NO lleva el tope de Layout.TEXTO. Ese tope está pensado para
		// columnas de texto legible —una línea muy larga cansa de leer— y un titular de
		// display no es eso. Con el tope puesto y la fuente escalada en pantallas
		// grandes, la frase pedía más de los 560px permitidos y se quedaba en "Elige
		// dónde quieres desperta": la última letra, cortada.
		panel.add(titular(), "growx, aligny bottom");

		controles = controles(buscador);
		panel.add(controles, "aligny bottom");

		return panel;
	}

	private JPanel titular() {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.SM + "[]"));
		panel.setOpaque(false);

		firmaDeMarca = Labels.capsAccent(FIRMA);
		panel.add(firmaDeMarca);

		// En UNA línea, no en dos como el mockup.
		//
		// El handoff parte el titular porque en una web sobra alto. Aquí el alto es lo
		// que se le quita al catálogo: la segunda línea costaba sesenta píxeles, que es
		// casi una cuarta parte de una ficha. En una línea el titular funciona además
		// como cabecera de revista, que es exactamente la referencia del diseño.
		//
		// Son dos etiquetas seguidas y no una con marcado: el renderizado HTML de Swing
		// calcula sus tamaños por su cuenta y se lleva mal con las fuentes registradas
		// en tiempo de ejecución.
		//
		// El handoff pone "despertar" en cursiva, y así estuvo hasta que el usuario
		// pidió quitarla. Es una desviación consciente: una cursiva serif de verdad no
		// es la redonda inclinada, lleva las letras dibujadas aparte, y ese cambio de
		// forma en mitad de la frase se percibía como que la palabra estaba en otra
		// tipografía.
		//
		// La separación entre las dos etiquetas es la de un espacio de esta fuente a
		// este cuerpo, MEDIDA — no una constante. Era Space.SM (12px), ajustado a ojo
		// con Spectral; al cambiar a Fraunces (Fase 8.2), que tiene el espacio más
		// estrecho, esos mismos 12px se leían como dos espacios seguidos.
		int espacio = Typography.anchoDeEspacio(Typography.serifMedium(TITULAR));

		JPanel linea = new JPanel(new MigLayout(Space.insets(0), "[]" + espacio + "[]push", "[]"));
		linea.setOpaque(false);

		tituloPrimera = Labels.hero(Textos.t("catalogo.hero.titulo1"));
		tituloPrimera.setFont(Typography.serifMedium(TITULAR));

		tituloSegunda = Labels.hero(Textos.t("catalogo.hero.titulo2"));
		tituloSegunda.setFont(Typography.serifMedium(TITULAR));

		linea.add(tituloPrimera, "aligny bottom");
		linea.add(tituloSegunda, "aligny bottom");

		panel.add(linea);

		return panel;
	}

	/**
	 * La columna derecha: el buscador y las tres cifras.
	 *
	 * <p>
	 * El selector de estación estuvo aquí y subió a la cabecera. Dos motivos, y el
	 * segundo es el importante: recupera alto para la lista, y sobre todo deja de
	 * ser alcanzable <b>solo</b> desde esta pantalla — antes, estando en el detalle
	 * o en un formulario no había forma de cambiar de estación.
	 *
	 * <p>
	 * <b>El buscador se recibe hecho, no se construye aquí</b>, y viene de
	 * {@link CatalogFilters}: es un filtro más y su lógica vive con los demás. Solo
	 * su <em>sitio</em> es este, porque ocupa exactamente el hueco que dejó el
	 * selector de estación y así se recupera su banda de ~85px sin que el hero
	 * crezca ni un píxel. Ponerlo bajo el titular fue el primer intento y salía más
	 * caro: el hero pasaba de 183 a 231px.
	 */
	private JPanel controles(JComponent buscador) {

		JPanel panel = new JPanel(new MigLayout("wrap 1, " + Space.insets(0), "[grow,fill]", "[]" + Space.MD + "[]"));
		panel.setOpaque(false);

		// El ancho del buscador es un RANGO, no un número. Con "w 340!" el hero exigía
		// 340 puntos pasara lo que pasara, y en una ventana estrecha esa exigencia se
		// traducía en que el campo se dibujaba saliéndose por la derecha.
		panel.add(buscador, "w 220:340:340, h " + Typography.altoDeControl() + "!, alignx right");
		panel.add(cifras());

		return panel;
	}

	private JPanel cifras() {

		String aire = Space.SM + ":" + Space.XL + ":" + Space.XL;

		JPanel panel = new JPanel(new MigLayout(Space.insets(0),
				"push[]" + aire + "[1!]" + aire + "[]" + aire + "[1!]" + aire + "[]", "[]"));
		panel.setOpaque(false);

		enCatalogo = new Stat("0", Textos.t("catalogo.stat.enCatalogo"));
		disponibles = new Stat("0", Textos.t("catalogo.stat.disponibles"));
		media = new Stat("—", Textos.t("catalogo.stat.media"), true);

		panel.add(enCatalogo);
		panel.add(Hairline.vertical(), "growy");
		panel.add(disponibles);
		panel.add(Hairline.vertical(), "growy");
		panel.add(media);

		return panel;
	}

	/** El hero reducido a una línea, para cuando el usuario ya está explorando. */
	private JPanel bandaCompacta() {

		JPanel panel = new JPanel(new MigLayout(Space.insets(Space.SM, Space.HUGE, Space.SM, Space.HUGE),
				"[]" + Space.MD + "[]push[]", "[]"));
		panel.setOpaque(false);
		panel.setVisible(false);

		resumenCompacto = Labels.capsAccent("");
		panel.add(resumenCompacto, "aligny center");

		tituloCompacto = Labels.body("");
		tituloCompacto.setFont(Typography.serifMedium(19f));
		panel.add(tituloCompacto, "aligny center");

		return panel;
	}

	// ------------------------------------------------------------------
	// Comportamiento
	// ------------------------------------------------------------------

	/**
	 * Decide si toca la banda completa o la compacta, según lo desplazada que esté
	 * la lista <b>y según lo alta que sea la ventana</b>.
	 *
	 * <p>
	 * <b>El umbral tiene histéresis a propósito</b>: se contrae al pasar de 60px y
	 * no se despliega hasta bajar de 20. Con un único umbral, quedarse justo en el
	 * límite hace que el hero parpadee entre los dos estados a cada píxel de
	 * scroll, y ese temblor es mucho peor que cualquiera de los dos estados.
	 *
	 * @param desplazamiento posición actual de la barra de la lista
	 * @param altoDeVentana  alto útil de la ventana, para la regla del quinto
	 * @return {@code true} si el estado ha cambiado y quien llama debe revalidar
	 */
	public boolean ajustarAlScroll(int desplazamiento, int altoDeVentana) {

		boolean contraer = !seGanaSuSitio(altoDeVentana)
				|| (contraido ? desplazamiento > SCROLL_PARA_DESPLEGAR : desplazamiento > SCROLL_PARA_CONTRAER);

		if (contraer == contraido) {
			return false;
		}

		contraido = contraer;

		completo.setVisible(!contraer);
		compacto.setVisible(contraer);

		if (contraer) {
			actualizarBandaCompacta();
		}

		revalidate();
		repaint();

		return true;
	}

	/**
	 * Si la ventana da de sí lo bastante como para que el hero completo valga lo
	 * que cuesta. Ver {@link #PARTE_DE_VENTANA_PARA_EL_HERO}.
	 *
	 * <p>
	 * <b>Se mide contra el alto de la ventana y no contra el que le queda a la
	 * lista</b>, y esa elección importa: el alto de la lista depende de si el hero
	 * está contraído, así que decidir con él realimentaría la propia decisión y el
	 * hero oscilaría entre los dos estados. El de la ventana no depende de nada de
	 * esto.
	 */
	private boolean seGanaSuSitio(int altoDeVentana) {

		// Antes del primer pase de layout todavía no hay alto. Se responde que sí
		// porque el hero completo es el estado de partida y así no hay un parpadeo de
		// contraído a desplegado nada más abrirse la pantalla.
		if (altoDeVentana <= 0) {
			return true;
		}

		return completo.getPreferredSize().height * PARTE_DE_VENTANA_PARA_EL_HERO <= altoDeVentana;
	}

	private void actualizarBandaCompacta() {

		resumenCompacto.setText(Theme.estacion().nombre().toUpperCase());
		tituloCompacto.setText(Formato.plural(estancias, Textos.t("palabra.estancia.singular"),
				Textos.t("palabra.estancia.plural")) + " " + Textos.t("catalogo.compacto.paraElegir"));
	}

	// ------------------------------------------------------------------
	// Refrescos
	// ------------------------------------------------------------------

	/**
	 * Recalcula las tres cifras.
	 *
	 * @param alojamientos  los que hay tras aplicar los filtros
	 * @param ocupadosAhora identificadores de los que no están libres hoy
	 */
	public void actualizarCifras(List<Housing> alojamientos, java.util.Set<Long> ocupadosAhora) {

		estancias = alojamientos.size();

		int libres = 0;
		double suma = 0;
		int conNota = 0;

		for (Housing housing : alojamientos) {

			if (!ocupadosAhora.contains(housing.getId())) {
				libres++;
			}

			if (housing.getScore() != null) {
				suma += housing.getScore();
				conNota++;
			}
		}

		enCatalogo.setValor(String.valueOf(estancias));
		disponibles.setValor(String.valueOf(libres));
		media.setValor(conNota == 0 ? "—" : Formato.nota(suma / conNota));

		if (contraido) {
			actualizarBandaCompacta();
		}
	}

	/**
	 * Reescribe lo que cambia con la estación.
	 *
	 * <p>
	 * Cambiar de estación no solo cambia colores: cambia también las palabras. Los
	 * colores se resuelven solos al repintar; el texto hay que reescribirlo.
	 */
	public void actualizarTextosEstacionales() {

		// La firma no cambia con la estacion: no hay nada que reescribir aqui.

		if (contraido) {
			actualizarBandaCompacta();
		}

		repaint();
	}

	/**
	 * Reescribe lo que depende solo del idioma (Fase 7.6).
	 *
	 * <p>
	 * El titular puede pasar a medir distinto —el inglés no ocupa lo mismo que el
	 * español—, así que quien llama debe reajustar la escala después.
	 */
	public void actualizarTextosFijos() {

		tituloPrimera.setText(Textos.t("catalogo.hero.titulo1"));
		tituloSegunda.setText(Textos.t("catalogo.hero.titulo2"));

		enCatalogo.setRotulo(Textos.t("catalogo.stat.enCatalogo"));
		disponibles.setRotulo(Textos.t("catalogo.stat.disponibles"));
		media.setRotulo(Textos.t("catalogo.stat.media"));

		if (contraido) {
			actualizarBandaCompacta();
		}
	}

	/**
	 * Escala el titular con el ancho de la ventana, <b>sin dejar nunca que se
	 * corte</b>.
	 *
	 * <p>
	 * <b>Escalar a ciegas no basta, y este fue el fallo.</b> La regla del sistema
	 * dice que la tipografía de display crece en ventanas grandes
	 * ({@code Layout.display}), y así estaba: ×1.35 por encima de cierto ancho.
	 * Pero nadie comprobaba que la frase resultante cupiera, así que en un monitor
	 * de 27 pulgadas el titular pedía más sitio del que tenía y se quedaba en
	 * "Elige dónde quieres <b>desperta</b>". <b>Un texto cortado es peor que un
	 * texto pequeño.</b>
	 *
	 * <p>
	 * Ahora el tamaño que devuelve la regla es el <em>punto de partida</em>, no la
	 * última palabra: se mide lo que ocuparía la frase con esa fuente y se va
	 * bajando hasta que entra en el sitio real. La medida se hace con
	 * {@code FontMetrics}, que es lo mismo que usará Swing al dibujar, así que no
	 * hay estimaciones de por medio.
	 *
	 * @param anchoDeVentana el ancho total disponible
	 */
	public void ajustarEscalaDeDisplay(int anchoDeVentana) {

		if (anchoDeVentana <= 0 || tituloPrimera == null) {
			return;
		}

		float tamano = Layout.display(TITULAR, anchoDeVentana);
		int disponible = anchoParaElTitular(anchoDeVentana);

		while (tamano > TITULAR_MINIMO && anchoDelTitular(tamano) > disponible) {
			tamano -= 1f;
		}

		tituloPrimera.setFont(Typography.serifMedium(tamano));
		tituloSegunda.setFont(Typography.serifMedium(tamano));

		revalidate();
		repaint();
	}

	/** Lo que mediría la frase completa dibujada con ese cuerpo. */
	private int anchoDelTitular(float tamano) {

		FontMetrics metrica = getFontMetrics(Typography.serifMedium(tamano));

		return metrica.stringWidth(tituloPrimera.getText()) + Space.SM + metrica.stringWidth(tituloSegunda.getText());
	}

	/**
	 * El ancho que le queda de verdad al titular: la ventana menos los márgenes del
	 * hero, la columna de la derecha y el hueco entre ambas.
	 */
	private int anchoParaElTitular(int anchoDeVentana) {

		int derecha = controles == null ? 360 : controles.getPreferredSize().width;

		return anchoDeVentana - Space.HUGE * 2 - Space.XXXL - derecha;
	}
}
