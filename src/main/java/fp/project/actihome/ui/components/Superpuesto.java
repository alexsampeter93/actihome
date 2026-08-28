package fp.project.actihome.ui.components;

/**
 * Marca un contenedor que <b>apila sus hijos a propósito</b>.
 *
 * <p>
 * Existe para que {@code MedirResponsive} pueda distinguir una superposición
 * intencionada de un fallo de reparto, cosa que no se puede deducir mirando las
 * coordenadas: la etiqueta "CASA" sobre una foto y el "ACTIHOME" pisado por
 * "BUSCAR" tienen exactamente la misma forma en píxeles y significados
 * opuestos.
 *
 * <p>
 * <b>Es una interfaz y no una lista de clases dentro de la herramienta, y el
 * motivo es el mismo que llevó a {@link fp.project.actihome.ui.nav.ConNombre}:</b>
 * una tabla en el detector obligaría al detector a conocer todas las pantallas,
 * y se quedaría vieja en cuanto alguien escribiera una nueva sin acordarse de
 * darla de alta. Quien apila lo declara donde apila.
 *
 * <p>
 * No tiene métodos: lo único que hace falta saber es si el contenedor apila o
 * no. Y ojo con el orden al implementarla, porque es al revés que en la web:
 * los hijos de un contenedor de Swing se pintan <b>del último índice al
 * primero</b>, así que el que se añade primero queda encima.
 */
public interface Superpuesto {
	// Interfaz de marca: no declara nada.
}
