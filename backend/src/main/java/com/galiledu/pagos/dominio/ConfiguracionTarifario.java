package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Input and invariants for HU-34/RF-70–72. */
public record ConfiguracionTarifario(UUID anioEscolarId, BigDecimal montoMatricula,
	BigDecimal montoPension, int cantidadCuotas, LocalDate vencimientoMatricula,
	List<LocalDate> vencimientosPensiones,
	int diasGracia, BigDecimal tasaMensual, BigDecimal topePorcentaje) {
	public ConfiguracionTarifario {
		if (anioEscolarId == null) throw new IllegalArgumentException("El año escolar es obligatorio");
		montoMatricula = montoNoNegativo(montoMatricula, "El monto de matrícula");
		montoPension = montoNoNegativo(montoPension, "El monto de pensión");
		tasaMensual = montoNoNegativo(tasaMensual, "La tasa mensual");
		topePorcentaje = montoNoNegativo(topePorcentaje, "El tope porcentual");
		if (cantidadCuotas <= 0) throw new IllegalArgumentException("La cantidad de cuotas debe ser positiva");
		if (vencimientoMatricula == null) {
			throw new IllegalArgumentException("El vencimiento de matrícula es obligatorio");
		}
		if (diasGracia < 0) throw new IllegalArgumentException("Los días de gracia no pueden ser negativos");
		if (vencimientosPensiones == null || vencimientosPensiones.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("Los vencimientos son obligatorios");
		}
		vencimientosPensiones = List.copyOf(vencimientosPensiones);
		if (vencimientosPensiones.size() != cantidadCuotas) {
			throw new IllegalArgumentException("La cantidad de vencimientos no coincide con las cuotas declaradas");
		}
		if (new HashSet<>(vencimientosPensiones).size() != cantidadCuotas) {
			throw new IllegalArgumentException("Dos cuotas no pueden tener la misma fecha de vencimiento");
		}
	}

	private static BigDecimal montoNoNegativo(BigDecimal valor, String campo) {
		if (valor == null || valor.signum() < 0) {
			throw new IllegalArgumentException(campo + " es obligatorio y no puede ser negativo");
		}
		if (valor.scale() > 2) throw new IllegalArgumentException(campo + " admite como máximo dos decimales");
		return valor;
	}
}
