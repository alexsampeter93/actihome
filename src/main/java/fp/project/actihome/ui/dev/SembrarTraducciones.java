package fp.project.actihome.ui.dev;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Genera el SQL que precarga la caché de traducciones (Fase 8.7).
 *
 * <pre>
 * .\mvnw.cmd compile exec:java "-Dexec.mainClass=fp.project.actihome.ui.dev.SembrarTraducciones"
 * </pre>
 *
 * <p>
 * <b>Por qué existe.</b> La caché guarda un <em>hash</em> del texto original,
 * no el texto, así que las traducciones escritas a mano no se pueden meter en
 * {@code data.sql} tecleándolas: hay que calcular el SHA-256 de cada original.
 * Esta herramienta lo hace y escribe el fichero.
 *
 * <p>
 * <b>Y por qué a mano y no dejando que las traduzca MyMemory.</b> Este es el
 * contenido que se ve en la primera pantalla: descripciones de alojamientos y
 * títulos de reseña. Es texto de producto —«casa de piedra restaurada a media
 * ladera»— y ahí la traducción automática se nota. Escritas a mano salen bien y
 * además <b>no gastan ni una llamada de la cuota diaria</b>, que queda entera
 * para el contenido que publique el usuario.
 *
 * <p>
 * Es exactamente lo que hace un producto real: traducción profesional para el
 * contenido propio, automática para el de los usuarios. La caché no distingue
 * entre las dos, y ahí está la gracia.
 *
 * <p>
 * Los <b>cuerpos</b> de las reseñas no están aquí a propósito: son veinte textos
 * largos que solo se leen al abrir una reseña concreta, así que se traducen
 * solos la primera vez y se quedan en la caché. Los nombres de alojamiento y las
 * ubicaciones tampoco: «Casa Rural El Pinar» y «Sierra Nevada, Granada» son
 * nombres propios, y ninguna plataforma real los traduce.
 */
public final class SembrarTraducciones {

	private static final Path DESTINO = Paths.get("src", "main", "resources", "traducciones.sql");

	/** Original en español → traducción en inglés, escrita a mano. */
	private static final Map<String, String> AL_INGLES = new LinkedHashMap<>();

