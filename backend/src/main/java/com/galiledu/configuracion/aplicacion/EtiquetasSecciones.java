package com.galiledu.configuracion.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;

/**
 * Textos de la pantalla 13 que se arman con las secciones: la etiqueta "1.° A" y el resumen "1.° a 5.°, A y B". El
 * número del grado sale del código del grado (SEC-1 es 1).
 */
final class EtiquetasSecciones {
	private EtiquetasSecciones() {
	}

	static int numeroDeGrado(String gradoCodigo) {
		return Integer.parseInt(gradoCodigo.substring(gradoCodigo.indexOf('-') + 1));
	}

	static String etiqueta(String gradoCodigo, String nombre) {
		return numeroDeGrado(gradoCodigo) + ".° " + nombre;
	}

	/**
	 * "1.° a 5.°, A y B". Si hay secciones de más de un nivel se indica cada uno ("Primaria 1.° a 6.°, Secundaria 1.° a
	 * 5.°, A y B"). Vacío si no hay secciones.
	 */
	static String resumen(List<SeccionConVacantes> secciones) {
		if (secciones.isEmpty()) {
			return "";
		}
		Map<String, int[]> rangos = new LinkedHashMap<>();
		TreeSet<String> nombres = new TreeSet<>();
		for (SeccionConVacantes s : secciones) {
			String nivel = s.gradoCodigo().substring(0, s.gradoCodigo().indexOf('-'));
			int numero = numeroDeGrado(s.gradoCodigo());
			int[] rango = rangos.computeIfAbsent(nivel, k -> new int[]{numero, numero});
			rango[0] = Math.min(rango[0], numero);
			rango[1] = Math.max(rango[1], numero);
			nombres.add(s.nombre());
		}
		List<String> grados = new ArrayList<>();
		rangos.forEach((nivel, rango) -> {
			String texto = rango[0] == rango[1] ? rango[0] + ".°" : rango[0] + ".° a " + rango[1] + ".°";
			grados.add(rangos.size() == 1 ? texto : nombreDeNivel(nivel) + " " + texto);
		});
		return String.join(", ", grados) + ", " + unir(List.copyOf(nombres));
	}

	private static String nombreDeNivel(String prefijo) {
		return switch (prefijo) {
			case "PRI" -> "Primaria";
			case "SEC" -> "Secundaria";
			default -> prefijo;
		};
	}

	/** "A", "A y B", "A, B y C". */
	private static String unir(List<String> nombres) {
		if (nombres.size() == 1) {
			return nombres.getFirst();
		}
		return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.getLast();
	}
}
