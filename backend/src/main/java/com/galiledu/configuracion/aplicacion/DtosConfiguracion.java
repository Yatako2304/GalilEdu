package com.galiledu.configuracion.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoPeriodo;
import com.galiledu.configuracion.dominio.PeriodoAcademico;
import com.galiledu.configuracion.dominio.Regimen;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion.AnioListado;

/** Contratos JSON del módulo de configuración. Deben coincidir con frontend/src/types/api.ts. */
public final class DtosConfiguracion {
	private DtosConfiguracion() {
	}

	/** Figma "Modal - Nuevo año escolar" (HU-74). */
	public record SolicitudAnioEscolar(
			@NotNull(message = "Ingresa el año escolar.") @Min(value = 2000, message = "El año escolar debe estar entre 2000 y 2100.") @Max(value = 2100, message = "El año escolar debe estar entre 2000 y 2100.") Integer anio,
			@NotNull(message = "Ingresa la fecha de inicio.") LocalDate fechaInicio,
			@NotNull(message = "Ingresa la fecha de fin.") LocalDate fechaFin,
			@NotNull(message = "Elige el régimen de periodos.") Regimen regimen) {
	}

	/** Fila del listado "CI-01 · Año escolar". */
	public record AnioEscolarResumen(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			int cantidadPeriodos, EstadoAnioEscolar estado) {
		static AnioEscolarResumen de(AnioListado a) {
			return new AnioEscolarResumen(a.id(), a.anio(), a.fechaInicio(), a.fechaFin(), a.regimen(),
					a.cantidadPeriodos(), a.estado());
		}
	}

	/** Año con sus periodos: "CI-03 · Año escolar 2026 › Periodos". */
	public record AnioEscolarDetalle(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			EstadoAnioEscolar estado, List<PeriodoDetalle> periodos) {
		static AnioEscolarDetalle de(AnioEscolar a) {
			return new AnioEscolarDetalle(a.getId(), a.getAnio(), a.getFechaInicio(), a.getFechaFin(), a.getRegimen(),
					a.getEstado(), a.getPeriodos().stream().map(PeriodoDetalle::de).toList());
		}
	}

	public record PeriodoDetalle(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, EstadoPeriodo estado) {
		static PeriodoDetalle de(PeriodoAcademico p) {
			return new PeriodoDetalle(p.getId(), p.getNumero(), p.getNombre(), p.getFechaInicio(), p.getFechaFin(),
					p.getFechaLimiteNotas(), p.getEstado());
		}
	}
}