	static {

		// --- Descripciones de los diez alojamientos ---

		AL_INGLES.put(
				"Casa de piedra restaurada a media ladera, con chimenea, huerto y vistas abiertas a la sierra. El pueblo queda a diez minutos a pie.",
				"A restored stone house halfway up the hillside, with a fireplace, a vegetable garden and open views of the mountains. The village is a ten-minute walk away.");

		AL_INGLES.put(
				"Segunda línea de playa, con terraza orientada al sur y ascensor. La zona de bares y el paseo marítimo quedan al doblar la esquina.",
				"Second line from the beach, with a south-facing terrace and a lift. The bars and the seafront promenade are just around the corner.");

		AL_INGLES.put(
				"Villa encalada con piscina privada, porche de sombra y acceso directo a una cala pequeña. Pensión completa incluida.",
				"A whitewashed villa with a private pool, a shaded porch and direct access to a small cove. Full board included.");

		AL_INGLES.put(
				"Cabaña de madera para dos, con estufa de leña y ventanal al hayedo. Sin cobertura y sin vecinos: ese es el plan.",
				"A wooden cabin for two, with a wood-burning stove and a picture window onto the beech forest. No phone signal and no neighbours: that is the whole point.");

		AL_INGLES.put(
				"Loft diáfano en un edificio del XIX, con vigas vistas y techos de cuatro metros. En pleno casco antiguo, a paso de todo.",
				"An open-plan loft in a 19th-century building, with exposed beams and four-metre ceilings. Right in the old town, walking distance from everything.");

		AL_INGLES.put(
				"Adosado con patio, barbacoa y piscina comunitaria, en una urbanización tranquila a quince minutos de la playa de Las Canteras.",
				"A townhouse with a patio, a barbecue and a shared pool, on a quiet development fifteen minutes from Las Canteras beach.");

		AL_INGLES.put(
				"Ático con terraza recién reformado, cocina abierta y aire acondicionado en las dos habitaciones. El barrio se recorre entero a pie: bares, teatros y el Retiro a quince minutos.",
				"A newly refurbished top-floor flat with a terrace, an open kitchen and air conditioning in both bedrooms. The whole neighbourhood is walkable: bars, theatres, and the Retiro fifteen minutes away.");

		AL_INGLES.put(
				"Villa de líneas contemporáneas con piscina infinita, terraza panorámica y vistas al mar desde las dos plantas. Pensión completa incluida.",
				"A contemporary villa with an infinity pool, a panoramic terrace and sea views from both floors. Full board included.");

		AL_INGLES.put(
				"Casa con patio interior en el corazón del casco histórico, paredes encaladas y suelo de barro cocido. El Tajo queda a cinco minutos a pie desde la puerta.",
				"A house with an inner courtyard in the heart of the old town, whitewashed walls and terracotta floors. The Tagus is a five-minute walk from the door.");

		AL_INGLES.put(
				"Refugio de madera con estufa de leña y vistas a las pistas desde el porche. Aparcamiento propio junto a la puerta, imprescindible cuando nieva.",
				"A wooden lodge with a wood-burning stove and views of the slopes from the porch. Private parking right by the door, which matters when it snows.");

		// --- Títulos de las reseñas ---

		AL_INGLES.put("Silencio y buen desayuno", "Quiet, and a good breakfast");
		AL_INGLES.put("Perfecta para desconectar", "Perfect for switching off");
		AL_INGLES.put("No se puede pedir más", "You could not ask for more");
		AL_INGLES.put("Muy bien situado", "Very well located");
		AL_INGLES.put("La cala privada lo vale", "The private cove is worth it");
		AL_INGLES.put("Caro pero se disfruta", "Pricey, but you enjoy every minute");
		AL_INGLES.put("Rústica de verdad", "Genuinely rustic");
		AL_INGLES.put("Nos faltó calefacción", "We could have used more heating");
		AL_INGLES.put("El mejor sitio del barrio", "The best spot in the neighbourhood");
		AL_INGLES.put("Encanto de edificio antiguo", "All the charm of an old building");
		AL_INGLES.put("Buen plan en familia", "A good family trip");
		AL_INGLES.put("Correcto de principio a fin", "Solid from start to finish");
		AL_INGLES.put("Ubicación insuperable", "Unbeatable location");
		AL_INGLES.put("Pequeño pero muy bien pensado", "Small, but very well thought out");
		AL_INGLES.put("Vale cada euro", "Worth every euro");
		AL_INGLES.put("Lujo sin postureo", "Luxury without the showing off");
		AL_INGLES.put("El patio es otro mundo", "The courtyard is another world");
		AL_INGLES.put("Auténtica de verdad", "The real thing");
		AL_INGLES.put("Desconexión total", "Complete disconnection");
		AL_INGLES.put("Ideal para esquiar", "Ideal for a ski trip");

		// --- Lo que buscan los intercambios abiertos ---

		AL_INGLES.put("una casa rural para agosto", "a country house for August");
		AL_INGLES.put("un apartamento urbano con buena conexión", "a city flat with good transport links");
		AL_INGLES.put("una cabaña de montaña en invierno", "a mountain cabin in winter");
	}

	private SembrarTraducciones() {
	}

	public static void main(String[] args) throws IOException {

		StringBuilder sql = new StringBuilder();

		sql.append("-- GENERADO por ui/dev/SembrarTraducciones. No editar a mano.\n");
		sql.append("--\n");
		sql.append("-- Precarga la cache de traducciones con las versiones inglesas escritas a\n");
		sql.append("-- mano del contenido de ejemplo. La clave es el SHA-256 del texto original,\n");
		sql.append("-- que es la razon de que este fichero se genere en lugar de teclearse.\n");
		sql.append("--\n");
		sql.append("-- Idempotente por el WHERE NOT EXISTS de siempre: ejecutarlo en cada arranque\n");
		sql.append("-- no duplica nada, y respeta cualquier traduccion ya guardada.\n\n");

		for (Map.Entry<String, String> entrada : AL_INGLES.entrySet()) {

			String clave = hash(entrada.getKey());
			String traducido = entrada.getValue().replace("'", "''");

			sql.append("INSERT INTO TRANSLATIONS(sourceHash, targetLanguage, translatedText)\n");
			sql.append("SELECT '").append(clave).append("', 'en', '").append(traducido).append("'\n");
			sql.append("FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM TRANSLATIONS")
					.append(" WHERE sourceHash = '").append(clave).append("' AND targetLanguage = 'en');\n\n");
		}

		Files.createDirectories(DESTINO.getParent());

		try (OutputStream salida = Files.newOutputStream(DESTINO)) {
			salida.write(sql.toString().getBytes(StandardCharsets.UTF_8));
		}

		System.out.println();
		System.out.println("Escrito " + DESTINO + " con " + AL_INGLES.size() + " traducciones.");
		System.out.println("Recuerda incluirlo en application.yaml (spring.datasource.data).");
		System.exit(0);
	}

	private static String hash(String texto) {

		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexadecimal = new StringBuilder(resumen.length * 2);

			for (byte b : resumen) {
				hexadecimal.append(String.format("%02x", b));
			}

			return hexadecimal.toString();

		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 no disponible", ex);
		}
	}
}
