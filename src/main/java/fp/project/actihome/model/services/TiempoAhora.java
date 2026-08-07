package fp.project.actihome.model.services;

/**
 * El tiempo que hace <b>en este momento</b> en un sitio.
 *
 * <p>
 * <b>Por qué existe, además de la previsión diaria.</b> Porque contestan
 * preguntas distintas y el usuario del proyecto lo detectó usando la
 * aplicación: miró el tiempo de Marbella en su móvil, vio <b>24°</b>, y ActiHome
 * decía <b>27° / 23°</b>. Ninguno de los dos mentía —27 y 23 eran la máxima y la
 * mínima previstas para ese día, y 24 la temperatura de ese instante— pero la
 * ficha <b>no estaba contestando la pregunta que se le hacía</b>. Un dato
 * correcto que responde a otra cosa se percibe, con razón, como un dato
 * equivocado.
 *
 * <p>
 * {@link PrevisionDiaria} sigue existiendo y sigue haciendo falta: dice cómo
 * estará <em>cuando vayas</em>, que es lo que importa para reservar. Esta dice
 * cómo está <em>ahora</em>, que es lo que se comprueba de un vistazo.
 *
 * @param temperatura grados Celsius medidos ahora
 * @param sensacion   temperatura aparente: lo que se siente contando viento y
 *                    humedad. Puede alejarse bastante de la real
 * @param codigo      código WMO del estado del cielo; ver {@link CieloWmo}
 * @param esDeDia     si en ese punto del mapa es de día. Lo da el proveedor, y
 *                    <b>no se calcula con la hora local de quien mira</b>: un
 *                    alojamiento puede estar en otro huso, y en verano la
 *                    diferencia entre las nueve de la noche en Canarias y en
 *                    Huesca es que en una hay sol y en la otra no
 */
public record TiempoAhora(double temperatura, double sensacion, int codigo, boolean esDeDia) {

	/** El estado del cielo ya interpretado, listo para enseñar. */
	public CieloWmo cielo() {
		return CieloWmo.de(codigo);
	}

	/**
	 * Si merece la pena enseñar la sensación térmica junto a la real.
	 *
	 * <p>
	 * <b>Solo cuando difieren de verdad.</b> Poner "24°, sensación 24°" es ruido:
	 * ocupa sitio para no añadir nada y entrena al usuario a no leer esa línea. Con
	 * dos grados de diferencia ya es información —significa viento o humedad— y
	 * entonces sí vale la pena.
	 */
	public boolean sensacionRelevante() {
		return Math.abs(temperatura - sensacion) >= 2;
	}
}
