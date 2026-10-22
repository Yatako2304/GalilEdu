package com.galiledu.configuracion.dominio;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Los datos de una estructura horaria que el usuario puede cambiar al crearla o editarla. El nivel y si es principal
 * no están aquí: el nivel no se cambia y el cambio de principal tiene sus propias reglas.
 */
public record DefinicionEstructura(String codigo, String descripcion, List<String> diasLectivos, LocalTime horaEntrada,
		int bloquesPorDia, int duracionBloqueMin, List<DatosReceso> recesos) {

	/**
	 * La misma definición lista para validar y guardar: código sin espacios y en mayúsculas, descripción vacía como
	 * null, hora de entrada truncada a minutos (lo guardado y lo calculado coinciden), días en mayúsculas y en el
	 * orden de la semana (los repetidos o desconocidos se conservan para que la validación los reporte) y recesos por
	 * orden de bloque, sin los null.
	 */
	public DefinicionEstructura normalizada() {
		String codigoNormalizado = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
		String descripcionNormalizada = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
		LocalTime entradaEnMinutos = horaEntrada == null ? null : horaEntrada.truncatedTo(ChronoUnit.MINUTES);
		return new DefinicionEstructura(codigoNormalizado, descripcionNormalizada, diasEnOrdenDeSemana(),
				entradaEnMinutos, bloquesPorDia, duracionBloqueMin, recesosPorBloque());
	}

	private List<String> diasEnOrdenDeSemana() {
		List<String> codigosPedidos = new ArrayList<>();
		if (diasLectivos != null) {
			for (String dia : diasLectivos) {
				codigosPedidos.add(dia == null ? "" : dia.trim().toUpperCase(Locale.ROOT));
			}
		}
		List<String> ordenados = new ArrayList<>();
		for (DiaSemana diaDeLaSemana : DiaSemana.values()) {
			for (String codigoPedido : codigosPedidos) {
				if (codigoPedido.equals(diaDeLaSemana.name())) {
					ordenados.add(codigoPedido);
				}
			}
		}
		for (String codigoPedido : codigosPedidos) {
			if (!DiaSemana.esCodigoValido(codigoPedido)) {
				ordenados.add(codigoPedido);
			}
		}
		return ordenados;
	}

	private List<DatosReceso> recesosPorBloque() {
		List<DatosReceso> ordenados = new ArrayList<>();
		if (recesos != null) {
			for (DatosReceso receso : recesos) {
				if (receso != null) {
					ordenados.add(receso);
				}
			}
		}
		ordenados.sort(Comparator.comparingInt(DatosReceso::despuesDeBloque));
		return ordenados;
	}
}
