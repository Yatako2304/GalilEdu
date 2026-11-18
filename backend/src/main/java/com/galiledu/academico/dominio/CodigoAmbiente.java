package com.galiledu.academico.dominio;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Código de un ambiente, generado por el sistema: prefijo del tipo, guion y número de al menos tres cifras (A-001,
 * E-014). Nunca lo escribe el usuario ni cambia después de creado.
 * <p>
 * El siguiente código es el mayor número existente con el prefijo del tipo, más uno. Los códigos que no siguen ese
 * formato (LAB-01, AUD-01) o son de otro prefijo no cuentan. Es una función pura: dos peticiones simultáneas pueden
 * calcular el mismo código, y eso lo resuelve la restricción UNIQUE de la BD con un reintento del servicio.
 */
public record CodigoAmbiente(String valor) {
	private static final Pattern FORMATO = Pattern.compile("[A-Z]+-\\d+");

	public CodigoAmbiente {
		if (valor == null || !FORMATO.matcher(valor).matches()) {
			throw new IllegalArgumentException("El código del ambiente no tiene un formato válido.");
		}
	}

	public static CodigoAmbiente siguiente(TipoAmbiente tipo, Collection<String> codigosExistentes) {
		if (tipo == null) {
			throw new IllegalArgumentException("Elige el tipo de ambiente.");
		}
		// Hasta 9 cifras: así el número cabe en un int y un código absurdo de la BD no rompe el cálculo.
		Pattern delTipo = Pattern.compile(Pattern.quote(tipo.prefijo()) + "-(\\d{1,9})");
		int mayor = 0;
		if (codigosExistentes != null) {
			for (String codigo : codigosExistentes) {
				if (codigo == null) {
					continue;
				}
				Matcher coincidencia = delTipo.matcher(codigo);
				if (coincidencia.matches()) {
					mayor = Math.max(mayor, Integer.parseInt(coincidencia.group(1)));
				}
			}
		}
		return new CodigoAmbiente("%s-%03d".formatted(tipo.prefijo(), mayor + 1));
	}

	@Override
	public String toString() {
		return valor;
	}
}
