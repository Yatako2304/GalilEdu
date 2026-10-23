package com.galiledu.configuracion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Apoyo común de las pruebas de entradas hostiles de la HU-44: armado de cuerpos JSON crudos y revisión de fugas. */
final class AyudasEntradasHostiles {
	/** Lo que una respuesta de error nunca debe mostrar: pila, clases, SQL, tablas, restricciones o índices. */
	static final List<String> RASTROS_INTERNOS = List.of("Exception", "org.", "com.galiledu", "java.", "SELECT ",
			"INSERT ", "ERROR:", "estructura_horaria", "configuracion.", "ux_", "uq_", "ck_", "fk_", "hibernate",
			"springframework", "Caused by", "\tat ");

	private AyudasEntradasHostiles() {
	}

	static List<String> rastrosEn(String texto) {
		List<String> encontrados = new ArrayList<>();
		for (String rastro : RASTROS_INTERNOS) {
			if (texto.contains(rastro)) {
				encontrados.add(rastro);
			}
		}
		return encontrados;
	}

	/** El texto como literal JSON entre comillas: escapa comillas, barras y todo carácter de control (incluido NUL). */
	static String texto(String valor) {
		StringBuilder literal = new StringBuilder("\"");
		for (int posicion = 0; posicion < valor.length(); posicion++) {
			char caracter = valor.charAt(posicion);
			if (caracter == '"') {
				literal.append("\\\"");
			} else if (caracter == '\\') {
				literal.append("\\\\");
			} else if (caracter < 0x20) {
				literal.append(String.format("\\u%04x", (int) caracter));
			} else {
				literal.append(caracter);
			}
		}
		return literal.append('"').toString();
	}

	/**
	 * Cuerpo de crear con valores válidos por defecto (EH-HOSTIL, primaria, lunes a viernes, 08:00, 6 bloques de 45
	 * min,
	 * sin recesos). Se pasan pares clave, literal JSON crudo para reemplazar un campo; un literal null quita la clave.
	 */
	static String cuerpoDeCrear(String... clavesYLiterales) {
		Map<String, String> campos = new LinkedHashMap<>();
		campos.put("codigo", texto("EH-HOSTIL"));
		campos.put("descripcion", texto("Prueba"));
		campos.put("nivel", texto("PRIMARIA"));
		campos.put("esPrincipal", "false");
		campos.put("diasLectivos", "[\"LUN\",\"MAR\",\"MIE\",\"JUE\",\"VIE\"]");
		campos.put("horaEntrada", texto("08:00"));
		campos.put("bloquesPorDia", "6");
		campos.put("duracionBloqueMin", "45");
		campos.put("recesos", "[]");
		return armar(campos, clavesYLiterales);
	}

	/** Cuerpo de editar: igual que el de crear, sin el nivel. */
	static String cuerpoDeEditar(String... clavesYLiterales) {
		Map<String, String> campos = new LinkedHashMap<>();
		campos.put("codigo", texto("EH-HOSTIL"));
		campos.put("descripcion", texto("Prueba"));
		campos.put("esPrincipal", "false");
		campos.put("diasLectivos", "[\"LUN\",\"MAR\",\"MIE\",\"JUE\",\"VIE\"]");
		campos.put("horaEntrada", texto("08:00"));
		campos.put("bloquesPorDia", "6");
		campos.put("duracionBloqueMin", "45");
		campos.put("recesos", "[]");
		return armar(campos, clavesYLiterales);
	}

	private static String armar(Map<String, String> campos, String[] clavesYLiterales) {
		for (int posicion = 0; posicion < clavesYLiterales.length; posicion += 2) {
			String clave = clavesYLiterales[posicion];
			String literal = clavesYLiterales[posicion + 1];
			if (literal == null) {
				campos.remove(clave);
			} else {
				campos.put(clave, literal);
			}
		}
		StringBuilder json = new StringBuilder("{");
		for (Map.Entry<String, String> campo : campos.entrySet()) {
			if (json.length() > 1) {
				json.append(',');
			}
			json.append(texto(campo.getKey())).append(':').append(campo.getValue());
		}
		return json.append('}').toString();
	}

	/** Un arreglo JSON de {@code cantidad} copias del mismo literal. */
	static String arreglo(String literal, int cantidad) {
		StringBuilder json = new StringBuilder("[");
		for (int numero = 0; numero < cantidad; numero++) {
			if (numero > 0) {
				json.append(',');
			}
			json.append(literal);
		}
		return json.append(']').toString();
	}

	/**
	 * Todo lo guardado de estructuras horarias y recesos, como texto, para comparar antes y después de una petición
	 * rechazada. Con {@code conActualizadoEn} false se ignora esa fecha (para ediciones que sí escriben).
	 */
	static String fotoDeEstructuras(JdbcClient jdbc, boolean conActualizadoEn) {
		String actualizadoEn = conActualizadoEn ? ", e.actualizado_en" : "";
		String estructuras = jdbc.sql("""
				SELECT coalesce(string_agg(concat_ws('|', e.id, e.anio_escolar_id, e.codigo, e.descripcion, e.nivel,
				    e.es_principal, e.dias_lectivos::text, e.hora_entrada, e.bloques_por_dia, e.duracion_bloque_min,
				    e.creado_en%s), E'\\n' ORDER BY e.id), '')
				FROM configuracion.estructura_horaria e""".formatted(actualizadoEn))
				.query(String.class).single();
		String recesos = jdbc.sql("""
				SELECT coalesce(string_agg(concat_ws('|', r.id, r.estructura_horaria_id, r.despues_de_bloque,
				    r.duracion_min), E'\\n' ORDER BY r.id), '')
				FROM configuracion.receso r""").query(String.class).single();
		return estructuras + "\n--recesos--\n" + recesos;
	}

	static boolean existeLaTabla(JdbcClient jdbc, String tabla) {
		return jdbc.sql("SELECT to_regclass(:tabla) IS NOT NULL").param("tabla", tabla).query(Boolean.class).single();
	}
}
